-- AI 자동 심판(LLM-as-judge) 평결 저장.
-- Purpose: 완료된 비교의 응답들을 블라인드(A/B/C)로 심판 CLI 에 보내 순위·점수·이유를 받아 저장.
--          사람 블라인드 투표와 별개로 즉시 객관 평가 제공.
-- Created: 2026-06-22
-- 원칙: 기존 V1~V5 불변. verdict 가변 구조는 opaque TEXT(JSON), 집계 대상(winner)만 컬럼 승격.

CREATE TABLE judge_verdict (
  id                 INTEGER PRIMARY KEY AUTOINCREMENT,
  comparison_id      INTEGER NOT NULL REFERENCES comparison(id) ON DELETE CASCADE,
  judge_provider_id  TEXT NOT NULL REFERENCES cli_provider(id),
  judge_model        TEXT,
  winner_provider_id TEXT REFERENCES cli_provider(id),
  verdict_json       TEXT NOT NULL,   -- {winnerProviderId, summary, scores:[{providerId,score,reason}]}
  status             TEXT NOT NULL DEFAULT 'ok',  -- ok | error
  created_at         TEXT NOT NULL
);
CREATE INDEX idx_judge_comparison ON judge_verdict(comparison_id);
CREATE INDEX idx_judge_winner ON judge_verdict(winner_provider_id);
