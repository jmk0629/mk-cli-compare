package com.mkclicompare.web.dto

import com.mkclicompare.domain.comparison.Comparison
import com.mkclicompare.domain.comparison.ComparisonRun
import com.mkclicompare.domain.comparison.ComparisonService
import com.mkclicompare.domain.provider.CliProvider
import com.mkclicompare.domain.provider.PromptPreset
import com.mkclicompare.domain.vote.VoteService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// ── Provider / Preset ──
data class ProviderRes(
    val id: String,
    val displayName: String,
    val vendor: String,
    val runnerKind: String,
    val model: String?,
    val color: String,
    val icon: String?,
) {
    companion object {
        fun from(p: CliProvider) = ProviderRes(p.id, p.displayName, p.vendor, p.runnerKind, p.model, p.color, p.icon)
    }
}

data class PresetRes(
    val id: String,
    val category: String,
    val title: String,
    val prompt: String,
    val description: String?,
) {
    companion object {
        fun from(p: PromptPreset) = PresetRes(p.id, p.category, p.title, p.prompt, p.description)
    }
}

// ── Comparison ──
data class CreateComparisonReq(
    @field:NotBlank(message = "프롬프트를 입력하세요.")
    @field:Size(max = 8000, message = "프롬프트는 최대 8000자입니다.")
    val prompt: String,
    val category: String = "general",
    val guestKey: String? = null,
)

data class RunRes(
    val providerId: String,
    val status: String,
    val responseText: String?,
    val errorText: String?,
    val exitCode: Int?,
    val latencyMs: Long?,
    val charCount: Int?,
) {
    companion object {
        fun from(r: ComparisonRun) = RunRes(
            providerId = r.providerId,
            status = r.status,
            responseText = r.responseText,
            errorText = r.errorText,
            exitCode = r.exitCode,
            latencyMs = r.latencyMs,
            charCount = r.charCount,
        )
    }
}

data class ComparisonRes(
    val id: Long,
    val category: String,
    val prompt: String,
    val status: String,
    val createdAt: String,
    val completedAt: String?,
    val runs: List<RunRes>,
) {
    companion object {
        fun from(c: Comparison, runs: List<ComparisonRun>) = ComparisonRes(
            id = requireNotNull(c.id),
            category = c.category,
            prompt = c.prompt,
            status = c.status,
            createdAt = c.createdAt,
            completedAt = c.completedAt,
            runs = runs.map { RunRes.from(it) },
        )

        fun of(result: ComparisonService.ComparisonResult) = from(result.comparison, result.runs)
    }
}

// ── Vote ──
data class CastVoteReq(
    val comparisonId: Long,
    @field:NotBlank val winnerProviderId: String,
    val dimension: String = "overall",
    val guestKey: String? = null,
)

data class VoteRes(val id: Long, val comparisonId: Long, val winnerProviderId: String, val dimension: String) {
    companion object {
        fun from(v: com.mkclicompare.domain.vote.Vote) =
            VoteRes(requireNotNull(v.id), v.comparisonId, v.winnerProviderId, v.dimension)
    }
}

// ── Leaderboard ──
data class RankingRes(
    val providerId: String,
    val displayName: String,
    val color: String,
    val vendor: String,
    val totalWins: Long,
    val winsByDimension: Map<String, Long>,
    val totalRuns: Long,
    val okRuns: Long,
    val okRate: Double,
    val avgLatencyMs: Long?,
) {
    companion object {
        fun from(r: VoteService.ProviderRanking) = RankingRes(
            providerId = r.providerId,
            displayName = r.displayName,
            color = r.color,
            vendor = r.vendor,
            totalWins = r.totalWins,
            winsByDimension = r.winsByDimension,
            totalRuns = r.totalRuns,
            okRuns = r.okRuns,
            okRate = r.okRate,
            avgLatencyMs = r.avgLatencyMs,
        )
    }
}

data class LeaderboardRes(val rankings: List<RankingRes>)
