# mk-cli-compare

> 같은 프롬프트, 세 개의 CLI. 어느 쪽이 더 나은가를 **데이터로** 답한다.

구독형 코딩 CLI 3종에 **동일한 프롬프트**를 동시에 던져 응답·속도·길이를 나란히 비교하고,
**블라인드 A/B/C 투표 → 승률 리더보드**로 우열을 가린다. (캐릭터챗·코딩·요약·추론 카테고리별)

| Provider | 표시명 | runner | 실행 명령 |
|---|---|---|---|
| `claude` | Claude Code | CLI | `claude -p "<prompt>"` |
| `gemini` | Antigravity (Gemini) | CLI | `agy -p "<prompt>"` |
| `codex`  | Codex (GPT) | CLI | `codex exec "<prompt>"` |

구독형 CLI 를 그대로 호출하므로 **API 키 없이** 동작한다(로컬에 CLI 가 설치·로그인돼 있어야 함).

## 스택

- **백엔드** — Spring Boot 3.5 · Kotlin · Java 21 · SQLite + Flyway · Spring Security(OAuth2 Google/Kakao/Naver) + JWT
- **프론트** — Next.js 16 · React 19 · Tailwind v4 · zod
- **비교 엔진** — `ProcessBuilder` 로 CLI 를 비대화형(headless) 병렬 실행, 레이턴시·exit code 측정

## 빠른 시작

```bash
cp .env.sample .env       # 비밀값 채우기 (OAuth/JWT 는 다른 mk-* 앱 값 재사용 가능)
make doctor               # claude / agy / codex 설치 점검
make backend              # http://localhost:8080/actuator/health
make install && make frontend   # http://localhost:3000
```

> ⚠️ 백엔드 8080 · 프론트 3000 을 `mk-hospital` / `mk-health-app` / `mk-remember-game` 과 공유한다.
> **동시에 두 앱을 실행하지 말 것.**

## 동작 흐름

1. 프롬프트 입력 + 카테고리 선택 → `POST /api/comparisons`
2. 백엔드가 활성 provider 별로 CLI 를 병렬 실행 → 응답·latency·exit code 를 `comparison_run` 에 저장
3. 프론트가 결과를 **블라인드 카드(A/B/C)** 로 표시
4. 사용자가 차원별(전체/품질/속도/캐릭터성) 투표 → 정체 공개
5. 누적 투표로 provider 별 **승률·ELO 리더보드** 집계

## 구조

```
backend/   Spring Boot + Kotlin (com.mkclicompare)
  domain/provider     CLI 카탈로그 (cli_provider)
  domain/comparison   비교 + 실행 (CliRunnerService)
  domain/vote         블라인드 투표 + 리더보드(ELO/승률)
  auth/               JWT + OAuth2 (Google/Kakao/Naver)
frontend/  Next.js + React (App Router)
  src/lib/api.ts      유일한 백엔드 호출 경로 (zod 검증)
.claude/   Claude Code 하네스 (skills · agents · memory)
```

## .claude 하네스

- **Skills**: `/add-provider` `/add-api-endpoint` `/db-migration` `/api-contract-check` `/commit-push` `/log-mistake`
- **Agents**: `kotlin-mvc-reviewer` · `cli-runner-reviewer` · `responsive-ui-reviewer`
- 자세한 사용법은 [`.claude/CLAUDE.md`](.claude/CLAUDE.md).
