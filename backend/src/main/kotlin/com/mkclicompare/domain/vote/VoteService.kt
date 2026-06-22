package com.mkclicompare.domain.vote

import com.mkclicompare.domain.comparison.ComparisonRepository
import com.mkclicompare.domain.comparison.ComparisonRunRepository
import com.mkclicompare.domain.provider.ProviderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** 블라인드 투표 기록 + 리더보드(승률·평균 레이턴시·실행 성공률) 집계. */
@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val comparisonRepository: ComparisonRepository,
    private val runRepository: ComparisonRunRepository,
    private val providerService: ProviderService,
) {
    val allowedDimensions = setOf("overall", "quality", "speed", "persona", "creativity")

    @Transactional
    fun cast(
        comparisonId: Long,
        winnerProviderId: String,
        dimension: String,
        userId: Long?,
        guestKey: String?,
    ): Vote {
        require(dimension in allowedDimensions) { "허용되지 않은 차원: $dimension" }
        comparisonRepository.findById(comparisonId).orElseThrow {
            IllegalArgumentException("비교를 찾을 수 없습니다: $comparisonId")
        }
        val runProviderIds = runRepository.findByComparisonId(comparisonId).map { it.providerId }.toSet()
        require(winnerProviderId in runProviderIds) {
            "이 비교에 참여하지 않은 provider 입니다: $winnerProviderId"
        }
        return voteRepository.save(
            Vote(
                comparisonId = comparisonId,
                userId = userId,
                guestKey = guestKey,
                dimension = dimension,
                winnerProviderId = winnerProviderId,
                createdAt = Instant.now().toString(),
            ),
        )
    }

    data class ProviderRanking(
        val providerId: String,
        val displayName: String,
        val color: String,
        val vendor: String,
        val totalWins: Long,
        val winsByDimension: Map<String, Long>,
        val totalRuns: Long,
        val okRuns: Long,
        val okRate: Double,
        val avgLatencyMs: Long?,
    )

    @Transactional(readOnly = true)
    fun leaderboard(): List<ProviderRanking> {
        // 투표 집계: providerId → (dimension → count)
        val winsByProvider = mutableMapOf<String, MutableMap<String, Long>>()
        voteRepository.aggregateWins().forEach { row ->
            val pid = row[0] as String
            val dim = row[1] as String
            val cnt = (row[2] as Number).toLong()
            winsByProvider.getOrPut(pid) { mutableMapOf() }[dim] = cnt
        }
        // 실행 통계: providerId → (total, ok, avgLatency)
        val runStats = runRepository.aggregateRunStats().associate { row ->
            val pid = row[0] as String
            val total = (row[1] as Number).toLong()
            val ok = (row[2] as Number?)?.toLong() ?: 0L
            val avg = (row[3] as Number?)?.toDouble()
            pid to Triple(total, ok, avg)
        }

        return providerService.allProviders().map { p ->
            val wins = winsByProvider[p.id].orEmpty()
            val (total, ok, avg) = runStats[p.id] ?: Triple(0L, 0L, null)
            ProviderRanking(
                providerId = p.id,
                displayName = p.displayName,
                color = p.color,
                vendor = p.vendor,
                totalWins = wins.values.sum(),
                winsByDimension = wins,
                totalRuns = total,
                okRuns = ok,
                okRate = if (total > 0) ok.toDouble() / total else 0.0,
                avgLatencyMs = avg?.toLong(),
            )
        }.sortedWith(compareByDescending<ProviderRanking> { it.totalWins }.thenBy { it.avgLatencyMs ?: Long.MAX_VALUE })
    }
}
