package com.mkclicompare.domain.comparison

import com.mkclicompare.domain.provider.ProviderService
import com.mkclicompare.web.error.UnauthorizedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 비교 오케스트레이션(생성/조회).
 *  1) comparison(pending) + provider별 pending run 즉시 저장
 *  2) ComparisonExecutor 로 백그라운드 실행 트리거(비블로킹)
 *  3) 프론트는 폴링으로 진행 추적
 *
 * 실제 CLI 실행/갱신은 ComparisonExecutor 가 트랜잭션 밖 백그라운드에서 수행.
 */
@Service
class ComparisonService(
    private val providerService: ProviderService,
    private val comparisonRepository: ComparisonRepository,
    private val runRepository: ComparisonRunRepository,
    private val executor: ComparisonExecutor,
) {

    data class ComparisonResult(val comparison: Comparison, val runs: List<ComparisonRun>)

    /**
     * 비교 생성 — comparison(pending) + provider별 pending run 을 즉시 저장하고 **백그라운드 실행을 트리거**한 뒤
     * 곧바로 반환한다(HTTP 비블로킹). 프론트는 `GET /api/comparisons/{id}` 폴링으로 진행을 추적.
     * 게스트면 userId=null + guestKey. models: providerId → 선택 모델(model_arg).
     */
    @Transactional
    fun createAsync(
        prompt: String,
        category: String,
        userId: Long?,
        guestKey: String?,
        models: Map<String, String>? = null,
    ): ComparisonResult {
        val cleaned = prompt.trim()
        require(cleaned.isNotBlank()) { "프롬프트가 비어 있습니다." }
        require(cleaned.length <= MAX_PROMPT) { "프롬프트가 너무 깁니다(최대 $MAX_PROMPT 자)." }

        val providers = providerService.enabledProviders()
        require(providers.isNotEmpty()) { "활성화된 provider 가 없습니다." }

        // 모델 whitelist 검증: 요청 모델이 provider 카탈로그에 있을 때만 채택(임의 문자열 차단).
        val allowedModels = providerService.modelsByProvider()
        val resolvedModels: Map<String, String?> = providers.associate { p ->
            val requested = models?.get(p.id)
            val valid = requested?.takeIf { req -> allowedModels[p.id]?.any { it.modelArg == req } == true }
            p.id to valid
        }

        val now = Instant.now().toString()
        val comparison = comparisonRepository.save(
            Comparison(
                userId = userId,
                guestKey = guestKey,
                category = category.ifBlank { "general" },
                prompt = cleaned,
                status = "pending",
                createdAt = now,
            ),
        )
        val comparisonId = requireNotNull(comparison.id)

        // provider별 pending run 을 미리 만들어 프론트가 즉시 3개 카드를 그릴 수 있게 한다.
        val runs = providers.map { provider ->
            runRepository.save(
                ComparisonRun(
                    comparisonId = comparisonId,
                    providerId = provider.id,
                    model = resolvedModels[provider.id],
                    status = "pending",
                    createdAt = now,
                ),
            )
        }

        executor.execute(comparisonId) // @Async — 트랜잭션 커밋 후 별도 스레드에서 실행
        return ComparisonResult(comparison, runs)
    }

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
