package com.mkclicompare.domain.comparison

import com.mkclicompare.config.CliProperties
import com.mkclicompare.domain.provider.CliProvider
import com.mkclicompare.domain.provider.ProviderService
import com.mkclicompare.web.error.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 비교 오케스트레이션:
 *  1) comparison 행 생성(pending)
 *  2) 활성 provider 별 CLI 실행 (병렬/순차) — **트랜잭션 밖**(수초~분 소요)
 *  3) run 행 저장 + comparison 상태 갱신
 *
 * CLI 실행을 DB 트랜잭션으로 감싸지 않는다(커넥션 점유·SQLite writer 1개 제약).
 */
@Service
class ComparisonService(
    private val providerService: ProviderService,
    private val comparisonRepository: ComparisonRepository,
    private val runRepository: ComparisonRunRepository,
    private val cliRunner: CliRunnerService,
    private val cliProperties: CliProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class ComparisonResult(val comparison: Comparison, val runs: List<ComparisonRun>)

    /** 비교 실행. 게스트면 userId=null + guestKey. */
    fun createAndRun(
        prompt: String,
        category: String,
        userId: Long?,
        guestKey: String?,
    ): ComparisonResult {
        val cleaned = prompt.trim()
        require(cleaned.isNotBlank()) { "프롬프트가 비어 있습니다." }
        require(cleaned.length <= MAX_PROMPT) { "프롬프트가 너무 깁니다(최대 $MAX_PROMPT 자)." }

        val providers = providerService.enabledProviders()
        require(providers.isNotEmpty()) { "활성화된 provider 가 없습니다." }

        val comparison = saveComparison(
            Comparison(
                userId = userId,
                guestKey = guestKey,
                category = category.ifBlank { "general" },
                prompt = cleaned,
                status = "pending",
                createdAt = Instant.now().toString(),
            ),
        )
        val comparisonId = requireNotNull(comparison.id)

        val results = execute(providers, cleaned)

        val now = Instant.now().toString()
        val runs = providers.map { provider ->
            val r = results[provider.id]
            runRepository.save(
                ComparisonRun(
                    comparisonId = comparisonId,
                    providerId = provider.id,
                    status = r?.status ?: "error",
                    responseText = r?.responseText,
                    errorText = r?.errorText ?: (if (r == null) "실행 결과 없음" else null),
                    exitCode = r?.exitCode,
                    latencyMs = r?.latencyMs,
                    charCount = r?.responseText?.length,
                    createdAt = now,
                ),
            )
        }
        val anyOk = runs.any { it.status == "ok" }
        comparison.status = if (anyOk) "done" else "error"
        comparison.completedAt = now
        saveComparison(comparison)

        return ComparisonResult(comparison, runs)
    }

    /** provider 별 실행 — parallel 설정에 따라 병렬/순차. 각 run 은 자체 타임아웃 보유. */
    private fun execute(
        providers: List<CliProvider>,
        prompt: String,
    ): Map<String, CliRunnerService.RunResult> {
        if (providers.size <= 1 || !cliProperties.parallel) {
            return providers.associate { p -> p.id to cliRunner.run(p, prompt) }
        }
        val pool = Executors.newFixedThreadPool(providers.size)
        return try {
            val futures = providers.associate { p ->
                p.id to CompletableFuture.supplyAsync({ cliRunner.run(p, prompt) }, pool)
            }
            futures.mapValues { (id, f) ->
                runCatching { f.get() }.getOrElse {
                    log.warn("provider {} future 실패: {}", id, it.message)
                    CliRunnerService.RunResult("error", null, it.message, null, 0)
                }
            }
        } finally {
            pool.shutdown()
        }
    }

    @Transactional
    fun saveComparison(c: Comparison): Comparison = comparisonRepository.save(c)

    @Transactional(readOnly = true)
    fun get(id: Long): ComparisonResult {
        val comparison = comparisonRepository.findById(id).orElseThrow {
            IllegalArgumentException("비교를 찾을 수 없습니다: $id")
        }
        return ComparisonResult(comparison, runRepository.findByComparisonId(id))
    }

    @Transactional(readOnly = true)
    fun recent(): List<ComparisonResult> = attachRuns(comparisonRepository.findTop50ByOrderByIdDesc())

    @Transactional(readOnly = true)
    fun history(userId: Long?, guestKey: String?): List<ComparisonResult> {
        val comparisons = when {
            userId != null -> comparisonRepository.findTop50ByUserIdOrderByIdDesc(userId)
            !guestKey.isNullOrBlank() -> comparisonRepository.findTop50ByGuestKeyOrderByIdDesc(guestKey)
            else -> throw UnauthorizedException("히스토리 식별자(로그인 또는 guestKey)가 필요합니다.")
        }
        return attachRuns(comparisons)
    }

    private fun attachRuns(comparisons: List<Comparison>): List<ComparisonResult> {
        if (comparisons.isEmpty()) return emptyList()
        val runsByCid = runRepository
            .findByComparisonIdIn(comparisons.mapNotNull { it.id })
            .groupBy { it.comparisonId }
        return comparisons.map { ComparisonResult(it, runsByCid[it.id].orEmpty()) }
    }

    private companion object {
        const val MAX_PROMPT = 8000
    }
}
