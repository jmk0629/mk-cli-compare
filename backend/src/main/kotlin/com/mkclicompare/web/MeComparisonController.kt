package com.mkclicompare.web

import com.mkclicompare.auth.AuthenticatedUser
import com.mkclicompare.domain.comparison.ComparisonService
import com.mkclicompare.web.dto.ComparisonRes
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 내 비교 히스토리 — JWT 인증 필요(`api/me` 하위는 SecurityConfig 에서 보호). */
@RestController
class MeComparisonController(
    private val comparisonService: ComparisonService,
) {
    @GetMapping("/api/me/comparisons")
    fun myComparisons(@AuthenticationPrincipal principal: AuthenticatedUser): List<ComparisonRes> =
        comparisonService.history(userId = principal.userId, guestKey = null).map { ComparisonRes.of(it) }
}
