---
name: add-api-endpoint
description: mk-cli-compare 에 신규 REST 엔드포인트를 백/프론트 동시에 추가한다. 백(Controller+Service+Repository+DTO) + 프론트(zod 타입 + api.ts 함수)를 계약 일치로 생성.
---

# add-api-endpoint

신규 엔드포인트를 **백엔드와 프론트엔드 계약을 맞춰** 한 번에 추가한다.

## 입력

- `method` + `path` (예: `POST /api/comparisons`)
- `summary` — 동작
- `auth` — `guest`(비로그인 허용) | `user`(JWT 필요)

## 동작 (백엔드, `com.mkclicompare`)

1. **DTO**: `web/dto/*Dtos.kt` 에 요청/응답 data class (`*Req` / `*Res`). Entity 직접 노출 금지.
2. **Controller**: `web/*Controller.kt` — `@RestController`, DTO 만 반환. 검증은 `@Valid`.
   - `user` 권한이면 `@AuthenticationPrincipal AuthenticatedUser`, `guest` 면 nullable principal.
3. **Service**: `domain/**Service.kt` — 비즈니스 로직. 쓰기 메서드 `@Transactional`. 소유권 검증.
4. **Repository**: 필요 시 `JpaRepository` 메서드 추가.
5. **Security**: `guest` 경로는 `SecurityConfig` 의 permitAll 패턴에 포함되는지 확인.

## 동작 (프론트, `frontend/src/lib`)

6. **zod 타입**: `api-types.ts` 에 응답 스키마 + `z.infer` 타입.
7. **api 함수**: `api.ts` 에 호출 래퍼 추가 — 응답을 zod `.parse()` 로 런타임 검증. 컴포넌트는 이 함수만 사용.

## 검증

- `/api-contract-check` 로 백 컨트롤러 ↔ 프론트 zod 정합 확인.
- 백엔드 기동 후 `curl` 로 happy path 1회.

## 출력

추가/수정 파일 목록 + 계약(요청/응답 shape) 요약.
