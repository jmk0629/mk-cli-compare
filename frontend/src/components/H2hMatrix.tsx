"use client";

import { useEffect, useState } from "react";
import { getHeadToHead } from "@/lib/api";
import { H2h } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";

/** 1:1 승률 매트릭스 — cell(행=A, 열=B) = A가 B를 상대로 거둔 승률. 투표 기반. */
export default function H2hMatrix() {
  const [data, setData] = useState<H2h | null>(null);

  useEffect(() => {
    getHeadToHead().then(setData).catch(() => {});
  }, []);

  if (!data || data.providers.length < 2) return null;
  const wins = new Map<string, number>();
  data.pairs.forEach((p) => wins.set(`${p.winner}|${p.loser}`, p.wins));
  const total = data.pairs.reduce((a, p) => a + p.wins, 0);
  if (total === 0) return null;

  const cell = (a: string, b: string) => {
    if (a === b) return null;
    const aw = wins.get(`${a}|${b}`) ?? 0;
    const bw = wins.get(`${b}|${a}`) ?? 0;
    const t = aw + bw;
    if (t === 0) return { rate: null as number | null, aw, bw };
    return { rate: aw / t, aw, bw };
  };
  const rateColor = (rate: number | null) => {
    if (rate == null) return undefined;
    if (rate >= 0.6) return "rgba(34,197,94,0.18)";
    if (rate <= 0.4) return "rgba(239,68,68,0.16)";
    return "rgba(99,102,241,0.10)";
  };

  return (
    <section className="overflow-x-auto rounded-2xl border border-black/10 bg-card p-3 dark:border-white/10">
      <div className="mb-2 text-sm font-black">⚔️ 1:1 승률 (행이 열을 상대로)</div>
      <table className="w-full min-w-[420px] text-center text-sm">
        <thead>
          <tr className="text-xs text-muted">
            <th className="px-2 py-1.5"></th>
            {data.providers.map((p) => (
              <th key={p.id} className="px-2 py-1.5" style={{ color: p.color }} title={p.displayName}>
                {providerEmoji(p.id)} {p.id}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.providers.map((row) => (
            <tr key={row.id} className="border-t border-black/5 dark:border-white/5">
              <td className="px-2 py-2 text-left text-xs font-bold" style={{ color: row.color }}>
                {providerEmoji(row.id)} {row.id}
              </td>
              {data.providers.map((col) => {
                const c = cell(row.id, col.id);
                if (c === null) return <td key={col.id} className="px-2 py-2 text-muted">—</td>;
                return (
                  <td key={col.id} className="px-2 py-2" style={{ background: rateColor(c.rate) }}>
                    {c.rate == null ? (
                      <span className="text-xs text-muted">·</span>
                    ) : (
                      <span>
                        <span className="font-black">{Math.round(c.rate * 100)}%</span>
                        <span className="block text-[10px] text-muted">{c.aw}–{c.bw}</span>
                      </span>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="mt-2 text-[11px] text-muted">초록=우세, 빨강=열세. 칸 아래 숫자는 승–패.</p>
    </section>
  );
}
