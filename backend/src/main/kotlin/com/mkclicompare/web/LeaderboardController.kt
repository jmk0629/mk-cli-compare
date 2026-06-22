package com.mkclicompare.web

import com.mkclicompare.domain.vote.VoteService
import com.mkclicompare.web.dto.LeaderboardRes
import com.mkclicompare.web.dto.RankingRes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** provider 리더보드 (공개) — 승률·차원별 승수·평균 레이턴시·실행 성공률. */
@RestController
class LeaderboardController(
    private val voteService: VoteService,
) {
    @GetMapping("/api/leaderboard")
    fun leaderboard(): LeaderboardRes =
        LeaderboardRes(voteService.leaderboard().map { RankingRes.from(it) })
}
