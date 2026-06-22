-- codex CLI 보정.
-- Purpose: codex 는 (1) git 저장소 밖에서 --skip-git-repo-check 가 필요하고,
--          (2) stdout 에 세션/추론/토큰 메타데이터가 섞여 최종 응답만 깔끔히 받으려면
--          --output-last-message <FILE> 로 파일 출력해야 한다.
-- Created: 2026-06-22
-- 원칙: 기존 V1 은 불변 — 새 컬럼/값은 V2 로 추가.

ALTER TABLE cli_provider ADD COLUMN output_file_flag TEXT;

UPDATE cli_provider
   SET command = 'codex exec --skip-git-repo-check',
       output_file_flag = '--output-last-message'
 WHERE id = 'codex';
