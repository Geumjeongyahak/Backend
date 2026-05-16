const CACHE_NAME = 'sonmoum-admin-static-v1';
const STATIC_ASSETS = [
  '/site.webmanifest',
  '/icons/geumjeongyahak-icon.svg',
  '/icons/android-chrome-192x192.png',
  '/icons/android-chrome-512x512.png',
  '/icons/apple-touch-icon.png',
  '/icons/favicon-32x32.png',
  '/icons/favicon-16x16.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(STATIC_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys
        .filter((key) => key !== CACHE_NAME)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method !== 'GET' || url.origin !== self.location.origin) {
    return;
  }

  if (!STATIC_ASSETS.includes(url.pathname)) {
    return;
  }

  event.respondWith(
    caches.match(request)
      .then((cached) => cached || fetch(request))
  );
});

const resolveClickUrl = (data = {}) => {
  if (data.requestType === 'PURCHASE' && data.requestId) {
    return `/admin/request/purchase/purchase-requests/${data.requestId}`;
  }
  return '/admin';
};

self.addEventListener('push', (event) => {
  const payload = event.data ? event.data.json() : {};
  const notification = payload.notification || {};
  const data = payload.data || {};
  const title = notification.title || payload.title || '금정열린배움터';
  const options = {
    body: notification.body || payload.body || '',
    icon: '/icons/android-chrome-192x192.png',
    badge: '/icons/favicon-32x32.png',
    data: {
      ...data,
      url: resolveClickUrl(data)
    }
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = new URL(event.notification.data?.url || '/admin', self.location.origin).href;

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clients) => {
        const existing = clients.find((client) => client.url === targetUrl);
        if (existing) {
          return existing.focus();
        }
        return self.clients.openWindow(targetUrl);
      })
  );
});
