package com.mkclicompare.web

import com.mkclicompare.domain.provider.ProviderService
import com.mkclicompare.web.dto.PresetRes
import com.mkclicompare.web.dto.ProviderRes
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 비교 대상 provider 카탈로그 + 카테고리 프리셋 (공개). */
@RestController
class ProviderController(
    private val providerService: ProviderService,
) {
    /** 활성 provider 목록(비교 대상). */
    @GetMapping("/api/providers")
    fun providers(): List<ProviderRes> =
        providerService.enabledProviders().map { ProviderRes.from(it) }

    /** 카테고리별 프롬프트 프리셋. */
    @GetMapping("/api/presets")
    fun presets(): List<PresetRes> =
        providerService.presets().map { PresetRes.from(it) }
}
