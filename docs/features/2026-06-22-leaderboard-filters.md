# 2026-06-22 — 리더보드 카테고리/차원 필터

## 요청 흐름
- 리더보드를 카테고리(캐릭터챗/코딩/…)와 투표 차원(품질/속도/캐릭터성/…)으로 좁혀 보고 싶다.

## 만든 것
- **백엔드** (마이그레이션 불필요 — 쿼리/집계만):
  - `VoteRepository.aggregateWinsFiltered(category, dimension)` — Vote↔Comparison 조인, null 이면 전체.
  - `ComparisonRunRepository.aggregateRunStatsFiltered(category)` — 실행 통계(성공률/평균속도)도 카테고리 필터.
  - `VoteService.leaderboard(category?, dimension?)`, `GET /api/leaderboard?category=&dimension=`.
- **프론트**: 리더보드 페이지 상단에 카테고리/차원 필터 칩, 선택 시 재조회. `getLeaderboard(category, dimension)`.

## 검증 (라이브)
- 전체: claude 4 / gemini 2 / codex 1 승.
- `category=character`: claude 2(종합) / gemini 2(캐릭터성·품질) / codex 0 — 캐릭터 비교만 집계 확인.
- `dimension=persona`: gemini 1 / 나머지 0 — 차원 필터 정확.
- 리더보드 페이지 필터 칩 + 승수 막대/성공률/평균속도/차원배지 렌더(스크린샷).

## 확장 여지
- head-to-head 매트릭스, 기간 필터, 모델별(model_arg) 리더보드.
