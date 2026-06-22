# 2026-06-22 — 프롬프트 라이브러리 확장 + AI 프롬프트 생성기

## 요청
- 카테고리(캐릭터챗/코딩/요약/추론/일반)별 프리셋이 1~2개뿐이라 빈약.
- 프롬프트를 별도로 생성해서 가져올 수 있게.

## 만든 것
1. **프리셋 확장 (V5, 데이터)**: 16개 추가 → 총 23개.
   - character 5 · coding 5 · summary 4 · reasoning 4 · general 5.
2. **AI 프롬프트 생성기 (옵트인, CLI 1회)**:
   - 백엔드 `PromptGenService` + `POST /api/prompts/generate {category, provider?}`.
     - 카테고리별 메타 프롬프트로 CLI(기본 claude) 호출 → 출력에서 따옴표/코드펜스/머리말 제거 후 본문만 반환.
     - provider 미지정 시 claude 선호, 없으면 첫 활성 provider.
   - 프론트: 프리셋 줄에 **"✨ AI로 프롬프트 생성"** 버튼 → 현재 카테고리로 생성 → 입력창에 자동 입력. 로딩 표시.

## 검증 (라이브)
- `/api/presets`: 23개(카테고리별 5/5/4/4/5) 확인.
- `POST /api/prompts/generate {category:character}`: claude 가 고품질 캐릭터 프롬프트("평양냉면집 3대 주방장 봉수…") 생성, 깔끔히 정제됨.
- 홈에서 생성 버튼 + 확장 프리셋 렌더(스크린샷).

## 확장 여지
- 생성기 provider 선택 UI(현재 기본 claude), "이 프롬프트로 바로 비교" 원클릭.
