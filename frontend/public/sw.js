// 최소 서비스워커 — 앱 셸 캐시(오프라인 진입) + 네트워크 우선.
// API 응답은 캐시하지 않는다(항상 최신 비교/리더보드).
const CACHE = "mkc-shell-v1";
const SHELL = ["/", "/leaderboard", "/history", "/account"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).catch(() => {}));
  self.skipWaiting();
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))),
  );
  self.clients.claim();
});

self.addEventListener("fetch", (e) => {
  const { request } = e;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  // API/백엔드 호출은 항상 네트워크(캐시 안 함).
  if (url.pathname.startsWith("/api") || url.port === "8080") return;
  // 네비게이션: 네트워크 우선, 실패 시 캐시된 셸.
  if (request.mode === "navigate") {
    e.respondWith(
      fetch(request)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(request, copy)).catch(() => {});
          return res;
        })
        .catch(() => caches.match(request).then((r) => r || caches.match("/"))),
    );
    return;
  }
  // 정적 자산: 캐시 우선, 없으면 네트워크.
  e.respondWith(caches.match(request).then((r) => r || fetch(request)));
});
