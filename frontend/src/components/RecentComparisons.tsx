"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getRecentComparisons } from "@/lib/api";
import { Comparison, CATEGORIES } from "@/lib/api-types";
import { providerEmoji } from "@/lib/providers";

/** 홈 하단 최근 공개 비교 피드 — 재방문/탐색 유도. 클릭 시 상세로. */
export default function RecentComparisons() {
  const [items, setItems] = useState<Comparison[] | null>(null);

  useEffect(() => {
    getRecentComparisons()
      .then((list) => setItems(list.filter((c) => c.status !== "pending").slice(0, 8)))
      .catch(() => setItems([]));
  }, []);

  if (!items || items.length === 0) return null;

  return (
    <section className="mt-8 flex flex-col gap-3">
      <h2 className="text-base font-black">최근 비교</h2>
      <div className="grid gap-3 sm:grid-cols-2">
        {items.map((c) => {
          const cat = CATEGORIES.find((x) => x.key === c.category);
          return (
            <Link
              key={c.id}
              href={`/comparison/${c.id}`}
              className="rounded-xl border border-black/10 bg-card p-3 transition hover:border-brand-400 hover:shadow-md dark:border-white/10"
            >
              <div className="mb-1 flex items-center gap-2">
                <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[11px] font-semibold text-brand-700 dark:bg-white/10 dark:text-brand-200">
                  {cat ? `${cat.emoji} ${cat.label}` : c.category}
                </span>
                <span className="ml-auto text-[11px] text-muted">{c.createdAt.slice(5, 16).replace("T", " ")}</span>
              </div>
              <p className="line-clamp-2 text-sm font-medium">{c.prompt}</p>
              <div className="mt-1.5 flex gap-2 text-[11px] text-muted">
                {c.runs.map((r) => (
                  <span key={r.providerId}>
                    {providerEmoji(r.providerId)}
                    {r.status === "ok" && r.latencyMs != null ? ` ${(r.latencyMs / 1000).toFixed(1)}s` : " –"}
                  </span>
                ))}
              </div>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
