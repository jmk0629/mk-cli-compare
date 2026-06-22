"use client";

import { useEffect } from "react";

/** 서비스워커 등록(프로덕션). 오프라인 셸 + 설치형 PWA. */
export default function PwaRegister() {
  useEffect(() => {
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator)) return;
    if (process.env.NODE_ENV !== "production") return;
    navigator.serviceWorker.register("/sw.js").catch(() => {});
  }, []);
  return null;
}
