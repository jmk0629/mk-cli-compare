# 2026-06-22 — provider별 모델 선택 (select 박스)

## 요청
- 각 CLI(codex / claude / agy)는 여러 모델을 고를 수 있다(앱 스크린샷 참고).
- 비교 시 provider별로 모델을 고르는 **select 박스**를 제공.

## 모델 플래그 (실측 확인)
| provider | 플래그 | 예시 인자 |
|---|---|---|
| claude | `--model` | `opus` · `sonnet` · `haiku` |
| gemini=agy | `--model` | `Gemini 3.5 Flash (Medium)` 등 (`agy models` 출력 문자열) |
| codex | `-m` | `gpt-5.5` · `gpt-5.4` · `gpt-5.4-mini` |

## 만든 것
- **Flyway V3** `V3__provider_models.sql`
  - `cli_provider.model_flag` 컬럼(provider별 모델 플래그).
  - `comparison_run.model` 컬럼(실행에 쓰인 모델 기록).
  - `provider_model` 테이블 + 시드(claude 3 · gemini 8 · codex 3). model_arg 가 CLI 에 넘기는 whitelist 인자.
- **백엔드**
  - `ProviderModel` 엔티티/리포지토리, `ProviderService.modelsByProvider()`.
  - `CliRunnerService.run(provider, prompt, model)` — model 이 있으면 `<modelFlag> <model>` 을 argv 에 주입.
  - `ComparisonService` — 요청 모델을 provider_model **whitelist 로 검증**(임의 문자열 차단) 후 실행·기록.
  - DTO: `ProviderRes.models[]`(arg/label/isDefault), `RunRes.model`, `CreateComparisonReq.models`.
- **프론트**
  - `CompareView` provider별 `<select>` (기본값 = is_default 모델), 선택값을 `createComparison` 으로 전송.
  - 결과 카드/상세 페이지에 사용 모델 표시.

## 검증
- `GET /api/providers` 가 provider별 모델 목록 노출(claude 3·gemini 8·codex 3) 확인.
- 라이브 실행 `models={claude:haiku, codex:gpt-5.4-mini, gemini:Flash}`:
  claude(haiku) "퐁" · codex(gpt-5.4-mini) "퐁" · gemini(Flash) 응답 — 모두 `ok`, run.model 기록 확인.
- 메인 select 박스 3종 렌더 확인(스크린샷).
