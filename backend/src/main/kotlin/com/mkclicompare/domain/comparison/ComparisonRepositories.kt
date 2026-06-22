package com.mkclicompare.domain.comparison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ComparisonRepository : JpaRepository<Comparison, Long> {
    fun findTop50ByOrderByIdDesc(): List<Comparison>
    fun findTop50ByUserIdOrderByIdDesc(userId: Long): List<Comparison>
    fun findTop50ByGuestKeyOrderByIdDesc(guestKey: String): List<Comparison>
}

interface ComparisonRunRepository : JpaRepository<ComparisonRun, Long> {
    fun findByComparisonId(comparisonId: Long): List<ComparisonRun>
    fun findByComparisonIdAndProviderId(comparisonId: Long, providerId: String): ComparisonRun?
    fun findByComparisonIdIn(comparisonIds: List<Long>): List<ComparisonRun>

    /** provider 별 실행 통계(카테고리 선택 필터): [providerId, totalRuns, okRuns, avgLatencyMs]. */
    @Query(
        "SELECT r.providerId, COUNT(r), " +
            "SUM(CASE WHEN r.status = 'ok' THEN 1 ELSE 0 END), " +
            "AVG(CASE WHEN r.status = 'ok' THEN r.latencyMs END) " +
            "FROM ComparisonRun r, Comparison c " +
            "WHERE r.comparisonId = c.id " +
            "AND (:category IS NULL OR c.category = :category) " +
            "GROUP BY r.providerId",
    )
    fun aggregateRunStatsFiltered(category: String?): List<Array<Any>>
}
