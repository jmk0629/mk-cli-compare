"use client";

import { useState } from "react";

/** 공유 버튼 — 모바일은 네이티브 공유 시트(Web Share API), 데스크톱은 링크 클립보드 복사. */
export default function ShareButton({
  path,
  title = "CLI 비교 결과",
  text = "claude vs gemini vs codex — 같은 프롬프트 비교 결과를 확인해보세요.",
}: {
  path: string;
  title?: string;
  text?: string;
}) {
  const [copied, setCopied] = useState(false);

  async function share() {
    const url = typeof window !== "undefined" ? `${window.location.origin}${path}` : path;
    const nav = typeof navigator !== "undefined" ? (navigator as Navigator & { share?: (d: ShareData) => Promise<void> }) : undefined;
    if (nav?.share) {
      try {
        await nav.share({ title, text, url });
        return;
      } catch {
        /* 사용자가 취소 → 폴백 */
      }
    }
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard 불가 — 무시 */
    }
  }

  return (
    <button
      type="button"
      onClick={share}
      className="min-h-9 rounded-full bg-brand-100 px-3 py-1.5 text-sm font-semibold text-brand-700 transition hover:bg-brand-200 dark:bg-white/10 dark:text-brand-200"
    >
      {copied ? "✓ 링크 복사됨" : "🔗 공유"}
    </button>
  );
}
