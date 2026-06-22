import type { Metadata, Viewport } from "next";
import Link from "next/link";
import "./globals.css";
import PwaRegister from "@/components/PwaRegister";
import BottomNav from "@/components/BottomNav";
import ThemeToggle from "@/components/ThemeToggle";

export const metadata: Metadata = {
  title: "CLI 비교 — claude vs gemini vs codex",
  description: "같은 프롬프트를 구독형 코딩 CLI 3종에 던져 응답·속도·품질을 나란히 비교하고 블라인드 투표로 승자를 가립니다.",
  manifest: "/manifest.webmanifest",
  appleWebApp: { capable: true, statusBarStyle: "default", title: "CLI 비교" },
};

export const viewport: Viewport = {
  themeColor: "#6366f1",
  width: "device-width",
  initialScale: 1,
};

/** SSR 깜빡임 없이 다크 테마 적용(페인트 전 실행). */
const bootScript = `(function(){try{
  var t=localStorage.getItem('mkc:theme:v1');if(t)document.documentElement.setAttribute('data-theme',JSON.parse(t));
}catch(e){}})();`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: bootScript }} />
      </head>
      <body className="min-h-dvh">
        <PwaRegister />
        <header className="sticky top-0 z-20 border-b border-black/5 bg-card/80 backdrop-blur dark:border-white/10">
          <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
            <Link href="/" className="flex shrink-0 items-center gap-2 text-lg font-black text-brand-600">
              <span className="text-2xl">⚖️</span>
              <span>CLI 비교</span>
            </Link>
            <div className="flex items-center gap-1">
              {/* 데스크톱 상단 네비(모바일은 하단 탭) */}
              <nav className="hidden items-center gap-0.5 text-sm font-semibold sm:flex">
                <Link href="/" className="rounded-full px-3 py-2 hover:bg-brand-100 dark:hover:bg-white/10">
                  비교
                </Link>
                <Link href="/leaderboard" className="rounded-full px-3 py-2 hover:bg-brand-100 dark:hover:bg-white/10">
                  리더보드
                </Link>
                <Link href="/history" className="rounded-full px-3 py-2 hover:bg-brand-100 dark:hover:bg-white/10">
                  기록
                </Link>
                <Link href="/account" className="rounded-full px-3 py-2 hover:bg-brand-100 dark:hover:bg-white/10">
                  계정
                </Link>
              </nav>
              <ThemeToggle />
            </div>
          </div>
        </header>
        <main className="mx-auto w-full max-w-5xl px-4 py-6 pb-24 sm:pb-6">{children}</main>
        <footer className="mx-auto max-w-5xl px-4 pb-24 pt-4 text-center text-xs text-muted sm:pb-8">
          구독형 CLI(claude · agy · codex)를 로컬에서 실행해 비교합니다. API 키 불필요.
        </footer>
        <BottomNav />
      </body>
    </html>
  );
}
