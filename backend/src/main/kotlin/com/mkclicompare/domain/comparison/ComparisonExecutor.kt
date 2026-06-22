package com.mkclicompare.domain.comparison

import com.mkclicompare.domain.provider.ProviderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * 비교의 CLI 실행을 **백그라운드**에서 수행한다(HTTP 스레드 비점유).
 * 각 provider 가 끝나는 즉시 해당 run 행을 갱신 → 프론트 폴링이 점진적으로 카드를 채운다.
 *
 * 동기 블로킹(수십 초) 대신 비동기 + 폴링으로 모바일 타임아웃/무진행 문제를 해소한다.
 */
@Component
class ComparisonExecutor(
    private val providerService: ProviderService,
    private val comparisonRepository: ComparisonRepository,
    private val runRepository: ComparisonRunRepository,
    private val cliRunner: CliRunnerService,
    private val streamService: ComparisonStreamService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** comparisonId 의 pending run 들을 병렬 실행하고 완료 시마다 갱신, 끝나면 comparison 상태 확정. */
    @Async("comparisonTaskExecutor")
    fun execute(comparisonId: Long) {
        val comparison = comparisonRepository.findById(comparisonId).orElse(null) ?: return
        val runs = runRepository.findByComparisonId(comparisonId)
        val providersById = providerService.allProviders().associateBy { it.id }

        val pool = Executors.newFixedThreadPool(runs.size.coerceAtLeast(1))
        try {
            val futures = runs.map { run ->
                CompletableFuture.runAsync({
                    val provider = providersById[run.providerId]
                    if (provider == null) {
                        updateRun(run.id!!, CliRunnerService.RunResult("error", null, "provider 없음", null, 0))
                        return@runAsync
                    }
                    val result = cliRunner.run(provider, comparison.prompt, run.model)
                    updateRun(run.id!!, result)
                }, pool)
            }
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        } catch (e: Exception) {
            log.error("비교 {} 실행 중 오류: {}", comparisonId, e.message)
        } finally {
            pool.shutdown()
            finalize(comparisonId)
        }
    }

    @Transactional
    fun updateRun(runId: Long, result: CliRunnerService.RunResult) {
        val run = runRepository.findById(runId).orElse(null) ?: return
        run.status = result.status
        run.responseText = result.responseText
        run.errorText = result.errorText
        run.exitCode = result.exitCode
        run.latencyMs = result.latencyMs
        run.charCount = result.responseText?.length
        runRepository.save(run)
        publishSnapshot(run.comparisonId, "update")
    }

    @Transactional
    fun finalize(comparisonId: Long) {
        val comparison = comparisonRepository.findById(comparisonId).orElse(null) ?: return
        val runs = runRepository.findByComparisonId(comparisonId)
        comparison.status = if (runs.any { it.status == "ok" }) "done" else "error"
        comparison.completedAt = Instant.now().toString()
        comparisonRepository.save(comparison)
        streamService.complete(comparisonId, ComparisonSnapshot.of(comparison, runs))
    }

    /** 현재 상태 스냅샷을 SSE 구독자에게 푸시(run 완료 즉시). */
    private fun publishSnapshot(comparisonId: Long, event: String) {
        val comparison = comparisonRepository.findById(comparisonId).orElse(null) ?: return
        val runs = runRepository.findByComparisonId(comparisonId)
        streamService.publish(comparisonId, event, ComparisonSnapshot.of(comparison, runs))
    }
}
