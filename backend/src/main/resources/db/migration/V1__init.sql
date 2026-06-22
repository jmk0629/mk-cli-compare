-- mk-cli-compare 초기 스키마.
-- Purpose: CLI provider 카탈로그 + 비교(comparison) + provider별 실행(comparison_run)
--          + 블라인드 투표(vote) + 카테고리 프리셋(prompt_preset) + 회원(users).
-- Created: 2026-06-22
-- 원칙: 카탈로그는 데이터(provider 추가 = INSERT), 가변 데이터는 opaque TEXT(JSON),
--       집계/필터 대상만 실제 컬럼으로 승격.

-- ── 회원 (SNS 소셜 로그인). 게스트 우선이라 비교/투표엔 불필요, 내 히스토리/동기화용. ──
CREATE TABLE users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  provider      TEXT NOT NULL,
  provider_id   TEXT NOT NULL,
  email         TEXT,
  nickname      TEXT,
  profile_image TEXT,
  delete_yn     TEXT NOT NULL DEFAULT 'N',
  deleted_at    TEXT,
  created_at    TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);
CREATE UNIQUE INDEX uq_users_provider ON users(provider, provider_id);

-- ── CLI provider 카탈로그 (데이터). 비교 대상 = 행. ──
--   runner_kind: 'cli' = 로컬 바이너리 서브프로세스, 'api' = HTTP API.
--   command    : cli 실행 프리픽스(예: 'claude -p'). 프롬프트는 마지막 argv 로 append.
--   bin_key    : cli.bins 매핑 키(예: 'claude','agy','codex'). 미지정 시 command 첫 토큰.
CREATE TABLE cli_provider (
  id           TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  vendor       TEXT NOT NULL,
  runner_kind  TEXT NOT NULL DEFAULT 'cli',
  command      TEXT NOT NULL DEFAULT '',
  bin_key      TEXT,
  model        TEXT,
  color        TEXT NOT NULL DEFAULT '#6366f1',
  icon         TEXT,
  enabled      INTEGER NOT NULL DEFAULT 1,
  sort_order   INTEGER NOT NULL DEFAULT 0
);

-- ── 카테고리 프리셋 (데이터). 같은 프롬프트를 어떤 성격으로 비교하나. ──
CREATE TABLE prompt_preset (
  id           TEXT PRIMARY KEY,
  category     TEXT NOT NULL,          -- character | coding | summary | reasoning | general
  title        TEXT NOT NULL,
  prompt       TEXT NOT NULL,
  description  TEXT,
  sort_order   INTEGER NOT NULL DEFAULT 0
);

-- ── 비교 1건 (프롬프트 + 카테고리). 게스트는 user_id NULL + guest_key. ──
CREATE TABLE comparison (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id      INTEGER REFERENCES users(id) ON DELETE SET NULL,
  guest_key    TEXT,                   -- 비로그인 식별(브라우저 로컬 키)
  category     TEXT NOT NULL DEFAULT 'general',
  prompt       TEXT NOT NULL,
  status       TEXT NOT NULL DEFAULT 'pending',  -- pending | done | error
  created_at   TEXT NOT NULL,
  completed_at TEXT
);
CREATE INDEX idx_comparison_created ON comparison(created_at);
CREATE INDEX idx_comparison_user ON comparison(user_id);
CREATE INDEX idx_comparison_guest ON comparison(guest_key);

-- ── provider별 실행 결과 (comparison 1 : N). 실패도 행으로 기록(graceful). ──
CREATE TABLE comparison_run (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  comparison_id INTEGER NOT NULL REFERENCES comparison(id) ON DELETE CASCADE,
  provider_id   TEXT NOT NULL REFERENCES cli_provider(id),
  status        TEXT NOT NULL DEFAULT 'pending',  -- pending | ok | error | timeout
  response_text TEXT,
  error_text    TEXT,
  exit_code     INTEGER,
  latency_ms    INTEGER,
  char_count    INTEGER,
  created_at    TEXT NOT NULL
);
CREATE INDEX idx_run_comparison ON comparison_run(comparison_id);
CREATE INDEX idx_run_provider ON comparison_run(provider_id);

-- ── 블라인드 투표 (comparison 의 어떤 provider 가 이겼나). 차원별(dimension) 다중 투표 허용. ──
--   dimension: overall | quality | speed | persona(캐릭터성) | creativity
CREATE TABLE vote (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  comparison_id INTEGER NOT NULL REFERENCES comparison(id) ON DELETE CASCADE,
  user_id       INTEGER REFERENCES users(id) ON DELETE SET NULL,
  guest_key     TEXT,
  dimension     TEXT NOT NULL DEFAULT 'overall',
  winner_provider_id TEXT NOT NULL REFERENCES cli_provider(id),
  created_at    TEXT NOT NULL
);
CREATE INDEX idx_vote_comparison ON vote(comparison_id);
CREATE INDEX idx_vote_winner ON vote(winner_provider_id);
CREATE INDEX idx_vote_dimension ON vote(dimension);

-- ── provider 카탈로그 시드 (3종 구독형 CLI). ──
INSERT INTO cli_provider (id, display_name, vendor, runner_kind, command, bin_key, model, color, icon, enabled, sort_order) VALUES
  ('claude', 'Claude Code',          'anthropic', 'cli', 'claude -p',  'claude', NULL, '#d97757', 'claude', 1, 1),
  ('gemini', 'Antigravity (Gemini)', 'google',    'cli', 'agy -p',     'agy',    NULL, '#4285f4', 'gemini', 1, 2),
  ('codex',  'Codex (GPT)',          'openai',    'cli', 'codex exec', 'codex',  NULL, '#10a37f', 'codex',  1, 3);

-- ── 카테고리 프리셋 시드. ──
INSERT INTO prompt_preset (id, category, title, prompt, description, sort_order) VALUES
  ('char_cat',   'character', '고양이 집사 캐릭터', '너는 새침한 고양이 집사 캐릭터야. 말끝마다 "~냥"을 붙이고, 오늘 날씨가 좋으니 산책을 권하는 짧은 대사를 해줘.', '캐릭터 챗 — 페르소나 유지력', 1),
  ('char_tsun',  'character', '츤데레 비서',       '너는 츤데레 성격의 AI 비서야. 사용자가 야근한다고 하면 걱정되지만 퉁명스럽게 표현하는 한 마디를 해줘.', '캐릭터 챗 — 감정 표현', 2),
  ('code_fizz',  'coding',    'FizzBuzz',          '1부터 20까지 FizzBuzz 를 출력하는 파이썬 함수를 작성하고, 한 줄로 설명해줘.', '코딩 — 기본 정확성', 3),
  ('code_bug',   'coding',    '버그 찾기',          '다음 자바스크립트의 버그를 찾아 고쳐줘: function sum(a,b){return a-b}', '코딩 — 디버깅', 4),
  ('sum_news',   'summary',   '세 줄 요약',         '인공지능의 역사를 모르는 사람에게 핵심만 세 줄로 요약해줘.', '요약 — 압축력', 5),
  ('reason_log', 'reasoning', '논리 추론',          '철수는 영희보다 키가 크고, 영희는 민수보다 크다. 가장 키가 작은 사람은 누구이며 그 이유는?', '추론 — 논리 전개', 6),
  ('gen_hello',  'general',   '자기소개',           '너 자신을 처음 만난 사람에게 친근하게 소개하는 짧은 글을 써줘.', '일반 — 톤/문체', 7);
