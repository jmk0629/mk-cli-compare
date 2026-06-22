package com.mkclicompare.web

import com.mkclicompare.domain.vote.VoteService
import com.mkclicompare.web.dto.LeaderboardRes
import com.mkclicompare.web.dto.RankingRes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** provider 리더보드 (공개) — 승률·차원별 승수·평균 레이턴시·실행 성공률. category/dimension 필터 지원. */
@RestController
class LeaderboardController(
    private val voteService: VoteService,
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
}
