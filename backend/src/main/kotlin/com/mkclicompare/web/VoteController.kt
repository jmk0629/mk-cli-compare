package com.mkclicompare.web

import com.mkclicompare.auth.AuthenticatedUser
import com.mkclicompare.domain.vote.VoteService
import com.mkclicompare.web.dto.CastVoteReq
import com.mkclicompare.web.dto.VoteRes
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 블라인드 투표 (게스트 우선). 차원별로 승자 provider 를 기록. */
@RestController
class VoteController(
    private val voteService: VoteService,
) {
    @PostMapping("/api/votes")
    fun cast(
        @AuthenticationPrincipal principal: AuthenticatedUser?,
        @RequestBody @Valid request: CastVoteReq,
    ): VoteRes = VoteRes.from(
        voteService.cast(
            comparisonId = request.comparisonId,
            winnerProviderId = request.winnerProviderId,
            dimension = request.dimension,
            userId = principal?.userId,
            guestKey = request.guestKey,
        ),
    )
}
