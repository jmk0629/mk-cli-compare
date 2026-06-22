import { z } from "zod";

/** 백엔드 응답 zod 스키마 — api.ts 가 런타임 검증에 사용. */

export const ProviderSchema = z.object({
  id: z.string(),
  displayName: z.string(),
  vendor: z.string(),
  runnerKind: z.string(),
  model: z.string().nullable(),
  color: z.string(),
  icon: z.string().nullable(),
});
export type Provider = z.infer<typeof ProviderSchema>;
export const ProviderListSchema = z.array(ProviderSchema);

export const PresetSchema = z.object({
  id: z.string(),
  category: z.string(),
  title: z.string(),
  prompt: z.string(),
  description: z.string().nullable(),
});
export type Preset = z.infer<typeof PresetSchema>;
export const PresetListSchema = z.array(PresetSchema);

export const RunSchema = z.object({
  providerId: z.string(),
  status: z.string(), // ok | error | timeout | pending
  responseText: z.string().nullable(),
  errorText: z.string().nullable(),
  exitCode: z.number().nullable(),
  latencyMs: z.number().nullable(),
  charCount: z.number().nullable(),
});
export type Run = z.infer<typeof RunSchema>;

export const ComparisonSchema = z.object({
  id: z.number(),
  category: z.string(),
  prompt: z.string(),
  status: z.string(),
  createdAt: z.string(),
  completedAt: z.string().nullable(),
  runs: z.array(RunSchema),
});
export type Comparison = z.infer<typeof ComparisonSchema>;
export const ComparisonListSchema = z.array(ComparisonSchema);

export const VoteSchema = z.object({
  id: z.number(),
  comparisonId: z.number(),
  winnerProviderId: z.string(),
  dimension: z.string(),
});
export type Vote = z.infer<typeof VoteSchema>;

export const RankingSchema = z.object({
  providerId: z.string(),
  displayName: z.string(),
  color: z.string(),
  vendor: z.string(),
  totalWins: z.number(),
  winsByDimension: z.record(z.string(), z.number()),
  totalRuns: z.number(),
  okRuns: z.number(),
  okRate: z.number(),
  avgLatencyMs: z.number().nullable(),
});
export type Ranking = z.infer<typeof RankingSchema>;

export const LeaderboardSchema = z.object({
  rankings: z.array(RankingSchema),
});
export type Leaderboard = z.infer<typeof LeaderboardSchema>;

export const MeSchema = z.object({
  id: z.number(),
  provider: z.string(),
  email: z.string().nullable(),
  nickname: z.string().nullable(),
  profileImage: z.string().nullable(),
  createdAt: z.string(),
});
export type Me = z.infer<typeof MeSchema>;

export const AuthProvidersSchema = z.object({
  providers: z.array(z.string()),
});
export type AuthProviders = z.infer<typeof AuthProvidersSchema>;

/** 투표 차원 — UI 라벨. */
export const DIMENSIONS = [
  { key: "overall", label: "종합" },
  { key: "quality", label: "품질" },
  { key: "speed", label: "속도" },
  { key: "persona", label: "캐릭터성" },
  { key: "creativity", label: "창의성" },
] as const;
export type Dimension = (typeof DIMENSIONS)[number]["key"];

/** 카테고리 — UI 라벨. */
export const CATEGORIES = [
  { key: "character", label: "캐릭터챗", emoji: "🎭" },
  { key: "coding", label: "코딩", emoji: "💻" },
  { key: "summary", label: "요약", emoji: "📝" },
  { key: "reasoning", label: "추론", emoji: "🧩" },
  { key: "general", label: "일반", emoji: "💬" },
] as const;
