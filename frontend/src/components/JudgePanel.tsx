"use client";

import { useEffect, useState } from "react";
import { getJudgeVerdict, judgeComparison, ApiError } from "@/lib/api";
import { JudgeVerdict, Provider } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";

/** AI 자동 심판 패널 — 평결을 보여주고, 없으면 버튼으로 심판을 실행(옵트인, CLI 1회). */
export default function JudgePanel({
  comparisonId,
  providers,
  enabled = true,
}: {
  comparisonId: number;
  providers: Record<string, Provider>;
  enabled?: boolean;
}) {
  const [verdict, setVerdict] = useState<JudgeVerdict | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getJudgeVerdict(comparisonId).then(setVerdict).catch(() => {});
  }, [comparisonId]);

  async function run() {
    setLoading(true);
    setError(null);
    try {
      setVerdict(await judgeComparison(comparisonId));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "AI 심판 실행에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }

  const name = (id: string) => providers[id]?.displayName ?? id;
  const color = (id: string) => providers[id]?.color ?? "#6366f1";
  const judgeName = verdict ? name(verdict.judgeProviderId) : "";

  return (
    <section className="rounded-2xl border border-black/10 bg-card p-4 dark:border-white/10">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-base font-black">🤖 AI 심판</h3>
        {enabled && (
          <button
            onClick={run}
            disabled={loading}
            className="min-h-9 rounded-full bg-brand-600 px-3 py-1.5 text-sm font-bold text-white transition hover:bg-brand-700 disabled:opacity-50"
          >
            {loading ? "심판 중… (수 초)" : verdict ? "다시 심판" : "AI 심판에게 평가받기"}
          </button>
        )}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {!verdict && !loading && !error && (
        <p className="text-sm text-muted">
          정체를 가린 채 심판 AI 가 세 응답을 채점합니다. 사람 투표와 비교해보세요.
        </p>
      )}

      {verdict?.status === "error" && (
        <p className="text-sm text-amber-600">심판이 평결을 내지 못했어요. 다시 시도해보세요.</p>
      )}

      {verdict && verdict.status === "ok" && (
        <div className="flex flex-col gap-3">
          {verdict.summary && <p className="text-sm">{verdict.summary}</p>}
          <div className="flex flex-col gap-2">
            {verdict.scores.map((s) => {
              const win = s.providerId === verdict.winnerProviderId;
              return (
                <div key={s.providerId} className="rounded-xl bg-black/[0.03] p-3 dark:bg-white/[0.04]">
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 font-bold" style={{ color: color(s.providerId) }}>
                      {providerEmoji(s.providerId)} {name(s.providerId)}
                      {win && <span className="text-xs text-brand-600">👑 심판 선택</span>}
                    </span>
                    <span className="text-sm font-black">{s.score.toFixed(1)}</span>
                  </div>
                  <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-black/5 dark:bg-white/10">
                    <div className="h-full rounded-full" style={{ width: `${(s.score / 10) * 100}%`, background: color(s.providerId) }} />
                  </div>
                  {s.reason && <p className="mt-1.5 text-xs text-muted">{s.reason}</p>}
                </div>
              );
            })}
          </div>
          <p className="text-[11px] text-muted">
            심판: {providerEmoji(verdict.judgeProviderId)} {judgeName} · 심판도 참가자 중 하나라 자기 응답에 유리할 수 있어요(블라인드로 완화).
          </p>
        </div>
      )}
    </section>
  );
}
