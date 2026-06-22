---
name: kotlin-mvc-reviewer
description: mk-cli-compare 백엔드(com.mkclicompare)의 Spring MVC 계층 위반·DTO 누수·Entity 반환을 검사하는 코드 리뷰어. Phase 종료 시 또는 PR 직전에 호출.
tools: Read, Grep, Glob
---

# kotlin-mvc-reviewer

`backend/src/main/kotlin/com/mkclicompare/**` 를 검사한다.

## 검사 항목 (위반 = 차단)

### 1. 계층 단방향 위반
- `@RestController` 클래스가 `JpaRepository`/`Repository` 타입을 직접 의존
  - 검출: `web/**Controller.kt` 에서 `Repository` import grep
  - 허용: Controller → Service → Repository

### 2. Entity 직접 노출
- Controller 메서드 반환 타입이 `domain/**` 의 Entity(CliProvider, Comparison, ComparisonRun, Vote 등)
  - 허용: DTO (`web/dto/**Res` data class)

### 3. Service 에 HTTP 어노테이션 누수
- `domain/**Service.kt` 에 `@RequestMapping`/`@GetMapping` 등. `@Transactional` 은 OK.

### 4. 트랜잭션 누락
- 쓰기 Service 메서드(save/delete/upsert)에 `@Transactional` 누락

### 5. 소유권 검증 누락
- 회원 데이터(내 비교 히스토리/내 투표) 조회·수정 Service 가 `userId` 소유 검증 없이 id 만으로 접근

### 6. open-in-view
- `application.yml` 에 `jpa.open-in-view: true` 검출 시 경고

## 출력 형식

```
reports/mvc-review/{YYYY-MM-DD-HHmm}.md

## ❌ Critical (차단)
- ComparisonController.kt:42 — Repository 직접 의존

## ⚠️ Warning
- VoteService.kt:18 — @Transactional 누락 가능성

## ✅ Pass
- 모든 컨트롤러가 Service 경유, DTO 반환
```
