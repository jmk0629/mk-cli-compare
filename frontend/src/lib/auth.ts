"use client";

/** JWT 토큰 보관(localStorage) + guest 키 발급. 컴포넌트는 api.ts 통해서만 사용 권장. */

const TOKEN_KEY = "mkc:token:v1";
const GUEST_KEY = "mkc:guest:v1";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

/** 비로그인 사용자 식별 키(브라우저 1개당 1개). 비교/투표 소유 추적용. */
export function getGuestKey(): string {
  if (typeof window === "undefined") return "ssr-guest";
  let key = localStorage.getItem(GUEST_KEY);
  if (!key) {
    key = "g_" + Math.random().toString(36).slice(2) + Date.now().toString(36);
    localStorage.setItem(GUEST_KEY, key);
  }
  return key;
}
