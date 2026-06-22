package com.mkclicompare.domain.judge

import com.fasterxml.jackson.databind.ObjectMapper
import com.mkclicompare.domain.comparison.ComparisonRepository
import com.mkclicompare.domain.comparison.ComparisonRun
import com.mkclicompare.domain.comparison.ComparisonRunRepository
import com.mkclicompare.domain.provider.ProviderService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * AI 자동 심판(LLM-as-judge). 완료된 비교의 ok 응답들을 A/B/C 블라인드로 심판 CLI 에 보내
 * 순위·점수·이유 JSON 을 받아 provider 로 환원해 저장. 사람 투표와 별개의 객관 평가.
 */
@Service
class JudgeService(
    private val providerService: ProviderService,
    private val comparisonRepository: ComparisonRepository,
    private val runRepository: ComparisonRunRepository,
    private val cliRunner: com.mkclicompare.domain.comparison.CliRunnerService,
    private val verdictRepository: JudgeVerdictRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class ScoreItem(val providerId: String, val score: Double, val reason: String)
    data class Verdict(val winnerProviderId: String?, val summary: String, val scores: List<ScoreItem>)

    @Transactional(readOnly = true)
    fun latest(comparisonId: Long): JudgeVerdict? =
        verdictRepository.findTop1ByComparisonIdOrderByIdDesc(comparisonId)

    @Transactional
    fun judge(comparisonId: Long, judgeProviderId: String?): JudgeVerdict {
        val comparison = comparisonRepository.findById(comparisonId).orElseThrow {
            IllegalArgumentException("비교를 찾을 수 없습니다: $comparisonId")
        }
        val okRuns = runRepository.findByComparisonId(comparisonId)
            .filter { it.status == "ok" && !it.responseText.isNullOrBlank() }
        require(okRuns.size >= 2) { "심판하려면 성공한 응답이 2개 이상 필요합니다." }

        // 블라인드 라벨 A,B,C... ↔ providerId 매핑(라벨은 정체를 가린다).
        val labeled = okRuns.mapIndexed { i, run -> ('A' + i).toString() to run }
        val labelToProvider = labeled.associate { (label, run) -> label to run.providerId }

        val enabled = providerService.enabledProviders()
        val judge = judgeProviderId?.let { req -> enabled.find { it.id == req } }
            ?: enabled.find { it.id == "claude" }
            ?: enabled.first()

        val now = Instant.now().toString()
        val prompt = buildPrompt(comparison.prompt, labeled)
        val result = cliRunner.run(judge, prompt, null)

        if (result.status != "ok" || result.responseText.isNullOrBlank()) {
            return verdictRepository.save(
                JudgeVerdict(
                    comparisonId = comparisonId,
                    judgeProviderId = judge.id,
                    verdictJson = """{"error":"심판 호출 실패"}""",
                    status = "error",
                    createdAt = now,
                ),
            )
        }

        val verdict = parseVerdict(result.responseText!!, labelToProvider)
            ?: return verdictRepository.save(
                JudgeVerdict(
                    comparisonId = comparisonId,
                    judgeProviderId = judge.id,
                    verdictJson = """{"error":"평결 파싱 실패"}""",
                    status = "error",
                    createdAt = now,
                ),
            )

        return verdictRepository.save(
            JudgeVerdict(
                comparisonId = comparisonId,
                judgeProviderId = judge.id,
                winnerProviderId = verdict.winnerProviderId,
                verdictJson = objectMapper.writeValueAsString(verdict),
                status = "ok",
                createdAt = now,
            ),
        )
    }

    private fun buildPrompt(originalPrompt: String, labeled: List<Pair<String, ComparisonRun>>): String =
        buildString {
            append("너는 엄정하고 공정한 심사위원이다. 아래 [원본 프롬프트]에 대한 여러 AI의 응답을 평가하라.\n")
            append("각 응답을 정확성·완성도·명료성·유용성을 종합해 0~10점(소수 한 자리 허용)으로 채점하고 한 줄 이유를 달아라.\n")
            append("어느 AI인지는 알 수 없으니 오직 응답 내용만으로 공정하게 판단하라.\n\n")
            append("반드시 아래 JSON 형식만 출력하라. 코드펜스(```)나 다른 설명을 절대 붙이지 마라:\n")
            append("""{"rankings":[{"label":"A","score":8.5,"reason":"한 줄 이유"}],"summary":"전체 총평 한 줄"}""")
            append("\n\n[원본 프롬프트]\n")
            append(originalPrompt)
            labeled.forEach { (label, run) ->
                append("\n\n[응답 $label]\n")
                append(run.responseText)
            }
        }

    /** CLI 출력에서 첫 '{' ~ 마지막 '}' 를 잘라 JSON 파싱. 실패 시 null. */
    private fun parseVerdict(raw: String, labelToProvider: Map<String, String>): Verdict? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val node = objectMapper.readTree(raw.substring(start, end + 1))
            val rankings = node["rankings"] ?: return null
            val scores = rankings.mapNotNull { r ->
                val label = r["label"]?.asText()?.trim()?.uppercase() ?: return@mapNotNull null
                val pid = labelToProvider[label] ?: return@mapNotNull null
                ScoreItem(
                    providerId = pid,
                    score = r["score"]?.asDouble() ?: 0.0,
                    reason = r["reason"]?.asText()?.trim().orEmpty(),
                )
            }
            if (scores.isEmpty()) return null
            val winner = scores.maxByOrNull { it.score }?.providerId
            Verdict(winner, node["summary"]?.asText()?.trim().orEmpty(), scores)
        } catch (e: Exception) {
            log.warn("평결 JSON 파싱 실패: {}", e.message)
            null
        }
    }
}
