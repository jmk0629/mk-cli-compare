package com.mkclicompare.domain.vote

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 블라인드 투표 — 한 comparison 에서 어떤 provider 가 차원별(dimension)로 이겼나. */
@Entity
@Table(name = "vote")
class Vote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "comparison_id", nullable = false)
    val comparisonId: Long = 0,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Column(name = "guest_key")
    val guestKey: String? = null,

    @Column(name = "dimension", nullable = false)
    val dimension: String = "overall",   // overall | quality | speed | persona | creativity

    @Column(name = "winner_provider_id", nullable = false)
    val winnerProviderId: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: String = "",
)
