package com.mkclicompare.domain.provider

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 카테고리별 프롬프트 프리셋(데이터). 사용자가 빠르게 비교를 시작하도록. */
@Entity
@Table(name = "prompt_preset")
class PromptPreset(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "category", nullable = false)
    val category: String = "general",

    @Column(name = "title", nullable = false)
    val title: String = "",

    @Column(name = "prompt", nullable = false)
    val prompt: String = "",

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
)
