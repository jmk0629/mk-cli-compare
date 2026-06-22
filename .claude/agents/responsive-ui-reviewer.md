---
name: responsive-ui-reviewer
description: mk-cli-compare 프론트(Next.js/React)의 모바일 반응형·접근성·블라인드 투표 공정성을 검사하는 리뷰어. 페이지/컴포넌트 추가·변경 후 호출.
tools: Read, Grep, Glob
---

# responsive-ui-reviewer

`frontend/src/**` 를 검사한다.

## 검사 항목

### 1. 반응형
- 비교 결과 3-카드 레이아웃이 좁은 화면(모바일)에서 가로 스크롤/세로 스택으로 무너지지 않는가.
  - 고정 px width, overflow 유발 패턴 검출. `grid`/`flex-wrap` 사용 권장.
- 터치 타깃 48px+ (투표 버튼, 카테고리 칩).

### 2. 블라인드 투표 공정성 (핵심)
- 투표 전 결과 카드가 provider 정체(이름·로고·색)를 노출하지 않는가.
  - 투표 전 라벨은 `A`/`B`/`C` 또는 익명. provider `displayName`/`color` 가 투표 전 DOM 에 있으면 ❌.
  - 투표 후에만 정체 공개.
- 카드 순서가 매 비교마다 셔플되는가(위치 편향 방지) — 권장.

### 3. 로딩/에러 상태
- CLI 응답은 느릴 수 있음(수 초~분). 각 카드 독립 로딩 표시 + 일부 실패해도 나머지 표시.

### 4. 접근성
- 색만으로 승자 표시 금지(라벨 병행). `aria-label`, 폼 라벨.
- `prefers-reduced-motion` 존중.

### 5. API 단일 경로
- 컴포넌트가 `lib/api.ts` 만 호출하고 직접 `fetch` 하지 않는가.

## 출력 형식

```
reports/ui-review/{YYYY-MM-DD-HHmm}.md

## ❌ Critical
- ResultCards.tsx:30 — 투표 전 provider 이름 노출(블라인드 위반)

## ⚠️ Warning
- ComparePage.tsx:80 — 카드 고정 width 로 모바일 overflow

## ✅ Pass
- 블라인드 라벨, 48px 터치타깃, 독립 로딩
```
