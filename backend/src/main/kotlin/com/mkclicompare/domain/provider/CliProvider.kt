package com.mkclicompare.domain.provider

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 비교 대상 CLI/모델 카탈로그 행. "카탈로그는 데이터" — provider 추가 = INSERT.
 * 실행 방식(runnerKind)·명령(command)·바이너리 키(binKey)는 컬럼이지 코드 분기가 아니다.
 */
@Entity
@Table(name = "cli_provider")
class CliProvider(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "display_name", nullable = false)
    val displayName: String = "",

    @Column(name = "vendor", nullable = false)
    val vendor: String = "",

    @Column(name = "runner_kind", nullable = false)
    val runnerKind: String = "cli",   // cli | api

    @Column(name = "command", nullable = false)
    val command: String = "",         // 예: "claude -p"

    @Column(name = "bin_key")
    val binKey: String? = null,       // cli.bins 매핑 키. null 이면 command 첫 토큰.

    @Column(name = "model")
    val model: String? = null,

    @Column(name = "color", nullable = false)
    val color: String = "#6366f1",

    @Column(name = "icon")
    val icon: String? = null,

    @Column(name = "enabled", nullable = false)
    val enabled: Int = 1,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,
) {
    val isEnabled: Boolean get() = enabled == 1

    /** command 를 argv 토큰으로 분해 (예: "claude -p" → ["claude","-p"]). 첫 토큰은 논리 바이너리. */
    fun commandTokens(): List<String> = command.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

    /** cli.bins 매핑에 사용할 논리 바이너리명. */
    fun resolveBinKey(): String = binKey?.takeIf { it.isNotBlank() } ?: commandTokens().firstOrNull().orEmpty()
}
