# 2026-06-22 — 통계 개요 + 최근 비교 피드

## 목표
- 사이트 활동을 한눈에(신뢰/재미), 최근 비교 피드로 탐색·재방문 유도.

## 만든 것
- **백엔드** `GET /api/stats` (`StatsController`): 총 비교 수·총 투표 수, 최다승 provider, 최속(평균 레이턴시) provider.
  - 기존 `voteService.leaderboard()` + `count()` 재사용(추가 마이그레이션 없음).
- **프론트**
  - 리더보드 상단 **통계 카드 4종**(총 비교/총 투표/🏆최다승/⚡최속).
  - 홈 하단 **최근 비교 피드**(`RecentComparisons`): 카테고리·프롬프트 미리보기·provider별 속도·시각, 클릭 시 상세.

## 검증 (라이브)
- `/api/stats`: totalComparisons=13, totalVotes=7, top=claude(4승), fastest=codex(6.7s).
- 리더보드 통계 카드 + 홈 최근 피드 렌더(스크린샷).

## 확장 여지
- 기간별 추세(일/주), provider별 승률 추이 차트.
