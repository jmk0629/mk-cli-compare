"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { getComparison, getProviders } from "@/lib/api";
import { CATEGORIES, Comparison, Provider, Run } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";
import Markdown from "@/components/Markdown";
import CopyButton from "@/components/CopyButton";
import ShareButton from "@/components/ShareButton";
import JudgePanel from "@/components/JudgePanel";
import MetricsPanel from "@/components/MetricsPanel";

/** 비교 상세 — 한 비교의 모든 데이터(프롬프트, provider별 전체 응답·지표·에러)를 펼쳐 본다. */
export default function ComparisonDetailPage() {
  const params = useParams();
  const id = Number(params.id);
  const [comparison, setComparison] = useState<Comparison | null>(null);
  const [providers, setProviders] = useState<Record<string, Provider>>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError("잘못된 비교 ID 입니다.");
      return;
    }
    getProviders()
      .then((ps) => setProviders(Object.fromEntries(ps.map((p) => [p.id, p]))))
      .catch(() => {});
    getComparison(id)
      .then(setComparison)
      .catch(() => setError("비교를 불러오지 못했습니다. (백엔드 실행 여부 확인)"));
  }, [id]);

  const category = CATEGORIES.find((c) => c.key === comparison?.category);

  // 가장 빠른 ok 응답 = 속도 우승(표시용).
  const fastest = comparison
    ? comparison.runs
        .filter((r) => r.status === "ok" && r.latencyMs != null)
        .sort((a, b) => (a.latencyMs ?? 0) - (b.latencyMs ?? 0))[0]?.providerId
    : undefined;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <Link href="/history" className="text-sm font-semibold text-brand-600 hover:underline">
          ← 기록으로
        </Link>
        {comparison && <ShareButton path={`/comparison/${comparison.id}`} />}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {!comparison && !error && <p className="text-sm text-muted">불러오는 중…</p>}

      {comparison && (
        <>
          {/* 헤더: 프롬프트 + 메타 */}
          <section className="rounded-2xl border border-black/10 bg-card p-5 shadow-sm dark:border-white/10">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-semibold text-brand-700 dark:bg-white/10 dark:text-brand-200">
                {category ? `${category.emoji} ${category.label}` : comparison.category}
              </span>
              <StatusPill status={comparison.status} />
              <span className="text-xs text-muted">#{comparison.id}</span>
            </div>
            <p className="text-base font-semibold leading-relaxed">{comparison.prompt}</p>
            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted">
              <span>실행 {comparison.createdAt.replace("T", " ").slice(0, 19)}</span>
              {comparison.completedAt && (
                <span>완료 {comparison.completedAt.replace("T", " ").slice(0, 19)}</span>
              )}
              <span>응답 {comparison.runs.length}개</span>
            </div>
          </section>

          {/* 요약 지표 테이블 */}
          <section className="overflow-x-auto rounded-2xl border border-black/10 bg-card dark:border-white/10">
            <table className="w-full min-w-[460px] text-sm">
              <thead>
                <tr className="border-b border-black/10 text-left text-xs text-muted dark:border-white/10">
                  <th className="px-4 py-3 font-semibold">Provider</th>
                  <th className="px-4 py-3 font-semibold">모델</th>
                  <th className="px-4 py-3 font-semibold">상태</th>
                  <th className="px-4 py-3 font-semibold">응답시간</th>
                  <th className="px-4 py-3 font-semibold">길이</th>
                  <th className="px-4 py-3 font-semibold">exit</th>
                </tr>
              </thead>
              <tbody>
                {comparison.runs.map((r) => {
                  const p = providers[r.providerId];
                  return (
                    <tr key={r.providerId} className="border-b border-black/5 last:border-0 dark:border-white/5">
                      <td className="px-4 py-3 font-bold" style={{ color: p?.color }}>
                        {providerEmoji(r.providerId)} {p?.displayName ?? r.providerId}
                        {fastest === r.providerId && <span className="ml-1 text-xs text-brand-600">⚡최속</span>}
                      </td>
                      <td className="px-4 py-3 text-muted">{r.model ?? "기본"}</td>
                      <td className="px-4 py-3"><StatusPill status={r.status} /></td>
                      <td className="px-4 py-3">{r.latencyMs != null ? `${(r.latencyMs / 1000).toFixed(1)}s` : "—"}</td>
                      <td className="px-4 py-3">{r.charCount != null ? `${r.charCount}자` : "—"}</td>
                      <td className="px-4 py-3 text-muted">{r.exitCode ?? "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </section>

          {/* 정량 비교 */}
          <MetricsPanel runs={comparison.runs} providers={providers} />

          {/* AI 자동 심판 */}
          {comparison.runs.filter((r) => r.status === "ok").length >= 2 && (
            <JudgePanel comparisonId={comparison.id} providers={providers} />
          )}

          {/* provider별 전체 응답 */}
          <section className="grid gap-4 lg:grid-cols-3">
            {comparison.runs.map((r) => (
              <DetailCard key={r.providerId} run={r} provider={providers[r.providerId]} />
            ))}
          </section>
        </>
      )}
    </div>
  );
}

function DetailCard({ run, provider }: { run: Run; provider?: Provider }) {
  const ok = run.status === "ok";
  return (
    <div className="flex flex-col rounded-2xl border border-black/10 bg-card p-4 dark:border-white/10">
      <div className="mb-2 flex items-center justify-between">
        <span className="flex flex-col gap-0.5">
          <span className="flex items-center gap-2 font-bold" style={{ color: provider?.color }}>
            <span aria-hidden>{providerEmoji(run.providerId)}</span>
            {provider?.displayName ?? run.providerId}
          </span>
          {run.model && <span className="text-xs text-muted">{run.model}</span>}
        </span>
        <div className="flex items-center gap-2">
          {ok && run.responseText && <CopyButton text={run.responseText} />}
          <span className="text-xs text-muted">
            {run.latencyMs != null ? `${(run.latencyMs / 1000).toFixed(1)}s` : ""}
          </span>
        </div>
      </div>
      <div className="max-h-[32rem] overflow-auto rounded-xl bg-black/[0.03] p-3 dark:bg-white/[0.04]">
        {ok ? (
          <Markdown content={run.responseText ?? ""} />
        ) : (
          <pre className="whitespace-pre-wrap break-words text-sm text-muted">{`⚠️ ${run.status}\n\n${run.errorText ?? "응답 없음"}`}</pre>
        )}
      </div>
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  const map: Record<string, { label: string; cls: string }> = {
    ok: { label: "완료", cls: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300" },
    done: { label: "완료", cls: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300" },
    error: { label: "실패", cls: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" },
    timeout: { label: "시간초과", cls: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300" },
    pending: { label: "실행중", cls: "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300" },
  };
  const s = map[status] ?? { label: status, cls: "bg-gray-100 text-gray-600" };
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${s.cls}`}>{s.label}</span>;
}
