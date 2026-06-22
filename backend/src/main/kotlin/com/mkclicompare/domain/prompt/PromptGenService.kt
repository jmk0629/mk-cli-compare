package com.mkclicompare.domain.prompt

import com.mkclicompare.domain.comparison.CliRunnerService
import com.mkclicompare.domain.provider.ProviderService
import org.springframework.stereotype.Service

/**
 * 카테고리에 맞는 비교용 프롬프트를 CLI 로 생성한다(옵트인).
 * 생성기 provider 는 요청값 또는 활성 provider 중 우선순위(claude 선호)로 선택.
 */
@Service
class PromptGenService(
    private val providerService: ProviderService,
    private val cliRunner: CliRunnerService,
) {
    data class GeneratedPrompt(val prompt: String, val providerId: String, val category: String)

    private val metaByCategory = mapOf(
        "character" to "AI 어시스턴트의 캐릭터(롤플레이) 연기 능력을 시험할 흥미로운 역할 지시문 1개를 만들어라. 성격·말투가 분명한 캐릭터와 상황을 담아라.",
        "coding" to "AI 의 코딩 능력을 비교할 짧고 명확한 프로그래밍 요청 1개를 만들어라. 언어/문제는 자유.",
        "summary" to "AI 의 요약 능력을 시험할 요청 1개를 만들어라. 요약할 짧은 지문이나 주제를 포함하라.",
        "reasoning" to "AI 의 논리·추론 능력을 시험할 문제 1개를 만들어라. 단계적 사고가 필요하게.",
        "general" to "AI 의 일반 대화·창작 능력을 볼 흥미로운 프롬프트 1개를 만들어라.",
    )

    fun generate(category: String, providerId: String?): GeneratedPrompt {
        val cat = category.ifBlank { "general" }
        val meta = metaByCategory[cat] ?: metaByCategory.getValue("general")

        val enabled = providerService.enabledProviders()
        require(enabled.isNotEmpty()) { "활성화된 provider 가 없습니다." }
        val provider = providerId?.let { req -> enabled.find { it.id == req } }
            ?: enabled.find { it.id == "claude" }
            ?: enabled.first()

        val instruction = buildString {
            append(meta)
            append("\n\n규칙: 결과는 '")
            append(cat)
            append("' 카테고리의 프롬프트 1개만, 한국어로 출력. 따옴표·번호·머리말·설명·코드펜스 없이 프롬프트 본문만 출력하라.")
        }

        val result = cliRunner.run(provider, instruction, null)
        require(result.status == "ok" && !result.responseText.isNullOrBlank()) {
            "프롬프트 생성에 실패했습니다(${provider.id}). 잠시 후 다시 시도해주세요."
        }
        return GeneratedPrompt(clean(result.responseText!!), provider.id, cat)
    }

    /** CLI 출력에서 따옴표/코드펜스/머리말을 제거해 프롬프트 본문만 남긴다. */
    private fun clean(raw: String): String {
        var s = raw.trim()
        // 코드펜스 제거
        s = s.removePrefix("```").removeSuffix("```").trim()
        s = s.replace(Regex("^```[a-zA-Z]*\\n"), "").trim()
        // "프롬프트:" 같은 머리말 제거
        s = s.replace(Regex("^(프롬프트|prompt|지시문)\\s*[:：]\\s*", RegexOption.IGNORE_CASE), "").trim()
        // 감싼 따옴표 제거
        s = s.removeSurrounding("\"").removeSurrounding("'").removeSurrounding("“", "”").trim()
        return s.take(4000)
    }
}
