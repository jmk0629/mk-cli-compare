# 2026-06-22 — 모바일 PWA + 하단 탭 네비 + 다크모드 토글

## 목표
- 모바일에서 더 많이 쓰도록 UX 강화: 설치형 PWA, 한 손 조작 하단 네비, 다크모드.

## 만든 것
- **PWA**
  - `app/manifest.ts`(Next 메타데이터 라우트) → `manifest.webmanifest`. standalone, theme_color, 아이콘(`/icon.svg`).
  - `public/sw.js` 서비스워커: 앱 셸 캐시 + 네비 네트워크우선/오프라인 폴백. **API(/api·:8080)는 캐시 안 함**(항상 최신).
  - `PwaRegister`(프로덕션에서만 SW 등록). `app/icon.svg` 브랜드 아이콘.
  - layout 메타에 `manifest`, `appleWebApp` 추가.
- **하단 탭 네비** `BottomNav`: 모바일(`sm:hidden`) 고정 하단, 비교/리더보드/기록/계정 4탭,
  `usePathname` 활성 표시, 56px 터치타깃, safe-area 패딩.
- **다크모드 토글** `ThemeToggle`: 헤더 🌙/☀️ 버튼, `data-theme` 전환 + `localStorage(mkc:theme:v1)` 저장
  (layout bootScript 가 페인트 전 적용 → 깜빡임 없음).
- 레이아웃: 데스크톱 상단 네비는 `sm:` 이상에서만, 모바일은 하단 탭. main 하단 패딩(pb-24)으로 탭 가림 방지.

## 검증 (라이브)
- 모바일 뷰포트(390×844): 모델 선택 세로 스택, 하단 4탭(비교 활성) 표시, 다크모드 토글 동작(스크린샷).
- `manifest.webmanifest` 라우트 생성, `next build` 통과.

## 참고
- 아이콘은 SVG(any/maskable). 일부 구형 브라우저는 PNG 192/512 선호 — 필요 시 추후 PNG 추가.
