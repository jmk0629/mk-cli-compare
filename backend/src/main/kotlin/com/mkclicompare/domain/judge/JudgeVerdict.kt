package com.mkclicompare.domain.judge

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

/** AI 심판 평결. verdict_json 에 점수/이유 구조 저장, winner 만 컬럼 승격. */
@Entity
@Table(name = "judge_verdict")
class JudgeVerdict(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "comparison_id", nullable = false)
    val comparisonId: Long = 0,

    @Column(name = "judge_provider_id", nullable = false)
    val judgeProviderId: String = "",

    @Column(name = "judge_model")
    val judgeModel: String? = null,

    @Column(name = "winner_provider_id")
    val winnerProviderId: String? = null,

    @Column(name = "verdict_json", nullable = false)
    val verdictJson: String = "{}",

    @Column(name = "status", nullable = false)
    val status: String = "ok",

    @Column(name = "created_at", nullable = false)
    val createdAt: String = "",
)

interface JudgeVerdictRepository : JpaRepository<JudgeVerdict, Long> {
    fun findTop1ByComparisonIdOrderByIdDesc(comparisonId: Long): JudgeVerdict?
    fun countByWinnerProviderId(providerId: String): Long
}
