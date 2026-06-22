package com.mkclicompare.domain.vote

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VoteRepository : JpaRepository<Vote, Long> {
    fun countByComparisonId(comparisonId: Long): Long

    /** dimension 별 provider 승리 수 집계. [winnerProviderId, dimension, count] 행. */
    @Query(
        "SELECT v.winnerProviderId, v.dimension, COUNT(v) " +
            "FROM Vote v GROUP BY v.winnerProviderId, v.dimension",
    )
    fun aggregateWins(): List<Array<Any>>
}
