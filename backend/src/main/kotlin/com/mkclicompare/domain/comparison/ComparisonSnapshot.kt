package com.mkclicompare.domain.comparison

/**
 * SSE 푸시용 직렬화 스냅샷. JPA Entity 직접 노출을 피하면서 web 의 ComparisonRes 와 **동일한 필드명**을 유지
 * → 프론트가 REST/SSE 를 같은 zod 스키마로 파싱한다.
 */
data class RunSnapshot(
    val providerId: String,
    val model: String?,
    val status: String,
    val responseText: String?,
    val errorText: String?,
    val exitCode: Int?,
    val latencyMs: Long?,
    val charCount: Int?,
)

data class ComparisonSnapshot(
    val id: Long,
    val category: String,
    val prompt: String,
    val status: String,
    val createdAt: String,
    val completedAt: String?,
    val runs: List<RunSnapshot>,
) {
    companion object {
        fun of(c: Comparison, runs: List<ComparisonRun>) = ComparisonSnapshot(
            id = requireNotNull(c.id),
            category = c.category,
            prompt = c.prompt,
            status = c.status,
            createdAt = c.createdAt,
            completedAt = c.completedAt,
            runs = runs.map {
                RunSnapshot(it.providerId, it.model, it.status, it.responseText, it.errorText, it.exitCode, it.latencyMs, it.charCount)
            },
        )
    }
}
