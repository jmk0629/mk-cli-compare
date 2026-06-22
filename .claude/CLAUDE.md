# mk-cli-compare — Claude Code 하네스 사용법

루트 `CLAUDE.md` 는 코드 컨벤션. 이 파일은 `.claude/` 하네스(skills/agents/memory) 사용법.

## ⚠️ 작업 시작 전 필독

매 작업 전 **`.claude/memory/MISTAKES.md`** 를 먼저 읽는다. 과거 실수를 반복하지 않기 위함.
새 실수를 발견하면 `/log-mistake` 로 즉시 기록한다.

## 핵심 원칙 — 카탈로그는 데이터, 실행은 코드

비교 대상 CLI 추가 = **Flyway `cli_provider` INSERT 1줄**. 새 코드 분기 없음.
provider 의 실행 방식(`runner_kind`=cli|api), 명령(`command`=`claude -p`), 모델, 색/아이콘은
모두 카탈로그 컬럼. 새 provider 는 항상 `/add-provider` 로 추가한다.

## Skills (6)

`/<skill-name>` 으로 호출. 각 SKILL.md 가 실제 지시문.

| Skill | 언제 사용 | 무엇을 만들/검사 |
|---|---|---|
| `/add-provider` | 비교 대상 CLI/모델 추가 시 | `cli_provider` INSERT(Flyway) + (필요 시) 프론트 색/아이콘 등록 |
| `/commit-push` | 작업/Phase 종료 시 | 변경을 의미 단위로 stage → conventional commit → `git push origin main`. 시크릿·force 차단 |
| `/db-migration` | 새 테이블/컬럼 필요 시 | 다음 `V{N+1}__{slug}.sql` 생성, 기존 V 파일 수정 차단, SQLite lint |
| `/add-api-endpoint` | 신규 REST 엔드포인트 | 백(Controller+Service+Repo+DTO) + 프론트(zod + api.ts) 동시 |
| `/api-contract-check` | PR 직전, Phase 종료 | 백엔드 컨트롤러 ↔ 프론트 zod(api-types.ts) 정합 검사 |
| `/log-mistake` | 실수했을 때 | `memory/MISTAKES.md` 에 증상/원인/수정/재발방지 기록 |

## Agents (3)

| Agent | 호출 시점 |
|---|---|
| `kotlin-mvc-reviewer` | Phase 종료/PR 직전. `com.mkclicompare` 계층 위반·Entity 노출·Service HTTP 누수 검출 |
| `cli-runner-reviewer` | CLI 실행/비교 로직 변경 후. 셸 인젝션(프롬프트 보간), 타임아웃 누락, whitelist 우회, exit code 미기록 검증 |
| `responsive-ui-reviewer` | 프론트 페이지 추가·변경 후. 모바일 반응형·블라인드 투표 라벨 가림·가독성 검사 |

## Memory (실수 ledger)

- `memory/MISTAKES.md` — 하네스 산출물로 repo 에 커밋(공유·영속). 작업 전 반드시 읽는다.
- 항목 포맷: `날짜 / 증상 / 원인 / 수정 / 재발방지 규칙`. `/log-mistake` 로 추가.

## 권한 (`.claude/settings.local.json`)

- Allow: gradlew/pnpm/npm/node, sqlite3, git(status/diff/log/add/commit/push non-force), gh,
  curl(localhost), **claude/agy/codex(비교 엔진 CLI)**
- Deny: `git push --force`, `git reset --hard`, `git clean`, `rm -rf`, `curl | sh|bash`, `sudo`
- 이 파일은 `.gitignore` 대상(로컬 전용). 새 클론 시 직접 생성.
