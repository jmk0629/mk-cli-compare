# 2026-06-22 — SSE 실시간 스트리밍 (폴링 폴백 유지)

## 목표 (시니어 관점)
- 폴링은 1.5s 지연이 있다. 각 CLI 가 끝나는 **즉시** 카드가 채워지면 체감 속도가 크게 오른다.
- SSE 로 서버 푸시, 단 EventSource 가 막히는 환경(프록시 버퍼링 등)을 위해 폴링 폴백을 남긴다.

## 설계
- **백엔드**
  - `ComparisonStreamService`: comparisonId 별 `SseEmitter` 목록(메모리). `subscribe/publish/complete`.
    (단일 인스턴스 가정 — 스케일아웃 시 Redis pub/sub 로 확장.)
  - `ComparisonExecutor`: run 갱신마다 `publish("update", 스냅샷)`, 완료 시 `complete("done", 스냅샷)`.
  - `ComparisonSnapshot`: JPA Entity 직접 노출 없이 web `ComparisonRes` 와 **동일 필드명** → 프론트가 같은 zod 로 파싱.
  - `GET /api/comparisons/{id}/stream`(text/event-stream, 공개): 구독 즉시 현재 스냅샷 전송 후 실시간 push.
- **프론트**
  - `CompareView`: pending 이면 `EventSource(comparisonStreamUrl)` 구독, `update`/`done` 이벤트로 카드 갱신.
  - `onerror`(SSE 끊김) → 1.5s 폴링으로 자동 폴백. 정리 시 ES/인터벌 모두 해제.

## 검증 (라이브)
- curl SSE: `event:update`(초기 pending) → claude ok(3.8s) → codex ok → gemini ok → `event:done`.
  각 CLI 완료 즉시 push 확인.
- CORS: `Access-Control-Allow-Origin: http://localhost:3000`, `Content-Type: text/event-stream` 확인.
- 곁들인 정리: 성공 run 의 stderr 노이즈(codex 세션/토큰 로그)를 errorText 에서 제거해 페이로드 경량화.

## 확장 여지
- 멀티 인스턴스: Redis/메시지브로커 pub/sub 로 emitter fan-out.
- last-event-id 재연결 지원.
