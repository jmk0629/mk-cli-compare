"use client";

import { useEffect, useState } from "react";
import { getAuthProviders, getMe, oauthUrl } from "@/lib/api";
import { Me } from "@/lib/api-types";
import { clearToken, isLoggedIn } from "@/lib/auth";

const PROVIDER_LABEL: Record<string, string> = { google: "Google", kakao: "Kakao", naver: "Naver" };

export default function AccountPage() {
  const [me, setMe] = useState<Me | null>(null);
  const [providers, setProviders] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAuthProviders().then((p) => setProviders(p.providers)).catch(() => {});
    if (isLoggedIn()) {
      getMe()
        .then(setMe)
        .catch(() => clearToken())
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  function logout() {
    clearToken();
    setMe(null);
  }

  if (loading) return <p className="text-sm text-muted">불러오는 중…</p>;

  return (
    <div className="mx-auto flex max-w-md flex-col gap-5">
      <h1 className="text-xl font-black">계정</h1>

      {me ? (
        <div className="rounded-2xl border border-black/10 bg-card p-5 dark:border-white/10">
          <p className="text-sm text-muted">로그인됨 ({me.provider})</p>
          <p className="mt-1 text-lg font-bold">{me.nickname ?? me.email ?? `사용자 #${me.id}`}</p>
          {me.email && <p className="text-sm text-muted">{me.email}</p>}
          <button
            onClick={logout}
            className="mt-4 min-h-11 w-full rounded-xl border border-black/10 py-2.5 text-sm font-semibold hover:bg-black/5 dark:border-white/10 dark:hover:bg-white/10"
          >
            로그아웃
          </button>
        </div>
      ) : (
        <div className="rounded-2xl border border-black/10 bg-card p-5 dark:border-white/10">
          <p className="mb-1 text-sm font-medium">로그인 (선택)</p>
          <p className="mb-4 text-sm text-muted">
            로그인 없이도 비교·투표가 모두 동작합니다. 로그인하면 내 비교 기록을 모아볼 수 있어요.
          </p>
          {providers.length === 0 ? (
            <p className="text-sm text-muted">현재 활성화된 소셜 로그인이 없습니다(.env 에 OAuth 키 설정 시 노출).</p>
          ) : (
            <div className="flex flex-col gap-2">
              {providers.map((p) => (
                <a
                  key={p}
                  href={oauthUrl(p)}
                  className="min-h-11 rounded-xl bg-brand-600 px-4 py-2.5 text-center text-sm font-bold text-white hover:bg-brand-700"
                >
                  {PROVIDER_LABEL[p] ?? p} 로 로그인
                </a>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
