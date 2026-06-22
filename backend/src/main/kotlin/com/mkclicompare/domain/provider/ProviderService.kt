package com.mkclicompare.domain.provider

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** provider 카탈로그 + 프리셋 + 모델 조회. */
@Service
class ProviderService(
    private val providerRepository: CliProviderRepository,
    private val presetRepository: PromptPresetRepository,
    private val modelRepository: ProviderModelRepository,
) {
    @Transactional(readOnly = true)
    fun enabledProviders(): List<CliProvider> = providerRepository.findByEnabledOrderBySortOrderAsc(1)

    @Transactional(readOnly = true)
    fun allProviders(): List<CliProvider> = providerRepository.findAllByOrderBySortOrderAsc()

    @Transactional(readOnly = true)
    fun presets(): List<PromptPreset> = presetRepository.findAllByOrderBySortOrderAsc()

    @Transactional(readOnly = true)
    fun modelsByProvider(): Map<String, List<ProviderModel>> =
        modelRepository.findAllByOrderBySortOrderAsc().groupBy { it.providerId }

    @Transactional(readOnly = true)
    fun models(providerId: String): List<ProviderModel> =
        modelRepository.findByProviderIdOrderBySortOrderAsc(providerId)
}
