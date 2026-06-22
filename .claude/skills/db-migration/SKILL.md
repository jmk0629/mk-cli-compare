---
name: db-migration
description: mk-cli-compare 의 다음 Flyway 마이그레이션 V{N+1}__{slug}.sql 을 생성한다. 기존 V 파일 수정을 차단하고 SQLite 호환 DDL 을 lint 한다.
---

# db-migration

`backend/src/main/resources/db/migration/` 에 다음 순번 마이그레이션을 만든다.

## 입력

- `slug` — 변경 요약 (예: `add_vote_dimension`, `provider_icon`)
- `summary` — 무엇을/왜

## 동작

1. **다음 번호 계산**: 기존 `V{N}__*.sql` 중 최대 N → `V{N+1}` 생성. 기존 파일은 **절대 수정 금지**.
2. **DDL 작성** (SQLite 호환):
   - 타입: `INTEGER` / `TEXT` / `REAL` / `BLOB`. `BOOLEAN`→`INTEGER`(0/1), 시각→`TEXT`(ISO-8601) 또는 epoch `INTEGER`.
   - PK: `id INTEGER PRIMARY KEY AUTOINCREMENT` 또는 자연키 `TEXT`.
   - 가변/구조 데이터는 opaque `TEXT`(JSON) 컬럼. 집계·랭킹·필터 대상만 실제 컬럼으로 승격.
   - FK 는 명시하되 SQLite 는 기본 비강제 — 앱 레벨에서도 검증.
3. **lint**: `ALTER TABLE ... ADD COLUMN` 만 안전(SQLite 의 DROP/ALTER COLUMN 제약 주의). 컬럼 삭제·타입변경은 테이블 재생성 패턴.
4. **검증**: `sqlite3 :memory:` 에 V1..V{N+1} 순차 적용해 문법 확인(선택).

## 금지

- 기존 `V{N}__*.sql` 편집/삭제 (이미 적용된 마이그레이션 불변).
- `ddl-auto` 로 스키마 생성 (Flyway 단독 권한자).

## 출력

생성한 파일 경로 + DDL 요약 + 영향 테이블.
