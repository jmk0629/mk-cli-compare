"use client";

import { Provider, Run } from "@/lib/api-types";
import { providerEmoji, slotLabel } from "@/lib/providers";

interface Props {
  run: Run;
  index: number;
  provider?: Provider;
  revealed: boolean;
  isWinner: boolean;
  canVote: boolean;
  onVote: () => void;
}

/** 단일 provider 결과 카드. revealed=false 면 정체(이름/색)를 가리고 슬롯 라벨(A/B/C)만 노출(블라인드). */
export default function ResultCard({ run, index, provider, revealed, isWinner, canVote, onVote }: Props) {
  const label = revealed ? (provider?.displayName ?? run.providerId) : `응답 ${slotLabel(index)}`;
  const accent = revealed ? provider?.color ?? "#6366f1" : "#6b6880";
  const ok = run.status === "ok";

  return (
    <div
      className={`flex flex-col rounded-2xl border bg-card p-4 shadow-sm transition ${
        isWinner ? "border-brand-500 ring-2 ring-brand-300" : "border-black/10 dark:border-white/10"
      }`}
      style={isWinner ? { boxShadow: `0 0 0 1px ${accent}` } : undefined}
    >
      <div className="mb-2 flex items-center justify-between gap-2">
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center gap-2 font-bold" style={{ color: accent }}>
            {revealed && <span aria-hidden>{providerEmoji(run.providerId)}</span>}
            <span>{label}</span>
            {isWinner && <span className="text-xs font-extrabold text-brand-600">👑 승자</span>}
          </div>
          {revealed && run.model && <span className="text-xs text-muted">{run.model}</span>}
        </div>
        <StatusBadge status={run.status} />
      </div>

      <div className="min-h-24 flex-1 whitespace-pre-wrap break-words rounded-xl bg-black/[0.03] p-3 text-sm leading-relaxed dark:bg-white/[0.04]">
        {ok ? (
          run.responseText
        ) : (
          <span className="text-muted">
            {run.status === "timeout" ? "⏱ 시간 초과" : "⚠️ 응답 실패"}
            {run.errorText ? `\n${run.errorText.slice(0, 300)}` : ""}
          </span>
        )}
      </div>

      <div className="mt-3 flex items-center justify-between text-xs text-muted">
        <span>{run.latencyMs != null ? `${(run.latencyMs / 1000).toFixed(1)}s` : "—"}</span>
        <span>{run.charCount != null ? `${run.charCount}자` : ""}</span>
      </div>

      {canVote && (
        <button
          onClick={onVote}
          className="mt-3 min-h-11 w-full rounded-xl bg-brand-600 px-3 py-2 text-sm font-bold text-white transition hover:bg-brand-700 active:scale-[0.98]"
        >
          이 응답에 투표
        </button>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; cls: string }> = {
    ok: { label: "완료", cls: "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300" },
    error: { label: "실패", cls: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300" },
    timeout: { label: "시간초과", cls: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300" },
    pending: { label: "실행중", cls: "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300" },
  };
  const s = map[status] ?? map.pending;
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${s.cls}`}>{s.label}</span>;
}
