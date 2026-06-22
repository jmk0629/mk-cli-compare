"use client";

import { useEffect, useState } from "react";
import { getLeaderboard, getStats } from "@/lib/api";
import { Ranking, DIMENSIONS, CATEGORIES, Stats } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";

export default function LeaderboardPage() {
  const [rankings, setRankings] = useState<Ranking[] | null>(null);
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [category, setCategory] = useState<string>(""); // "" = 전체
  const [dimension, setDimension] = useState<string>(""); // "" = 전체

  useEffect(() => {
    getStats().then(setStats).catch(() => {});
  }, []);

  useEffect(() => {
    setRankings(null);
    getLeaderboard(category || undefined, dimension || undefined)
      .then((l) => setRankings(l.rankings))
      .catch(() => setError("리더보드를 불러오지 못했습니다. 백엔드가 켜져 있나요?"));
  }, [category, dimension]);

  const StatCard = ({ label, value, color }: { label: string; value: string; color?: string }) => (
    <div className="rounded-2xl border border-black/10 bg-card p-3 dark:border-white/10">
      <div className="text-xs text-muted">{label}</div>
      <div className="mt-1 text-lg font-black" style={color ? { color } : undefined}>
        {value}
      </div>
    </div>
  );

  const maxWins = Math.max(1, ...(rankings ?? []).map((r) => r.totalWins));
  const chip = (active: boolean) =>
    `min-h-9 rounded-full px-3 py-1 text-xs font-semibold transition ${
      active ? "bg-brand-600 text-white" : "bg-brand-100 text-brand-700 hover:bg-brand-200 dark:bg-white/10 dark:text-brand-200"
    }`;

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="text-xl font-black">리더보드</h1>
        <p className="text-sm text-muted">누적 블라인드 투표 기준 승수 · 평균 속도 · 응답 성공률.</p>
      </div>

      {/* 통계 개요 */}
      {stats && (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <StatCard label="총 비교" value={`${stats.totalComparisons}`} />
          <StatCard label="총 투표" value={`${stats.totalVotes}`} />
          <StatCard
            label="🏆 최다승"
            value={stats.topProvider ? `${providerEmoji(stats.topProvider.id)} ${stats.topProvider.value}승` : "—"}
            color={stats.topProvider?.color}
          />
          <StatCard
            label="⚡ 최속(평균)"
            value={stats.fastestProvider ? `${providerEmoji(stats.fastestProvider.id)} ${(stats.fastestProvider.value / 1000).toFixed(1)}s` : "—"}
            color={stats.fastestProvider?.color}
          />
        </div>
      )}

      {/* 필터: 카테고리 / 차원 */}
      <div className="flex flex-col gap-2 rounded-2xl border border-black/10 bg-card p-3 dark:border-white/10">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-xs font-bold text-muted">카테고리</span>
          <button onClick={() => setCategory("")} className={chip(category === "")}>전체</button>
          {CATEGORIES.map((c) => (
            <button key={c.key} onClick={() => setCategory(c.key)} className={chip(category === c.key)}>
              {c.emoji} {c.label}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="mr-1 text-xs font-bold text-muted">차원</span>
          <button onClick={() => setDimension("")} className={chip(dimension === "")}>전체</button>
          {DIMENSIONS.map((d) => (
            <button key={d.key} onClick={() => setDimension(d.key)} className={chip(dimension === d.key)}>
              {d.label}
            </button>
          ))}
        </div>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {!rankings && !error && <p className="text-sm text-muted">불러오는 중…</p>}

      <div className="flex flex-col gap-3">
        {rankings?.map((r, i) => (
          <div key={r.providerId} className="rounded-2xl border border-black/10 bg-card p-4 dark:border-white/10">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 font-bold" style={{ color: r.color }}>
                <span className="text-muted">#{i + 1}</span>
                <span aria-hidden>{providerEmoji(r.providerId)}</span>
                <span>{r.displayName}</span>
              </div>
              <span className="text-lg font-black text-brand-600">{r.totalWins}승</span>
            </div>

            {/* 승수 막대 */}
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-black/5 dark:bg-white/10">
              <div
                className="h-full rounded-full"
                style={{ width: `${(r.totalWins / maxWins) * 100}%`, background: r.color }}
              />
            </div>

            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
              <span>실행 {r.totalRuns}회</span>
              <span>성공률 {(r.okRate * 100).toFixed(0)}%</span>
              <span>평균 {r.avgLatencyMs != null ? `${(r.avgLatencyMs / 1000).toFixed(1)}s` : "—"}</span>
            </div>

            {/* 차원별 승수 */}
            <div className="mt-2 flex flex-wrap gap-1.5">
              {DIMENSIONS.map((d) => {
                const w = r.winsByDimension[d.key] ?? 0;
                if (!w) return null;
                return (
                  <span
                    key={d.key}
                    className="rounded-full bg-brand-50 px-2 py-0.5 text-xs text-brand-700 dark:bg-white/5 dark:text-brand-200"
                  >
                    {d.label} {w}
                  </span>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
