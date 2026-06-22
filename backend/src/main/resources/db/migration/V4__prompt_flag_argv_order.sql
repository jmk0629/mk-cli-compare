-- argv 순서 버그 수정.
-- Purpose: agy(Gemini)의 `-p`(--prompt)는 **다음 토큰을 프롬프트 값으로 소비**한다.
--          기존엔 `agy -p --model X "<prompt>"` 로 조립되어 `-p` 가 `--model` 을 프롬프트로 먹고,
--          실제 프롬프트는 무시되어 "Summary of work" 같은 엉뚱한 에이전트 응답이 나왔다.
-- Fix: print/prompt 플래그를 **마지막**(프롬프트 직전)에 두고, 모델/출력 플래그는 그 앞에 둔다.
--      이를 위해 prompt_flag 를 command 에서 분리한다.
--      최종 argv = [bin] + command(서브커맨드/정적플래그) + [model_flag model] + [output_flag file]
--                 + [prompt_flag] + [prompt]
-- Created: 2026-06-22
-- 원칙: 기존 V1~V3 불변 — 새 컬럼/값은 V4 로.

ALTER TABLE cli_provider ADD COLUMN prompt_flag TEXT;

-- claude: `-p` 는 불리언(print)이지만 일관성을 위해 prompt_flag 로 분리. command 는 바이너리만.
UPDATE cli_provider SET command = 'claude', prompt_flag = '-p' WHERE id = 'claude';
-- gemini=agy: `-p` 는 프롬프트를 값으로 받음 → 반드시 프롬프트 직전.
UPDATE cli_provider SET command = 'agy',    prompt_flag = '-p' WHERE id = 'gemini';
-- codex: 프롬프트는 exec 의 positional 인자 → prompt_flag 없음. command 유지.
UPDATE cli_provider SET prompt_flag = NULL WHERE id = 'codex';
