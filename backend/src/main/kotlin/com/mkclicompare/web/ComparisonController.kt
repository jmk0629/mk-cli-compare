package com.mkclicompare.web

import com.mkclicompare.auth.AuthenticatedUser
import com.mkclicompare.domain.comparison.ComparisonService
import com.mkclicompare.web.dto.ComparisonRes
import com.mkclicompare.web.dto.CreateComparisonReq
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 비교 실행/조회 (게스트 우선 — 로그인 불필요).
 * 로그인 사용자면 principal.userId 로 소유 기록, 게스트면 guestKey 로.
 */
@RestController
class ComparisonController(
    private val comparisonService: ComparisonService,
) {
    /** 비교 생성 — pending 상태로 즉시 반환(백그라운드 실행). 진행은 GET 폴링으로 추적. */
    @PostMapping("/api/comparisons")
    fun create(
        @AuthenticationPrincipal principal: AuthenticatedUser?,
        @RequestBody @Valid request: CreateComparisonReq,
    ): ComparisonRes = ComparisonRes.of(
        comparisonService.createAsync(
            prompt = request.prompt,
            category = request.category,
            userId = principal?.userId,
            guestKey = request.guestKey,
            models = request.models,
        ),
    )

    /** 단일 비교 조회(결과 카드 재방문/공유). */
    @GetMapping("/api/comparisons/{id}")
    fun get(@PathVariable id: Long): ComparisonRes = ComparisonRes.of(comparisonService.get(id))

    /** 최근 공개 비교 피드. */
    @GetMapping("/api/comparisons")
    fun recent(): List<ComparisonRes> = comparisonService.recent().map { ComparisonRes.of(it) }
}
