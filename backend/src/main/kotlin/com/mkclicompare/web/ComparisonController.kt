package com.mkclicompare.web

import com.mkclicompare.auth.AuthenticatedUser
import com.mkclicompare.domain.comparison.ComparisonService
import com.mkclicompare.domain.comparison.ComparisonSnapshot
import com.mkclicompare.domain.comparison.ComparisonStreamService
import com.mkclicompare.web.dto.ComparisonRes
import com.mkclicompare.web.dto.CreateComparisonReq
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * 비교 실행/조회 (게스트 우선 — 로그인 불필요).
 * 로그인 사용자면 principal.userId 로 소유 기록, 게스트면 guestKey 로.
 */
@RestController
class ComparisonController(
    private val comparisonService: ComparisonService,
    private val streamService: ComparisonStreamService,
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

    /**
     * 비교 진행 실시간 스트림(SSE). 구독 즉시 현재 스냅샷을 보내고, run 완료마다 push.
     * 완료 시 `done` 이벤트 후 종료. (프론트는 실패 시 GET 폴링으로 폴백)
     */
    @GetMapping("/api/comparisons/{id}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@PathVariable id: Long): SseEmitter {
        val emitter = streamService.subscribe(id)
        val result = comparisonService.get(id)
        val snapshot = ComparisonSnapshot.of(result.comparison, result.runs)
        try {
            if (result.comparison.status == "pending") {
                emitter.send(SseEmitter.event().name("update").data(snapshot))
            } else {
                // 이미 완료 — 스냅샷을 done 으로 보내고 종료.
                emitter.send(SseEmitter.event().name("done").data(snapshot))
                emitter.complete()
            }
        } catch (e: Exception) {
            emitter.completeWithError(e)
        }
        return emitter
    }

    /** 최근 공개 비교 피드. */
    @GetMapping("/api/comparisons")
    fun recent(): List<ComparisonRes> = comparisonService.recent().map { ComparisonRes.of(it) }
}
