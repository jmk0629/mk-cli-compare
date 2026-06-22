package com.mkclicompare.domain.provider

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** provider 카탈로그 + 프리셋 조회. */
@Service
class ProviderService(
    private val providerRepository: CliProviderRepository,
    private val presetRepository: PromptPresetRepository,
) {
    @Transactional(readOnly = true)
    fun enabledProviders(): List<CliProvider> = providerRepository.findByEnabledOrderBySortOrderAsc(1)

    @Transactional(readOnly = true)
    fun allProviders(): List<CliProvider> = providerRepository.findAllByOrderBySortOrderAsc()

    @Transactional(readOnly = true)
    fun presets(): List<PromptPreset> = presetRepository.findAllByOrderBySortOrderAsc()
}
