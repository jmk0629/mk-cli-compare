# mk-cli-compare — 코드 컨벤션

구독형 코딩 CLI 3종(**claude** / **gemini=agy(Antigravity)** / **codex**)에 같은 프롬프트를 던져
응답·속도·품질을 나란히 비교하고, 블라인드 투표 → 승률 리더보드로 우열을 데이터로 답하는 사이트.
백엔드 Spring Boot + Kotlin, 프론트 Next.js + React, DB SQLite.
`.claude/` 하네스 사용법은 `.claude/CLAUDE.md` 참조.

## 절대 규칙

1. **카탈로그는 데이터, 실행은 코드.** 비교 대상 CLI 추가 = `cli_provider` INSERT(Flyway) 1줄.
   provider 의 `runner_kind`(cli|api) + `command`(예: `claude -p`)는 카탈로그 컬럼. 코드 분기 최소화.
2. **MVC 단방향**: Controller → Service → Repository. 컨트롤러는 DTO(`*Res`)만 반환, Entity 직접 노출 금지.
3. **DB 불변성**: 기존 `V{N}__*.sql` 수정 금지. 항상 새 `V{N+1}` 추가(`/db-migration`).
4. **프론트 API 단일 경로**: 모든 호출은 `frontend/src/lib/api.ts` 경유. 컴포넌트 직접 fetch 금지.
   응답은 `api-types.ts` zod 로 검증.
5. **CLI 실행 안전**: 프롬프트는 **인자(argv)로만** 전달. 셸 문자열 보간 금지(injection 차단).
   실행 명령은 카탈로그 whitelist 의 바이너리만. 타임아웃·exit code 항상 기록.
6. **게스트 우선**: 비교 실행·결과 열람·투표는 로그인 없이 동작. 로그인은 부가(내 히스토리/동기화).
7. **블라인드 공정성**: 투표 UI 는 provider 이름을 가린 A/B/C 로 노출. 투표 후에만 정체 공개.
8. **히스토리**: 기능 추가마다 `docs/features/YYYY-MM-DD-*.md` 기록.

## Provider 매핑 (현재 카탈로그)
| id | 표시명 | runner | command | 비고 |
|---|---|---|---|---|
| `claude` | Claude Code | cli | `claude -p` | Anthropic 구독 |
| `gemini` | Antigravity (Gemini) | cli | `agy -p` | `agy` = Antigravity CLI |
| `codex`  | Codex (GPT) | cli | `codex exec` | OpenAI 구독 |

## 백엔드 (Kotlin)
- 패키지 루트 `com.mkclicompare`. 데이터 클래스 선호, 명시적 가시성.
- SQLite + Flyway(`ddl-auto=none`). JSON 가변 데이터는 opaque `TEXT` 컬럼, 집계 필드만 실제 컬럼 승격.
- CLI 실행은 `ProcessBuilder` + 타임아웃. 절대 `bash -c "<프롬프트>"` 금지.

## 프론트 (Next.js/React)
- App Router, `"use client"` 명시. Tailwind v4(인디고/바이올렛 brand — 기존 앱과 구분).
- 비교 결과는 provider 별 카드. 투표 전에는 라벨 가림(A/B/C). `api.ts` 직접 import 만.

## 실행
```bash
cp .env.sample .env   # 비밀값 채우기 (또는 mk-remember-game .env 재사용)
make doctor           # CLI 설치 점검
make backend          # http://localhost:8080/actuator/health
make install && make frontend   # http://localhost:3000
```
⚠️ 8080/3000 을 다른 mk-* 앱과 공유 → **동시 실행 금지**.
