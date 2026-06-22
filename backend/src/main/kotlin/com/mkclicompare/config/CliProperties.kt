package com.mkclicompare.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * CLI 비교 엔진 설정 (application.yml `cli.*`).
 *
 * - `bins`: 카탈로그 command 의 첫 토큰(논리 바이너리명, 예: `claude`) → 실제 실행 경로.
 *   기본은 동일 명령(PATH 탐색). 설치 위치가 다르면 `.env` 의 `CLI_*_BIN` 으로 절대경로 지정.
 * - 카탈로그에 있는 provider 라도 매핑된 바이너리가 PATH/경로에 없으면 graceful 하게 error 행으로 처리.
 */
@ConfigurationProperties(prefix = "cli")
data class CliProperties(
    val timeoutSeconds: Long = 120,
    val parallel: Boolean = true,
    val bins: Map<String, String> = emptyMap(),
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash-lite",
) {
    /** 논리 바이너리명 → 실제 경로(미지정 시 이름 그대로 PATH 탐색). */
    fun resolveBin(name: String): String = bins[name]?.takeIf { it.isNotBlank() } ?: name

    val geminiApiEnabled: Boolean get() = geminiApiKey.isNotBlank()
}
