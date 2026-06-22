"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/", label: "비교", icon: "⚖️" },
  { href: "/leaderboard", label: "리더보드", icon: "🏆" },
  { href: "/history", label: "기록", icon: "🕑" },
  { href: "/account", label: "계정", icon: "👤" },
];

/** 모바일 하단 탭 네비게이션(데스크톱에선 숨김 — 상단 네비 사용). */
export default function BottomNav() {
  const pathname = usePathname();
  const isActive = (href: string) => (href === "/" ? pathname === "/" : pathname.startsWith(href));

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-30 border-t border-black/5 bg-card/95 backdrop-blur sm:hidden dark:border-white/10"
      style={{ paddingBottom: "env(safe-area-inset-bottom)" }}
      aria-label="하단 내비게이션"
    >
      <div className="mx-auto flex max-w-5xl">
        {TABS.map((t) => {
          const active = isActive(t.href);
          return (
            <Link
              key={t.href}
              href={t.href}
              aria-current={active ? "page" : undefined}
              className={`flex min-h-14 flex-1 flex-col items-center justify-center gap-0.5 text-xs font-semibold transition ${
                active ? "text-brand-600" : "text-muted"
              }`}
            >
              <span className="text-lg" aria-hidden>
                {t.icon}
              </span>
              {t.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
