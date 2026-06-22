"use client";

import { useEffect, useState } from "react";

const KEY = "mkc:theme:v1";

/** 라이트/다크 테마 토글. 선택을 localStorage 에 저장(레이아웃의 bootScript 가 페인트 전 적용). */
export default function ThemeToggle() {
  const [dark, setDark] = useState(false);

  useEffect(() => {
    const current = document.documentElement.getAttribute("data-theme");
    setDark(current === "dark");
  }, []);

  function toggle() {
    const next = dark ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", next);
    localStorage.setItem(KEY, JSON.stringify(next));
    setDark(!dark);
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={dark ? "라이트 모드로" : "다크 모드로"}
      className="flex h-9 w-9 items-center justify-center rounded-full text-lg transition hover:bg-brand-100 dark:hover:bg-white/10"
    >
      {dark ? "☀️" : "🌙"}
    </button>
  );
}
