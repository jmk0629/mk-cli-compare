package com.mkclicompare.web

import com.mkclicompare.domain.comparison.ComparisonRepository
import com.mkclicompare.domain.vote.VoteRepository
import com.mkclicompare.domain.vote.VoteService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 사이트 통계 개요 (공개) — 총 비교/투표 수 + 최다승/최속 provider. */
@RestController
class StatsController(
    private val comparisonRepository: ComparisonRepository,
    private val voteRepository: VoteRepository,
    private val voteService: VoteService,
) {
    @GetMapping("/api/stats")
    fun stats(): StatsRes {
        val rankings = voteService.leaderboard()
        val top = rankings.maxByOrNull { it.totalWins }?.takeIf { it.totalWins > 0 }
        val fastest = rankings
            .filter { it.avgLatencyMs != null && it.totalRuns > 0 }
            .minByOrNull { it.avgLatencyMs!! }
        return StatsRes(
            totalComparisons = comparisonRepository.count(),
            totalVotes = voteRepository.count(),
            topProvider = top?.let { ProviderBrief(it.providerId, it.displayName, it.color, it.totalWins) },
            fastestProvider = fastest?.let { ProviderBrief(it.providerId, it.displayName, it.color, it.avgLatencyMs!!) },
        )
    }
}

data class ProviderBrief(val id: String, val displayName: String, val color: String, val value: Long)

data class StatsRes(
    val totalComparisons: Long,
    val totalVotes: Long,
    val topProvider: ProviderBrief?,
    val fastestProvider: ProviderBrief?,
)
