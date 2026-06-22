import type { MetadataRoute } from "next";

/** PWA 매니페스트 — 모바일 홈 화면 설치/스탠드얼론 실행 지원. */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "CLI 비교 — claude vs gemini vs codex",
    short_name: "CLI 비교",
    description: "같은 프롬프트를 구독형 코딩 CLI 3종에 던져 응답·속도·품질을 비교하고 투표합니다.",
    start_url: "/",
    display: "standalone",
    background_color: "#f5f5fb",
    theme_color: "#6366f1",
    lang: "ko",
    orientation: "portrait",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
    ],
  };
}
