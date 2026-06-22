# 2026-06-22 — 비동기 실행 + 폴링 (점진적 결과, 모바일 견고성)

## 문제 (시니어 관점)
- 기존 `POST /api/comparisons` 는 **3개 CLI 완료까지 동기 블로킹**(최대 수십 초).
- 모바일/네트워크에서 장시간 단일 요청 → 타임아웃·진행 표시 없음·재시도 불가. 확장성 나쁨.

## 설계
- **생성과 실행 분리**:
  - `POST` → comparison(pending) + provider별 **pending run** 즉시 저장 → 곧바로 반환(HTTP 비블로킹).
  - 실제 CLI 실행은 `ComparisonExecutor`(`@Async("comparisonTaskExecutor")`)가 백그라운드에서 병렬 수행.
  - 각 provider 완료 **즉시** 해당 run 행 갱신 → 점진적 가시성.
  - 모두 끝나면 comparison 상태 확정(done/error).
- **프론트 폴링**: `GET /api/comparisons/{id}` 를 1.5s 간격 폴링 → 카드를 pending→ok 로 점진 갱신,
  done/error 시 폴링 중단. 카드 순서는 providerId 시퀀스로 고정(블라인드 셔플 유지).

## 변경
- 백엔드: `AsyncConfig`(ThreadPoolTaskExecutor `comparisonTaskExecutor`), `@EnableAsync`,
  `ComparisonExecutor`(병렬 실행 + run별 갱신 + finalize), `ComparisonService.createAsync`(pending 저장),
  `ComparisonRunRepository.findByComparisonIdAndProviderId`.
- 프론트: `CompareView` 폴링(useEffect+interval), 고정 순서(orderIds), "실행 중" 스피너,
  `ResultCard` pending 상태 스피너.

## 검증 (라이브)
- `POST` 응답 **0초**(status=pending, run 3개 pending).
- 폴링: t=4s 에 claude 먼저 ok(다른 둘 pending) → 점진 갱신 확인. 최종 done, 3개 ok.
- 응답 JSON `JSON.parse`(브라우저 동급) 정상 — 제어문자 escape 확인.

## 확장 여지
- 폴링 → SSE(`/stream`) 전환 시 즉시성 향상(인프라는 ComparisonExecutor 이벤트 발행만 추가).
