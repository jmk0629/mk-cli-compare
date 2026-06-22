package com.mkclicompare.web

import com.mkclicompare.domain.provider.ProviderService
import com.mkclicompare.domain.vote.VoteRepository
import com.mkclicompare.domain.vote.VoteService
import com.mkclicompare.web.dto.LeaderboardRes
import com.mkclicompare.web.dto.RankingRes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** provider 리더보드 (공개) — 승률·차원별 승수·평균 레이턴시·실행 성공률 + head-to-head. */
@RestController
class LeaderboardController(
    private val voteService: VoteService,
    private val voteRepository: VoteRepository,
    private val providerService: ProviderService,
) {
    @GetMapping("/api/leaderboard")
    fun leaderboard(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) dimension: String?,
    ): LeaderboardRes =
        LeaderboardRes(
            voteService.leaderboard(category?.ifBlank { null }, dimension?.ifBlank { null })
                .map { RankingRes.from(it) },
        )

    /** 1:1 승률 매트릭스(공개) — 기존 투표를 쌍으로 재집계. */
    @GetMapping("/api/leaderboard/h2h")
    fun headToHead(): H2hRes {
        val pairs = voteRepository.aggregateHeadToHead().map { row ->
            H2hPair(winner = row[0] as String, loser = row[1] as String, wins = (row[2] as Number).toLong())
        }
        val providers = providerService.enabledProviders()
            .map { ProviderBrief(it.id, it.displayName, it.color, 0) }
        return H2hRes(providers, pairs)
    }
}

data class H2hPair(val winner: String, val loser: String, val wins: Long)

data class H2hRes(val providers: List<ProviderBrief>, val pairs: List<H2hPair>)
