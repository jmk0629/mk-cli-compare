package com.mkclicompare.domain.provider

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** provider 가 선택 가능한 모델(데이터). model_arg 는 CLI 에 그대로 넘기는 whitelist 인자. */
@Entity
@Table(name = "provider_model")
class ProviderModel(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "provider_id", nullable = false)
    val providerId: String = "",

    @Column(name = "model_arg", nullable = false)
    val modelArg: String = "",

    @Column(name = "label", nullable = false)
    val label: String = "",

    @Column(name = "is_default", nullable = false)
    val isDefault: Int = 0,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
)
