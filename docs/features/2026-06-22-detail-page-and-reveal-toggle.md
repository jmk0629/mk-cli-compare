# 2026-06-22 — 비교 상세 페이지 + 투표 없이 보기 토글

## 요청
- 기록(history) 카드를 누르면 상세 페이지로 가서 전체 데이터를 확인.
- 메인에서 결과가 나오면 투표를 강제당하지 않고, **별도 토글**로 투표 없이 전체를 볼 수 있게.

## 만든 것
- **상세 페이지** `frontend/src/app/comparison/[id]/page.tsx`
  - `GET /api/comparisons/{id}` 로 단건 조회.
  - 헤더(카테고리·상태·프롬프트·실행/완료 시각), 요약 지표 테이블(provider·상태·응답시간·길이·exit, 최속 ⚡표시),
    provider별 **전체 응답** 카드(스크롤).
- **기록 카드 → 링크**: `frontend/src/app/history/page.tsx` 의 카드를 `/comparison/{id}` 로 이동하는 `<Link>` 로 변경.
- **투표 없이 전체 보기 토글**: `frontend/src/components/CompareView.tsx`
  - 기존 일회성 "정체 공개" 버튼 → 켜고 끌 수 있는 스위치로 교체.
  - `revealed = showAll || hasVoted` — 토글을 켜거나 투표하면 정체+전체 내용 공개.
  - "투표는 선택이에요" 안내 + 결과 상단에 "전체 데이터 보기 →" 상세 링크.

## 검증
- 라이브: 기록 카드 클릭 → 상세 진입, FizzBuzz 비교의 3 CLI 전체 코드/지표 표시 확인(스크린샷).
- 메인 토글 OFF→블라인드(응답 A/B/C), ON→provider 정체 공개 확인.

## 커밋
- `aee4d8e` feat: 비교 상세 페이지 + 투표 없이 보기 토글
