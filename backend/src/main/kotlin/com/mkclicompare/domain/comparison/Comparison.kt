package com.mkclicompare.domain.comparison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 비교 1건 — 프롬프트 + 카테고리. 게스트는 userId NULL + guestKey 로 식별. */
@Entity
@Table(name = "comparison")
class Comparison(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "user_id")
    var userId: Long? = null,

    @Column(name = "guest_key")
    var guestKey: String? = null,

    @Column(name = "category", nullable = false)
    val category: String = "general",

    @Column(name = "prompt", nullable = false)
    val prompt: String = "",

    @Column(name = "status", nullable = false)
    var status: String = "pending",   // pending | done | error

    @Column(name = "created_at", nullable = false)
    val createdAt: String = "",

    @Column(name = "completed_at")
    var completedAt: String? = null,
)
