"use client";

import { useState } from "react";

/** 텍스트 복사 버튼. 클릭 시 잠깐 "복사됨" 표시. */
export default function CopyButton({ text, label = "복사" }: { text: string; label?: string }) {
  const [copied, setCopied] = useState(false);
  if (!text) return null;
  return (
    <button
      type="button"
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(text);
          setCopied(true);
          setTimeout(() => setCopied(false), 1500);
        } catch {
          /* clipboard 권한 없음 — 무시 */
        }
      }}
      className="rounded-lg px-2 py-1 text-xs font-semibold text-muted transition hover:bg-black/5 hover:text-foreground dark:hover:bg-white/10"
      aria-label="응답 복사"
    >
      {copied ? "✓ 복사됨" : `⧉ ${label}`}
    </button>
  );
}
