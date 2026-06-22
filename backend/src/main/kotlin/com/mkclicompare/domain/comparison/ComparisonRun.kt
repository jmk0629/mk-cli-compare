package com.mkclicompare.domain.comparison

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** provider 별 CLI 실행 결과. 실패도 행으로 기록(graceful degradation). */
@Entity
@Table(name = "comparison_run")
class ComparisonRun(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "comparison_id", nullable = false)
    val comparisonId: Long = 0,

    @Column(name = "provider_id", nullable = false)
    val providerId: String = "",

    @Column(name = "status", nullable = false)
    var status: String = "pending",   // pending | ok | error | timeout

    @Column(name = "response_text")
    var responseText: String? = null,

    @Column(name = "error_text")
    var errorText: String? = null,

    @Column(name = "exit_code")
    var exitCode: Int? = null,

    @Column(name = "latency_ms")
    var latencyMs: Long? = null,

    @Column(name = "char_count")
    var charCount: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: String = "",
)
