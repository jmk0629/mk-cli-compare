"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  CATEGORIES,
  Comparison,
  DIMENSIONS,
  Dimension,
  Preset,
  Provider,
} from "@/lib/api-types";
import { castVote, createComparison, getComparison, getPresets, getProviders, ApiError } from "@/lib/api";
import { getGuestKey } from "@/lib/auth";
import ResultCard from "./ResultCard";

/** 비교 메인 플로우: 프롬프트 입력 → 3 CLI 실행 → 블라인드 카드 → 차원별 투표 → 정체 공개. */
export default function CompareView() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [presets, setPresets] = useState<Preset[]>([]);
  const [prompt, setPrompt] = useState("");
  const [category, setCategory] = useState<string>("general");
  const [selectedModels, setSelectedModels] = useState<Record<string, string>>({}); // providerId → model arg
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<Comparison | null>(null);

  // 블라인드: 결과 받으면 카드 순서(providerId 시퀀스)를 한 번 섞는다(위치 편향 방지). 폴링해도 순서 고정.
  const [orderIds, setOrderIds] = useState<string[]>([]);
  const [showAll, setShowAll] = useState(false); // 투표 없이 전체 보기 토글
  const [votes, setVotes] = useState<Record<string, string>>({}); // dimension → providerId
  const [voting, setVoting] = useState<Dimension | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 투표를 한 번이라도 했거나, "투표 없이 전체 보기" 토글이 켜지면 정체 공개.
  const hasVoted = Object.keys(votes).length > 0;
  const revealed = showAll || hasVoted;

  // orderIds(고정 순서) → 최신 run 데이터로 매핑. 폴링으로 run 이 pending→ok 되어도 위치 유지.
  const orderedRuns = orderIds
    .map((id) => result?.runs.find((r) => r.providerId === id))
    .filter((r): r is NonNullable<typeof r> => r != null);
  const inProgress = running || result?.status === "pending";

  useEffect(() => {
    getProviders()
      .then((ps) => {
        setProviders(ps);
        // provider 별 기본 모델 선택값 초기화(is_default, 없으면 첫 모델).
        const defaults: Record<string, string> = {};
        ps.forEach((p) => {
          const def = p.models.find((m) => m.isDefault) ?? p.models[0];
          if (def) defaults[p.id] = def.arg;
        });
        setSelectedModels(defaults);
      })
      .catch(() => {});
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
    if (pollRef.current) clearInterval(pollRef.current);
    try {
      // 비교 생성 → pending run 3개 즉시 반환. 카드를 바로 그리고, 폴링으로 점진적으로 채운다.
      const c = await createComparison(text, category, getGuestKey(), selectedModels);
      setResult(c);
      setOrderIds(shuffle(c.runs.map((r) => r.providerId)));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "비교 실행에 실패했습니다. 백엔드가 켜져 있는지 확인하세요.");
    } finally {
      setRunning(false);
    }
  }

  // 결과가 pending 이면 1.5s 마다 폴링해 카드를 점진적으로 갱신. done/error 면 중단.
  useEffect(() => {
    if (!result || result.status !== "pending") return;
    const id = result.id;
    pollRef.current = setInterval(async () => {
      try {
        const fresh = await getComparison(id);
        setResult(fresh);
        if (fresh.status !== "pending" && pollRef.current) {
          clearInterval(pollRef.current);
          pollRef.current = null;
        }
      } catch {
        /* 일시 실패는 다음 폴링에서 재시도 */
      }
    }, 1500);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [result?.id, result?.status]);

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

        {/* provider 별 모델 선택 */}
        {providers.some((p) => p.models.length > 0) && (
          <div className="mt-3 grid gap-2 sm:grid-cols-3">
            {providers.map((p) =>
              p.models.length > 0 ? (
                <label key={p.id} className="flex flex-col gap-1 text-xs">
                  <span className="font-semibold" style={{ color: p.color }}>
                    {p.displayName} 모델
                  </span>
                  <select
                    value={selectedModels[p.id] ?? ""}
                    onChange={(e) => setSelectedModels((m) => ({ ...m, [p.id]: e.target.value }))}
                    className="min-h-10 rounded-lg border border-black/10 bg-background px-2 py-2 text-sm outline-none focus:border-brand-400 dark:border-white/10"
                  >
                    {p.models.map((m) => (
                      <option key={m.arg} value={m.arg}>
                        {m.label}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null,
            )}
          </div>
        )}

        <div className="mt-3 flex items-center justify-between gap-3">
          <span className="text-xs text-muted">
            비교 대상: {providers.map((p) => p.displayName).join(" · ") || "로딩 중…"}
          </span>
          <button
            onClick={run}
            disabled={inProgress}
            className="min-h-11 rounded-xl bg-brand-600 px-5 py-2.5 font-bold text-white transition hover:bg-brand-700 active:scale-[0.98] disabled:opacity-50"
          >
            {inProgress ? "실행 중…" : "⚡ 비교 실행"}
          </button>
        </div>

        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
      </section>

      {/* 최초 생성 대기(매우 짧음) 스켈레톤 */}
      {running && !result && (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(providers.length ? providers : [0, 1, 2]).map((_, i) => (
            <div
              key={i}
              className="h-48 animate-pulse rounded-2xl border border-black/10 bg-card dark:border-white/10"
            />
          ))}
        </section>
      )}

      {/* 결과 (pending 부터 점진적으로 채워짐) */}
      {result && (
        <section className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 text-lg font-black">
              결과 {revealed ? "" : "(블라인드)"}
              {inProgress && (
                <span className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600">
                  <span className="h-3 w-3 animate-spin rounded-full border-2 border-brand-300 border-t-brand-600" />
                  실행 중
                </span>
              )}
            </h2>
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
            {orderedRuns.map((run, i) => (
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
                  {orderedRuns
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
