"use client";

import { useEffect, useMemo, useState } from "react";
import {
  CATEGORIES,
  Comparison,
  DIMENSIONS,
  Dimension,
  Preset,
  Provider,
  Run,
} from "@/lib/api-types";
import { castVote, createComparison, getPresets, getProviders, ApiError } from "@/lib/api";
import { getGuestKey } from "@/lib/auth";
import ResultCard from "./ResultCard";

/** 비교 메인 플로우: 프롬프트 입력 → 3 CLI 실행 → 블라인드 카드 → 차원별 투표 → 정체 공개. */
export default function CompareView() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [presets, setPresets] = useState<Preset[]>([]);
  const [prompt, setPrompt] = useState("");
  const [category, setCategory] = useState<string>("general");
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<Comparison | null>(null);

  // 블라인드: 결과 받으면 카드 순서를 한 번 섞는다(위치 편향 방지).
  const [order, setOrder] = useState<Run[]>([]);
  const [showAll, setShowAll] = useState(false); // 투표 없이 전체 보기 토글
  const [votes, setVotes] = useState<Record<string, string>>({}); // dimension → providerId
  const [voting, setVoting] = useState<Dimension | null>(null);

  // 투표를 한 번이라도 했거나, "투표 없이 전체 보기" 토글이 켜지면 정체 공개.
  const hasVoted = Object.keys(votes).length > 0;
  const revealed = showAll || hasVoted;

  useEffect(() => {
    getProviders().then(setProviders).catch(() => {});
    getPresets().then(setPresets).catch(() => {});
  }, []);

  const providerById = useMemo(
    () => Object.fromEntries(providers.map((p) => [p.id, p])),
    [providers],
  );

  const presetsForCategory = presets.filter((p) => p.category === category);

  async function run() {
    const text = prompt.trim();
    if (!text) {
      setError("프롬프트를 입력하세요.");
      return;
    }
    setRunning(true);
    setError(null);
    setResult(null);
    setShowAll(false);
    setVotes({});
    try {
      const c = await createComparison(text, category, getGuestKey());
      setResult(c);
      setOrder(shuffle(c.runs));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "비교 실행에 실패했습니다. 백엔드가 켜져 있는지 확인하세요.");
    } finally {
      setRunning(false);
    }
  }

  async function vote(dimension: Dimension, providerId: string) {
    if (!result) return;
    setVoting(dimension);
    try {
      await castVote(result.id, providerId, dimension, getGuestKey());
      setVotes((v) => ({ ...v, [dimension]: providerId }));
      // 투표하면 자동으로 정체 공개(revealed 가 hasVoted 로 파생됨).
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "투표에 실패했습니다.");
    } finally {
      setVoting(null);
    }
  }

  const overallWinner = votes["overall"];

  return (
    <div className="flex flex-col gap-6">
      {/* 입력 */}
      <section className="rounded-2xl border border-black/10 bg-card p-5 shadow-sm dark:border-white/10">
        <h1 className="mb-1 text-xl font-black">같은 프롬프트, 세 CLI 비교</h1>
        <p className="mb-4 text-sm text-muted">
          claude · gemini(agy) · codex 에 동시에 던지고, 정체를 가린 채 더 나은 응답에 투표하세요.
        </p>

        {/* 카테고리 */}
        <div className="mb-3 flex flex-wrap gap-2">
          {CATEGORIES.map((c) => (
            <button
              key={c.key}
              onClick={() => setCategory(c.key)}
              className={`min-h-11 rounded-full px-3 py-1.5 text-sm font-semibold transition ${
                category === c.key
                  ? "bg-brand-600 text-white"
                  : "bg-brand-100 text-brand-700 hover:bg-brand-200 dark:bg-white/10 dark:text-brand-200"
              }`}
            >
              {c.emoji} {c.label}
            </button>
          ))}
        </div>

        {/* 프리셋 */}
        {presetsForCategory.length > 0 && (
          <div className="mb-3 flex flex-wrap gap-2">
            {presetsForCategory.map((p) => (
              <button
                key={p.id}
                onClick={() => setPrompt(p.prompt)}
                title={p.description ?? ""}
                className="rounded-lg border border-brand-200 px-2.5 py-1 text-xs text-brand-700 hover:bg-brand-50 dark:border-white/10 dark:text-brand-200 dark:hover:bg-white/5"
              >
                ＋ {p.title}
              </button>
            ))}
          </div>
        )}

        <textarea
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          rows={4}
          placeholder="여기에 프롬프트를 입력하세요. 예) 너는 새침한 고양이 집사야. 오늘 날씨가 좋으니 산책을 권해줘."
          className="w-full resize-y rounded-xl border border-black/10 bg-background p-3 text-sm outline-none focus:border-brand-400 dark:border-white/10"
        />

        <div className="mt-3 flex items-center justify-between gap-3">
          <span className="text-xs text-muted">
            비교 대상: {providers.map((p) => p.displayName).join(" · ") || "로딩 중…"}
          </span>
          <button
            onClick={run}
            disabled={running}
            className="min-h-11 rounded-xl bg-brand-600 px-5 py-2.5 font-bold text-white transition hover:bg-brand-700 active:scale-[0.98] disabled:opacity-50"
          >
            {running ? "실행 중… (수 초 소요)" : "⚡ 비교 실행"}
          </button>
        </div>

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </section>

      {/* 실행 중 스켈레톤 */}
      {running && (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(providers.length ? providers : [0, 1, 2]).map((_, i) => (
            <div
              key={i}
              className="h-48 animate-pulse rounded-2xl border border-black/10 bg-card dark:border-white/10"
            />
          ))}
        </section>
      )}

      {/* 결과 */}
      {result && !running && (
        <section className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-black">결과 {revealed ? "" : "(블라인드)"}</h2>
            <div className="flex items-center gap-3">
              <a
                href={`/comparison/${result.id}`}
                className="text-sm font-semibold text-brand-600 hover:underline"
              >
                전체 데이터 보기 →
              </a>
              {/* 투표 없이 전체(정체+응답) 확인용 토글. 투표를 했으면 항상 공개라 비활성. */}
              <label
                className={`flex items-center gap-2 text-sm font-semibold ${hasVoted ? "opacity-50" : "cursor-pointer"}`}
              >
                <span>투표 없이 전체 보기</span>
                <button
                  type="button"
                  role="switch"
                  aria-checked={revealed}
                  disabled={hasVoted}
                  onClick={() => setShowAll((v) => !v)}
                  className={`relative h-6 w-11 rounded-full transition ${
                    revealed ? "bg-brand-600" : "bg-black/20 dark:bg-white/20"
                  }`}
                >
                  <span
                    className={`absolute top-0.5 left-0.5 h-5 w-5 rounded-full bg-white transition ${
                      revealed ? "translate-x-5" : ""
                    }`}
                  />
                </button>
              </label>
            </div>
          </div>
          {!hasVoted && (
            <p className="-mt-2 text-xs text-muted">
              투표는 선택이에요. 토글을 켜면 투표 없이 어떤 CLI 응답인지·전체 내용을 바로 볼 수 있어요.
            </p>
          )}

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {order.map((run, i) => (
              <ResultCard
                key={run.providerId}
                run={run}
                index={i}
                provider={providerById[run.providerId]}
                revealed={revealed}
                isWinner={overallWinner === run.providerId}
                canVote={run.status === "ok" && !votes["overall"]}
                onVote={() => vote("overall", run.providerId)}
              />
            ))}
          </div>

          {/* 차원별 투표 */}
          <div className="rounded-2xl border border-black/10 bg-card p-4 dark:border-white/10">
            <h3 className="mb-3 text-sm font-bold">차원별 투표 — 어느 응답이 더 나은가요?</h3>
            <div className="flex flex-col gap-3">
              {DIMENSIONS.map((d) => (
                <div key={d.key} className="flex flex-wrap items-center gap-2">
                  <span className="w-16 shrink-0 text-sm font-semibold">{d.label}</span>
                  {order
                    .filter((r) => r.status === "ok")
                    .map((r, i) => {
                      const chosen = votes[d.key] === r.providerId;
                      const p = providerById[r.providerId];
                      const cardLabel = revealed ? p?.displayName ?? r.providerId : `응답 ${String.fromCharCode(65 + i)}`;
                      return (
                        <button
                          key={r.providerId}
                          disabled={voting === d.key}
                          onClick={() => vote(d.key as Dimension, r.providerId)}
                          className={`min-h-10 rounded-lg px-3 py-1.5 text-sm font-semibold transition disabled:opacity-50 ${
                            chosen
                              ? "bg-brand-600 text-white"
                              : "bg-brand-50 text-brand-700 hover:bg-brand-100 dark:bg-white/5 dark:text-brand-200"
                          }`}
                        >
                          {chosen ? "✓ " : ""}
                          {cardLabel}
                        </button>
                      );
                    })}
                </div>
              ))}
            </div>
            <p className="mt-3 text-xs text-muted">투표하면 정체가 공개됩니다. 차원마다 한 번씩 투표할 수 있어요.</p>
          </div>
        </section>
      )}
    </div>
  );
}

/** Fisher–Yates 셔플(블라인드 위치 편향 방지). */
function shuffle<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}
