package com.mkclicompare.domain.provider

import org.springframework.data.jpa.repository.JpaRepository

interface CliProviderRepository : JpaRepository<CliProvider, String> {
    fun findByEnabledOrderBySortOrderAsc(enabled: Int): List<CliProvider>
    fun findAllByOrderBySortOrderAsc(): List<CliProvider>
}

interface PromptPresetRepository : JpaRepository<PromptPreset, String> {
    fun findAllByOrderBySortOrderAsc(): List<PromptPreset>
}

interface ProviderModelRepository : JpaRepository<ProviderModel, String> {
    fun findAllByOrderBySortOrderAsc(): List<ProviderModel>
    fun findByProviderIdOrderBySortOrderAsc(providerId: String): List<ProviderModel>
}
