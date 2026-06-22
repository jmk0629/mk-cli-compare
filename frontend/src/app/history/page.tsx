"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getMyComparisons, getRecentComparisons } from "@/lib/api";
import { Comparison } from "@/lib/api-types";
import { isLoggedIn } from "@/lib/auth";
import { providerEmoji } from "@/lib/providers";

export default function HistoryPage() {
  const [items, setItems] = useState<Comparison[] | null>(null);
  const [mine, setMine] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loggedIn = isLoggedIn();
    setMine(loggedIn);
    const load = loggedIn ? getMyComparisons() : getRecentComparisons();
    load.then(setItems).catch(() => setError("기록을 불러오지 못했습니다."));
  }, []);

  return (
    <div className="flex flex-col gap-5">
      <div>
        <h1 className="text-xl font-black">{mine ? "내 비교 기록" : "최근 비교"}</h1>
        <p className="text-sm text-muted">
          {mine ? "로그인한 계정으로 실행한 비교입니다." : "최근 공개 비교 피드입니다. 로그인하면 내 기록만 볼 수 있어요."}
        </p>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}
      {!items && !error && <p className="text-sm text-muted">불러오는 중…</p>}
      {items?.length === 0 && <p className="text-sm text-muted">아직 비교 기록이 없습니다.</p>}

      <div className="flex flex-col gap-3">
        {items?.map((c) => (
          <div key={c.id} className="rounded-2xl border border-black/10 bg-card p-4 dark:border-white/10">
            <div className="flex items-center justify-between gap-2">
              <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700 dark:bg-white/10 dark:text-brand-200">
                {c.category}
              </span>
              <span className="text-xs text-muted">{c.createdAt.slice(0, 16).replace("T", " ")}</span>
            </div>
            <p className="mt-2 line-clamp-2 text-sm font-medium">{c.prompt}</p>
            <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted">
              {c.runs.map((r) => (
                <span key={r.providerId} className="flex items-center gap-1">
                  {providerEmoji(r.providerId)} {r.providerId}
                  {r.status === "ok" ? (
                    <span className="text-green-600">{r.latencyMs != null ? ` ${(r.latencyMs / 1000).toFixed(1)}s` : ""}</span>
                  ) : (
                    <span className="text-red-500"> 실패</span>
                  )}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>

      <Link href="/" className="text-sm font-semibold text-brand-600 hover:underline">
        ← 새 비교 실행하기
      </Link>
    </div>
  );
}
