package com.mkclicompare.domain.comparison

import com.mkclicompare.config.CliProperties
import com.mkclicompare.domain.ai.GeminiApiClient
import com.mkclicompare.domain.provider.CliProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * provider 의 CLI 를 서브프로세스로 실행해 같은 프롬프트의 응답을 얻는다.
 *
 * 보안/견고성 (cli-runner-reviewer 기준):
 * - 프롬프트는 **argv 원소 하나**로만 전달. 셸(`bash -c`) 미경유 → 인젝션 차단.
 * - 실행 바이너리는 카탈로그(command)/`cli.bins` 매핑에서만. 사용자 입력이 바이너리로 흐르지 않음.
 * - 타임아웃 초과 시 `destroyForcibly`. stdout/stderr 를 별도 스레드로 드레인(버퍼 deadlock 방지).
 * - 실패(미설치/비인증/timeout)는 예외가 아니라 RunResult(error/timeout) 로 수렴 → 호출부가 행으로 기록.
 */
@Service
class CliRunnerService(
    private val cliProperties: CliProperties,
    private val geminiApiClient: GeminiApiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class RunResult(
        val status: String,        // ok | error | timeout
        val responseText: String?,
        val errorText: String?,
        val exitCode: Int?,
        val latencyMs: Long,
    )

    /** model 이 주어지면 provider.modelFlag 와 함께 argv 에 주입(whitelist 검증은 호출부 책임). */
    fun run(provider: CliProvider, prompt: String, model: String? = null): RunResult {
        val started = System.nanoTime()
        return try {
            when (provider.runnerKind) {
                "api" -> runApi(provider, prompt, started)
                else -> runCli(provider, prompt, model, started)
            }
        } catch (e: Exception) {
            log.warn("provider {} 실행 실패: {}", provider.id, e.message)
            RunResult("error", null, e.message ?: e.javaClass.simpleName, null, elapsedMs(started))
        }
    }

    /** 로컬 CLI 바이너리 서브프로세스 실행. */
    private fun runCli(provider: CliProvider, prompt: String, model: String?, started: Long): RunResult {
        val tokens = provider.commandTokens()
        if (tokens.isEmpty()) {
            return RunResult("error", null, "command 가 비어 있습니다.", null, elapsedMs(started))
        }
        // 첫 토큰(논리 바이너리) → 실제 실행 경로로 치환. 나머지 인자 유지. 프롬프트는 마지막 argv.
        val realBin = cliProperties.resolveBin(provider.resolveBinKey())
        // 일부 CLI(codex)는 최종 응답이 stdout 메타데이터에 섞임 → 파일로 받는다.
        val outputFile: File? = provider.outputFileFlag
            ?.takeIf { it.isNotBlank() }
            ?.let { File.createTempFile("clicmp-", ".out") }
        // argv 순서 (중요): [bin] + 서브커맨드/정적플래그 + [모델] + [출력파일] + [프롬프트플래그] + [프롬프트]
        // agy 의 `-p` 는 다음 토큰을 프롬프트 값으로 소비하므로 프롬프트플래그는 반드시 프롬프트 직전에 둔다.
        val argv = buildList {
            add(realBin)
            addAll(tokens.drop(1))
            // 모델 선택: provider 가 modelFlag 를 가지고 model 이 주어지면 주입(예: --model haiku / -m gpt-5.4).
            val flag = provider.modelFlag
            if (!model.isNullOrBlank() && !flag.isNullOrBlank()) {
                add(flag.trim())
                add(model)
            }
            if (outputFile != null) {
                add(provider.outputFileFlag!!.trim())
                add(outputFile.absolutePath)
            }
            val pf = provider.promptFlag
            if (!pf.isNullOrBlank()) add(pf.trim())
            add(prompt)
        }

        val process = try {
            ProcessBuilder(argv)
                .directory(File(System.getProperty("java.io.tmpdir")))
                .redirectErrorStream(false)
                .start()
        } catch (e: Exception) {
            // 바이너리 미존재 등 → graceful
            return RunResult("error", null, "실행 불가(${realBin}): ${e.message}", null, elapsedMs(started))
        }

        // stdin 즉시 닫음(대화형 진입 방지). stdout/stderr 는 병렬 드레인.
        process.outputStream.close()
        val outF = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().use { it.readText() } }
        val errF = CompletableFuture.supplyAsync { process.errorStream.bufferedReader().use { it.readText() } }

        try {
            val finished = process.waitFor(cliProperties.timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                outF.cancel(true); errF.cancel(true)
                return RunResult("timeout", null, "${cliProperties.timeoutSeconds}s 타임아웃", null, elapsedMs(started))
            }
            val exit = process.exitValue()
            val stdout = runCatching { outF.get(5, TimeUnit.SECONDS) }.getOrDefault("").trim()
            val err = runCatching { errF.get(5, TimeUnit.SECONDS) }.getOrDefault("").trim()
            // 출력 파일 모드면 파일 내용을, 아니면 stdout 을 응답으로.
            val response = outputFile?.let { runCatching { it.readText().trim() }.getOrDefault("") } ?: stdout
            val latency = elapsedMs(started)

            return if (exit == 0 && response.isNotBlank()) {
                RunResult("ok", response, err.ifBlank { null }, exit, latency)
            } else {
                val msg = err.ifBlank { stdout.ifBlank { "exit=$exit, 빈 응답" } }
                RunResult("error", response.ifBlank { null }, msg, exit, latency)
            }
        } finally {
            outputFile?.delete()
        }
    }

    /** API fallback (현재 Gemini 만). CLI 미설치 환경 대체용. */
    private fun runApi(provider: CliProvider, prompt: String, started: Long): RunResult {
        if (provider.vendor == "google" && cliProperties.geminiApiEnabled) {
            val text = geminiApiClient.generate(prompt)
            return if (text != null) RunResult("ok", text, null, 0, elapsedMs(started))
            else RunResult("error", null, "Gemini API 호출 실패", null, elapsedMs(started))
        }
        return RunResult("error", null, "api runner 미지원 provider: ${provider.id}", null, elapsedMs(started))
    }

    private fun elapsedMs(startedNanos: Long): Long =
        (System.nanoTime() - startedNanos) / 1_000_000
}
