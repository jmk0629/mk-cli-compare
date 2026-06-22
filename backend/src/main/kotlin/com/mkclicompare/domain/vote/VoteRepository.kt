package com.mkclicompare.domain.vote

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VoteRepository : JpaRepository<Vote, Long> {
    fun countByComparisonId(comparisonId: Long): Long

    /**
     * dimension 별 provider 승리 수 집계(카테고리/차원 선택 필터).
     * [winnerProviderId, dimension, count] 행. 필터 null 이면 전체.
     */
    @Query(
        "SELECT v.winnerProviderId, v.dimension, COUNT(v) " +
            "FROM Vote v, Comparison c " +
            "WHERE v.comparisonId = c.id " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:dimension IS NULL OR v.dimension = :dimension) " +
            "GROUP BY v.winnerProviderId, v.dimension",
    )
    fun aggregateWinsFiltered(category: String?, dimension: String?): List<Array<Any>>

    /**
     * 1:1(head-to-head) 집계: 한 투표는 winner 가 같은 비교의 다른 ok 참가자들을 각각 이긴 것.
     * [winnerProviderId, loserProviderId, count] 행.
     */
    @Query(
        "SELECT v.winnerProviderId, r.providerId, COUNT(v) " +
            "FROM Vote v, ComparisonRun r " +
            "WHERE r.comparisonId = v.comparisonId " +
            "AND r.providerId <> v.winnerProviderId " +
            "AND r.status = 'ok' " +
            "GROUP BY v.winnerProviderId, r.providerId",
    )
    fun aggregateHeadToHead(): List<Array<Any>>
}
