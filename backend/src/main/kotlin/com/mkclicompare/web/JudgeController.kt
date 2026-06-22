package com.mkclicompare.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.mkclicompare.domain.judge.JudgeService
import com.mkclicompare.domain.judge.JudgeVerdict
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** AI 자동 심판 (공개, 옵트인). POST 로 평결 생성, GET 으로 최근 평결 조회. */
@RestController
class JudgeController(
    private val judgeService: JudgeService,
    private val objectMapper: ObjectMapper,
) {
    /** 비교를 AI 심판에게 평가받는다(CLI 1회 호출, 수 초~수십 초). */
    @PostMapping("/api/comparisons/{id}/judge")
    fun judge(@PathVariable id: Long, @RequestBody(required = false) req: JudgeReq?): JudgeVerdictRes =
        toRes(judgeService.judge(id, req?.provider))

    /** 최근 평결 조회(없으면 204). */
    @GetMapping("/api/comparisons/{id}/judge")
    fun latest(@PathVariable id: Long): ResponseEntity<JudgeVerdictRes> {
        val v = judgeService.latest(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(toRes(v))
    }

    private fun toRes(v: JudgeVerdict): JudgeVerdictRes {
        val scores: List<ScoreItemRes> = if (v.status == "ok") {
            runCatching {
                val node = objectMapper.readTree(v.verdictJson)
                node["scores"]?.map {
                    ScoreItemRes(
                        providerId = it["providerId"].asText(),
                        score = it["score"].asDouble(),
                        reason = it["reason"]?.asText().orEmpty(),
                    )
                } ?: emptyList()
            }.getOrDefault(emptyList())
        } else emptyList()
        val summary = runCatching { objectMapper.readTree(v.verdictJson)["summary"]?.asText() }.getOrNull()
        return JudgeVerdictRes(
            id = requireNotNull(v.id),
            judgeProviderId = v.judgeProviderId,
            winnerProviderId = v.winnerProviderId,
            summary = summary,
            scores = scores.sortedByDescending { it.score },
            status = v.status,
            createdAt = v.createdAt,
        )
    }
}

data class JudgeReq(val provider: String? = null)

data class ScoreItemRes(val providerId: String, val score: Double, val reason: String)

data class JudgeVerdictRes(
    val id: Long,
    val judgeProviderId: String,
    val winnerProviderId: String?,
    val summary: String?,
    val scores: List<ScoreItemRes>,
    val status: String,
    val createdAt: String,
)
