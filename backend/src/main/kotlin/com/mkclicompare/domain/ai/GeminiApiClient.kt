package com.mkclicompare.domain.ai

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.mkclicompare.config.CliProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

/**
 * Gemini(Generative Language API) 경량 클라이언트 — CLI 미설치 환경의 API fallback.
 * 키 없거나 실패 시 null. provider 카탈로그에서 runner_kind='api' 인 google provider 가 사용.
 */
@Component
class GeminiApiClient(
    private val props: CliProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder().build()

    fun generate(prompt: String): String? {
        if (!props.geminiApiEnabled) return null
        val uri = UriComponentsBuilder
            .fromUriString("https://generativelanguage.googleapis.com")
            .path("/v1beta/models/{model}:generateContent")
            .build(mapOf("model" to props.geminiModel))
        val req = GenReq(
            contents = listOf(GenReq.Content("user", listOf(GenReq.Part(prompt)))),
            generationConfig = GenReq.GenerationConfig(temperature = 0.8, maxOutputTokens = 800),
        )
        return try {
            val res = restClient.post()
                .uri(uri)
                .header("x-goog-api-key", props.geminiApiKey)
                .header("content-type", "application/json")
                .body(req)
                .retrieve()
                .body(GenRes::class.java)
            res?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.ifBlank { null }
        } catch (e: Exception) {
            log.warn("Gemini API 호출 실패: {}", e.message)
            null
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GenReq(
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null,
    ) {
        data class Content(val role: String, val parts: List<Part>)
        data class Part(val text: String)
        data class GenerationConfig(val temperature: Double? = null, val maxOutputTokens: Int? = null)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GenRes(val candidates: List<Candidate>? = null) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Candidate(val content: Content? = null)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Content(val parts: List<Part>? = null)
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Part(val text: String? = null)
    }
}
