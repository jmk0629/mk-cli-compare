# 2026-06-22 — mk-cli-compare 초기 구축 (P0–P6)

구독형 코딩 CLI 3종(claude · gemini=agy · codex)에 같은 프롬프트를 던져 비교하고
블라인드 투표 → 승률 리더보드로 우열을 가리는 사이트. mk-remember-game 하네스 이식.

## 결정 사항
- **gemini = Antigravity CLI(`agy`)** 사용(일반 `gemini` CLI 아님 — 사용자 지정).
- **codex**: git 저장소 밖 실행에 `--skip-git-repo-check`, stdout 메타데이터 회피 위해
  `--output-last-message <FILE>` 로 최종 응답만 파일로 수신(카탈로그 `output_file_flag` 컬럼).
- 구독형 CLI 를 서브프로세스로 호출 → **API 키 불필요**(로컬 CLI 로그인 필요).
- 포트 8080/3000 을 다른 mk-* 앱과 공유 → **동시 실행 금지**.

## 백엔드 (Spring Boot + Kotlin, `com.mkclicompare`)
- Flyway V1: `cli_provider`(카탈로그) · `prompt_preset` · `comparison` · `comparison_run` · `vote` · `users`.
- Flyway V2: codex 보정(`output_file_flag` 컬럼 + command 갱신).
- `CliRunnerService`: ProcessBuilder argv 실행(셸 인젝션 차단), 타임아웃+destroyForcibly,
  stdout/stderr 병렬 드레인, output-file 모드, 실패 graceful.
- `ComparisonService`: 트랜잭션 밖 병렬 실행 → run 저장 → 상태 갱신.
- `VoteService`: 블라인드 투표 + 승률/차원별 승수/평균 레이턴시/성공률 리더보드.
- JWT + OAuth2(Google/Kakao/Naver) 이식, 게스트 우선(`/api/me/**` 만 보호).
- 통합 SmokeTest 6건 통과.

## 프론트 (Next.js 16 + React 19 + Tailwind v4 + zod)
- 비교 페이지(카테고리/프리셋/프롬프트 → 블라인드 A/B/C 카드 → 차원별 투표 → 정체 공개).
- 리더보드(승수 막대·속도·성공률), 기록(게스트=최근/로그인=내 기록), 계정(OAuth/콜백).
- `lib/api.ts` 단일 호출 경로 + zod 검증. 인디고 브랜드.

## 실측 검증 (라이브)
- 백엔드 기동 → `POST /api/comparisons` "한 단어로만: 안녕":
  claude "안녕하세요"(4.6s) · agy "안녕하세요"(8.3s) · codex "안녕"(5.7s) 모두 `ok`.
- 투표·리더보드 집계 동작, `/api/me` 401(인증 게이트), CORS(3000→8080) 허용 확인.
- 프론트 빌드(7 라우트) + 브라우저 렌더 + 클라이언트 fetch(provider 3종 노출) 확인.

## API 엔드포인트
| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/providers` | 공개 | 활성 provider 카탈로그 |
| GET | `/api/presets` | 공개 | 카테고리 프리셋 |
| POST | `/api/comparisons` | 게스트 | 비교 실행(3 CLI) |
| GET | `/api/comparisons/{id}` | 공개 | 단일 비교 조회 |
| GET | `/api/comparisons` | 공개 | 최근 비교 피드 |
| POST | `/api/votes` | 게스트 | 블라인드 투표 |
| GET | `/api/leaderboard` | 공개 | provider 리더보드 |
| GET | `/api/me` · `/api/me/comparisons` | JWT | 내 정보 · 내 비교 기록 |
| GET | `/api/auth/providers` | 공개 | 활성 소셜 로그인 목록 |

## 남은 아이디어
- 비교 스트리밍(SSE)로 각 CLI 응답 도착 즉시 표시.
- ELO 레이팅, head-to-head 매트릭스, 카테고리별 리더보드 필터.
- 모델 선택(claude/codex `--model`)을 카탈로그 행으로 추가(`/add-provider`).
