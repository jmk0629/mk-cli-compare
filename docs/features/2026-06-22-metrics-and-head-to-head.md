# 2026-06-22 — 정량 메트릭 비교 + Head-to-head 승률 매트릭스

추가 CLI 호출 없이(기존 데이터 재가공) 비교 깊이를 더하는 두 기능.

## 1) 정량 메트릭 비교 패널 (CLI 호출 0, 클라이언트 계산)
- `MetricsPanel`: 응답 텍스트에서 글자수·단어수·줄수·코드블록수·읽기시간(200wpm)·응답속도를 계산해
  provider별 나란히 표로 비교. 최속만 ⚡강조, 분량류는 "우열 아님" 고지(스타일 차이).
- 비교 상세 페이지(요약표 ↔ AI 심판 사이)에 배치.

## 2) Head-to-head 승률 매트릭스 (CLI 호출 0, 투표 재집계)
- 한 투표 = winner 가 같은 비교의 다른 ok 참가자를 각각 이긴 것 → 1:1 쌍 집계.
- 백엔드 `VoteRepository.aggregateHeadToHead()` + `GET /api/leaderboard/h2h`(providers + pairs[{winner,loser,wins}]).
- 프론트 `H2hMatrix`(리더보드 하단): 행=A, 열=B, 칸=A가 B 상대 승률(%)+승–패. 초록=우세/빨강=열세.

## 검증 (라이브)
- `/api/leaderboard/h2h`: claude–codex 4–2, claude–gemini 4–2, codex–gemini 2–2 등 집계 확인.
- 리더보드 매트릭스(claude 67%/67%, 색상·승–패), 상세 정량 패널 렌더(스크린샷).

## 확장 여지
- 매트릭스에 카테고리/차원 필터 연동, 시간 추이.
