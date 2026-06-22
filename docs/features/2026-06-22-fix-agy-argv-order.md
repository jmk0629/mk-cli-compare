# 2026-06-22 — [버그수정] agy(Gemini) 응답 오류 — argv 순서

## 증상
- Antigravity 앱에서 직접 롤플레이 프롬프트를 주면 정상 응답.
- 그러나 mk-cli-compare 를 통하면 gemini 카드가 엉뚱하게
  `I am running on Gemini 3.5 Flash. ### Summary of Work ...` 같은 에이전트형 출력만 냄.

## 원인 (확정)
- agy 의 `-p`(--prompt)는 **바로 다음 토큰을 프롬프트 값으로 소비**한다.
- 기존 CliRunner argv: `agy -p --model "Gemini 3.1 Pro (High)" "<실제 프롬프트>"`
  → `-p` 가 `--model` 을 프롬프트로 먹어 "현재 모델이 뭐냐"는 식으로 동작, 실제 프롬프트는 무시됨.
- 재현: `agy -p --model "Gemini 3.1 Pro (High)" "<프롬프트>"` → "Summary of Work" 출력(스크린샷과 동일).

## 수정
- argv 순서를 **모델/출력 플래그를 먼저, 프롬프트 플래그를 맨 끝(프롬프트 직전)** 으로 변경.
- 이를 위해 `prompt_flag` 를 `command` 에서 분리(V4):
  - claude: command=`claude`, prompt_flag=`-p`
  - gemini: command=`agy`, prompt_flag=`-p`  ← 프롬프트가 `-p` 직후
  - codex: command=`codex exec --skip-git-repo-check`, prompt_flag=없음(positional)
- 최종 argv = `[bin] + 서브커맨드/정적플래그 + [model_flag model] + [output_flag file] + [prompt_flag] + [prompt]`
  - gemini 예: `agy --model "Gemini 3.1 Pro (High)" -p "<프롬프트>"` ✓

## 검증 (라이브)
- 같은 츤데레 프롬프트 + gemini=`Gemini 3.1 Pro (High)`:
  - claude "흥, 야근한다고? …밥은 먹고 하는 거야?" / gemini "또 야근이야? 진짜 바보 아니야? …" / codex "또 야근이야? 몸 망가지면…"
  - 세 CLI 모두 정상 인캐릭터 응답. "Summary of Work" 사라짐.
- Flyway V1~V4 적용 확인, 카탈로그 command/prompt_flag 검증.
