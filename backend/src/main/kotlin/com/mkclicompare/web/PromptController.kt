package com.mkclicompare.web

import com.mkclicompare.domain.prompt.PromptGenService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 프롬프트 생성(공개, 옵트인) — 카테고리 맞춤 프롬프트를 CLI 로 생성해 입력창에 가져오기. */
@RestController
class PromptController(
    private val promptGenService: PromptGenService,
) {
    @PostMapping("/api/prompts/generate")
    fun generate(@RequestBody req: GeneratePromptReq): GeneratePromptRes {
        val g = promptGenService.generate(req.category, req.provider)
        return GeneratePromptRes(prompt = g.prompt, providerId = g.providerId, category = g.category)
    }
}

data class GeneratePromptReq(val category: String = "general", val provider: String? = null)

data class GeneratePromptRes(val prompt: String, val providerId: String, val category: String)
