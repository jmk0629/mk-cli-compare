-- provider 별 선택 가능한 모델 카탈로그.
-- Purpose: 각 CLI 가 여러 모델을 지원(claude: opus/sonnet/haiku, agy: Gemini/Claude/GPT-OSS,
--          codex: gpt-5.x) → 사용자가 비교 시 provider 별 모델을 select 로 고를 수 있게 한다.
-- Created: 2026-06-22
-- 원칙: 모델도 데이터 — 행 추가로 확장. model_arg 는 CLI 에 그대로 넘길 인자(whitelist).

-- 모델 플래그(provider 별 상이): claude/agy = --model, codex = -m.
ALTER TABLE cli_provider ADD COLUMN model_flag TEXT;
UPDATE cli_provider SET model_flag = '--model' WHERE id IN ('claude', 'gemini');
UPDATE cli_provider SET model_flag = '-m'      WHERE id = 'codex';

-- 실행에 사용된 모델 기록(비교 상세에서 표시).
ALTER TABLE comparison_run ADD COLUMN model TEXT;

CREATE TABLE provider_model (
  id          TEXT PRIMARY KEY,
  provider_id TEXT NOT NULL REFERENCES cli_provider(id),
  model_arg   TEXT NOT NULL,   -- CLI 에 넘길 정확한 인자(예: 'haiku', 'Gemini 3.1 Pro (High)', 'gpt-5.4-mini')
  label       TEXT NOT NULL,   -- 화면 표시명
  is_default  INTEGER NOT NULL DEFAULT 0,
  sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_provider_model_provider ON provider_model(provider_id);

-- claude
INSERT INTO provider_model (id, provider_id, model_arg, label, is_default, sort_order) VALUES
  ('claude_opus',   'claude', 'opus',   'Opus 4.8',   1, 1),
  ('claude_sonnet', 'claude', 'sonnet', 'Sonnet 4.6', 0, 2),
  ('claude_haiku',  'claude', 'haiku',  'Haiku 4.5',  0, 3);

-- gemini = Antigravity(agy). model_arg = `agy models` 출력 문자열 그대로.
INSERT INTO provider_model (id, provider_id, model_arg, label, is_default, sort_order) VALUES
  ('agy_flash_med',  'gemini', 'Gemini 3.5 Flash (Medium)',   'Gemini 3.5 Flash (Medium)', 1, 1),
  ('agy_flash_high', 'gemini', 'Gemini 3.5 Flash (High)',     'Gemini 3.5 Flash (High)',   0, 2),
  ('agy_flash_low',  'gemini', 'Gemini 3.5 Flash (Low)',      'Gemini 3.5 Flash (Low)',    0, 3),
  ('agy_pro_low',    'gemini', 'Gemini 3.1 Pro (Low)',        'Gemini 3.1 Pro (Low)',      0, 4),
  ('agy_pro_high',   'gemini', 'Gemini 3.1 Pro (High)',       'Gemini 3.1 Pro (High)',     0, 5),
  ('agy_claude_son', 'gemini', 'Claude Sonnet 4.6 (Thinking)','Claude Sonnet 4.6 (Thinking)', 0, 6),
  ('agy_claude_op',  'gemini', 'Claude Opus 4.6 (Thinking)',  'Claude Opus 4.6 (Thinking)', 0, 7),
  ('agy_gptoss',     'gemini', 'GPT-OSS 120B (Medium)',       'GPT-OSS 120B (Medium)',     0, 8);

-- codex
INSERT INTO provider_model (id, provider_id, model_arg, label, is_default, sort_order) VALUES
  ('codex_55',      'codex', 'gpt-5.5',      'GPT-5.5',      1, 1),
  ('codex_54',      'codex', 'gpt-5.4',      'GPT-5.4',      0, 2),
  ('codex_54_mini', 'codex', 'gpt-5.4-mini', 'GPT-5.4-Mini', 0, 3);
