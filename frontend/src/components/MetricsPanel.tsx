"use client";

import { Provider, Run } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";

/** 응답 텍스트에서 뽑은 정량 지표(추가 호출 0). 길이류는 우열이 아니므로 막대만, 속도는 최소 강조. */
function metricsOf(run: Run) {
  const text = run.responseText ?? "";
  const words = text.trim() ? text.trim().split(/\s+/).filter(Boolean).length : 0;
  const codeBlocks = Math.floor((text.match(/```/g)?.length ?? 0) / 2);
  return {
    chars: text.length,
    words,
    lines: text ? text.split("\n").length : 0,
    codeBlocks,
    readSec: Math.round((words / 200) * 60), // 200 wpm 기준
    latencyMs: run.latencyMs ?? null,
  };
}

const COLS: { key: keyof ReturnType<typeof metricsOf>; label: string; fmt: (v: number) => string; lowerBetter?: boolean }[] = [
  { key: "latencyMs", label: "응답속도", fmt: (v) => `${(v / 1000).toFixed(1)}s`, lowerBetter: true },
  { key: "chars", label: "글자수", fmt: (v) => `${v}` },
  { key: "words", label: "단어수", fmt: (v) => `${v}` },
  { key: "lines", label: "줄수", fmt: (v) => `${v}` },
  { key: "codeBlocks", label: "코드블록", fmt: (v) => `${v}` },
  { key: "readSec", label: "읽기시간", fmt: (v) => (v >= 60 ? `${Math.round(v / 60)}분` : `${v}초`) },
];

export default function MetricsPanel({ runs, providers }: { runs: Run[]; providers: Record<string, Provider> }) {
  const ok = runs.filter((r) => r.status === "ok");
  if (ok.length < 2) return null;
  const rows = ok.map((r) => ({ run: r, m: metricsOf(r) }));

  // 각 지표의 최솟값(속도) 강조용
  const fastest = rows
    .filter((x) => x.m.latencyMs != null)
    .sort((a, b) => (a.m.latencyMs! - b.m.latencyMs!))[0]?.run.providerId;

  return (
    <section className="overflow-x-auto rounded-2xl border border-black/10 bg-card dark:border-white/10">
      <div className="px-4 pt-3 text-sm font-black">📊 정량 비교</div>
      <table className="mt-2 w-full min-w-[520px] text-sm">
        <thead>
          <tr className="border-b border-black/10 text-left text-xs text-muted dark:border-white/10">
            <th className="px-4 py-2 font-semibold">Provider</th>
            {COLS.map((c) => (
              <th key={c.key} className="px-3 py-2 font-semibold">{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map(({ run, m }) => (
            <tr key={run.providerId} className="border-b border-black/5 last:border-0 dark:border-white/5">
              <td className="px-4 py-2.5 font-bold" style={{ color: providers[run.providerId]?.color }}>
                {providerEmoji(run.providerId)} {providers[run.providerId]?.displayName ?? run.providerId}
              </td>
              {COLS.map((c) => {
                const v = m[c.key];
                const highlight = c.key === "latencyMs" && run.providerId === fastest;
                return (
                  <td key={c.key} className="px-3 py-2.5">
                    <span className={highlight ? "font-black text-brand-600" : ""}>
                      {v == null ? "—" : c.fmt(v)}
                      {highlight && " ⚡"}
                    </span>
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <p className="px-4 pb-3 pt-1 text-[11px] text-muted">분량(글자·단어·줄)은 우열이 아니라 응답 스타일 차이입니다.</p>
    </section>
  );
}
