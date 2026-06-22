import {
  AuthProviders,
  AuthProvidersSchema,
  Comparison,
  ComparisonListSchema,
  ComparisonSchema,
  Dimension,
  Leaderboard,
  LeaderboardSchema,
  Me,
  MeSchema,
  Preset,
  PresetListSchema,
  Provider,
  ProviderListSchema,
  Vote,
  VoteSchema,
} from "./api-types";
import { getToken } from "./auth";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080";

/**
 * 백엔드 REST 호출 단일 경로. 컴포넌트는 fetch 직접 호출 금지 — 이 모듈만 사용.
 * 응답은 zod 로 런타임 검증한다.
 */
async function request<T>(
  path: string,
  schema: { parse: (v: unknown) => T },
  init?: RequestInit,
): Promise<T> {
  const headers: Record<string, string> = { "content-type": "application/json" };
  const token = getToken();
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { ...headers, ...(init?.headers as Record<string, string>) },
  });
  if (!res.ok) {
    let message = `요청 실패 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* ignore */
    }
    throw new ApiError(message, res.status);
  }
  if (res.status === 204) return undefined as T;
  const json = await res.json();
  return schema.parse(json);
}

export class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
    this.name = "ApiError";
  }
}

// ── Provider / Preset ──
export const getProviders = (): Promise<Provider[]> =>
  request("/api/providers", ProviderListSchema);

export const getPresets = (): Promise<Preset[]> =>
  request("/api/presets", PresetListSchema);

// ── Comparison ──
export const createComparison = (
  prompt: string,
  category: string,
  guestKey: string,
): Promise<Comparison> =>
  request("/api/comparisons", ComparisonSchema, {
    method: "POST",
    body: JSON.stringify({ prompt, category, guestKey }),
  });

export const getComparison = (id: number): Promise<Comparison> =>
  request(`/api/comparisons/${id}`, ComparisonSchema);

export const getRecentComparisons = (): Promise<Comparison[]> =>
  request("/api/comparisons", ComparisonListSchema);

export const getMyComparisons = (): Promise<Comparison[]> =>
  request("/api/me/comparisons", ComparisonListSchema);

// ── Vote ──
export const castVote = (
  comparisonId: number,
  winnerProviderId: string,
  dimension: Dimension,
  guestKey: string,
): Promise<Vote> =>
  request("/api/votes", VoteSchema, {
    method: "POST",
    body: JSON.stringify({ comparisonId, winnerProviderId, dimension, guestKey }),
  });

// ── Leaderboard ──
export const getLeaderboard = (): Promise<Leaderboard> =>
  request("/api/leaderboard", LeaderboardSchema);

// ── Auth ──
export const getAuthProviders = (): Promise<AuthProviders> =>
  request("/api/auth/providers", AuthProvidersSchema);

export const getMe = (): Promise<Me> => request("/api/me", MeSchema);

export const oauthUrl = (provider: string): string =>
  `${API_BASE}/oauth2/authorization/${provider}`;
