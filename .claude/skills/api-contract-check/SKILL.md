---
name: api-contract-check
description: mk-cli-compare 백엔드 컨트롤러와 프론트 zod(api-types.ts) 계약 정합을 검사한다. 엔드포인트 경로/메서드, 요청·응답 필드, provider 카탈로그↔프론트 색맵 드리프트를 대조.
---

# api-contract-check

PR 직전·Phase 종료 시 백/프론트 계약 정합을 검사한다. (읽기 전용 — 코드 수정 없음)

## 검사 항목

1. **엔드포인트 커버리지**: `web/*Controller.kt` 의 모든 `@(Get|Post|Put|Delete)Mapping` 경로가
   `frontend/src/lib/api.ts` 에 대응 함수로 존재하는가. (orphan endpoint / orphan client 탐지)
2. **응답 shape 정합**: 컨트롤러 반환 `*Res` data class 필드 ↔ `api-types.ts` zod 스키마 필드 일치
   (이름·옵셔널·타입). 누락/초과 필드 보고.
3. **직접 fetch 우회**: `frontend/src/**` 에서 `api.ts` 를 거치지 않는 `fetch(`/`axios` 직접 호출 grep.
4. **하드코딩 URL**: 컴포넌트에 `http://localhost:8080` 등 직접 URL 박힌 곳.
5. **provider 드리프트**: 백엔드 `cli_provider` 카탈로그 id 집합 ↔ 프론트 `lib/providers.ts` 색/아이콘 맵 키 일치.
   카탈로그에 있는데 프론트 맵에 없으면 기본 폴백 경고.
6. **auth 경계**: `guest` 로 노출돼야 할 비교/투표 경로가 `SecurityConfig` permitAll 에 있는가.

## 출력 형식

```
reports/contract-check/{YYYY-MM-DD-HHmm}.md

## ❌ 불일치
- POST /api/comparisons — api.ts 에 대응 함수 없음
- ComparisonRes.latencyMs — zod 스키마에 누락

## ⚠️ 경고
- providers.ts 에 'codex' 색 매핑 없음 → 회색 폴백

## ✅ Pass
- 7/8 엔드포인트 계약 일치
```
