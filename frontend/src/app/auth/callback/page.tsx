"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { setToken } from "@/lib/auth";

/** OAuth 성공 시 백엔드가 `/auth/callback#token=<JWT>` (또는 `#error=...`) 로 redirect. 프래그먼트에서 토큰 추출. */
export default function AuthCallbackPage() {
  const router = useRouter();
  const [message, setMessage] = useState("로그인 처리 중…");

  useEffect(() => {
    const hash = window.location.hash.replace(/^#/, "");
    const params = new URLSearchParams(hash);
    const token = params.get("token");
    const err = params.get("error");
    if (token) {
      setToken(token);
      setMessage("로그인 성공! 이동 중…");
      setTimeout(() => router.replace("/account"), 600);
    } else {
      setMessage(`로그인 실패: ${err ?? "알 수 없는 오류"}`);
      setTimeout(() => router.replace("/account"), 1500);
    }
  }, [router]);

  return (
    <div className="flex min-h-[40dvh] flex-col items-center justify-center gap-3 text-center">
      <span className="text-3xl">⚖️</span>
      <p className="text-sm font-medium">{message}</p>
    </div>
  );
}
