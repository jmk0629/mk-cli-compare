/** provider 표시 메타(색/이모지) — 백엔드 카탈로그에 없을 때의 프론트 폴백.
 *  백엔드 cli_provider.color 가 우선이며, 이 맵은 아이콘/이모지 보강용. */
export const PROVIDER_META: Record<string, { emoji: string; color: string }> = {
  claude: { emoji: "🟠", color: "#d97757" },
  gemini: { emoji: "🔵", color: "#4285f4" },
  codex: { emoji: "🟢", color: "#10a37f" },
};

export function providerEmoji(id: string): string {
  return PROVIDER_META[id]?.emoji ?? "⚪";
}

/** 블라인드 슬롯 라벨(A/B/C…). 투표 전에는 정체 대신 이 라벨만 노출. */
export function slotLabel(index: number): string {
  return String.fromCharCode(65 + index); // 0→A, 1→B, 2→C
}
