---
name: add-provider
description: mk-cli-compare 비교 대상 CLI/모델을 추가한다. cli_provider 카탈로그 INSERT(Flyway) + 프론트 색/아이콘 등록을 한 번에. "카탈로그는 데이터" 원칙 — 새 코드 분기 없이 행 추가만.
---

# add-provider

새 비교 대상(CLI 또는 API provider)을 카탈로그에 추가한다. **코드 변경 없이 데이터로** 추가하는 게 핵심.

## 입력

- `id` — 안정 키 (예: `claude`, `gemini`, `codex`, `claude-opus`)
- `displayName` — 표시명 (예: `Claude Code`)
- `vendor` — 벤더 (`anthropic` | `google` | `openai` | ...)
- `runnerKind` — `cli` | `api`
- `command` — cli 실행 명령 프리픽스 (예: `claude -p`, `agy -p`, `codex exec`). api 면 비움.
- `model` — (선택) 모델명. CLI 가 `--model` 받으면 command 에 반영하거나 별도 컬럼.
- `color` / `icon` — 프론트 배지 색/아이콘 키.

## 동작

1. `/db-migration` 으로 `V{N+1}__add_provider_{id}.sql` 생성:
   ```sql
   INSERT INTO cli_provider (id, display_name, vendor, runner_kind, command, model, color, icon, enabled, sort_order)
   VALUES ('<id>', '<displayName>', '<vendor>', '<cli|api>', '<command>', '<model>', '<color>', '<icon>', 1, <next>);
   ```
2. command 의 첫 토큰(바이너리)이 `CliRunnerService` whitelist 와 `.env` 의 `CLI_*_BIN` 에 매핑되는지 확인.
   새 바이너리면 `.env.sample` 에 `CLI_<NAME>_BIN` 추가 안내.
3. 프론트 `frontend/src/lib/providers.ts`(색/아이콘 맵)에 `id` 키 추가 — 없으면 기본 회색 폴백.
4. 검증: 백엔드 재기동 후 `GET /api/providers` 에 새 행 노출 확인.

## 금지

- provider 별 if/when 코드 분기 추가 (카탈로그 컬럼으로 표현할 것).
- 셸 보간형 command (예: `bash -c ...`). command 는 argv 토큰으로만 분해 실행됨.

## 출력

생성한 마이그레이션 경로 + 카탈로그 행 + 프론트 등록 결과.
