'use strict';
const crypto = require('crypto');

// Sessions remain server-side and expire after eight hours or a server restart.
module.exports = function installAdminAuth(app) {
  const sessions = new Map();
  const attempts = new Map();
  const ttl = 8 * 60 * 60 * 1000;
  const windowMs = 15 * 60 * 1000;
  const cookieName = 'cclive_admin_session';
  const digest = value => crypto.createHash('sha256').update(value).digest();
  const secure = process.env.ADMIN_COOKIE_SECURE !== 'false';
  const cookieOptions = { httpOnly: true, secure, sameSite: 'strict', path: '/admin' };

  function clean() {
    const now = Date.now();
    for (const [key, session] of sessions) if (session.expires <= now) sessions.delete(key);
    for (const [key, attempt] of attempts) if (attempt.expires <= now) attempts.delete(key);
  }
  const timer = setInterval(clean, 60000);
  timer.unref();

  function cookieToken(req) {
    const match = (req.headers.cookie || '').match(/(?:^|;\s*)cclive_admin_session=([a-f0-9]{64})(?:;|$)/);
    return match ? match[1] : '';
  }
  function tokenHash(token) { return digest(token).toString('hex'); }
  function revoke(req) {
    const token = cookieToken(req);
    if (token) sessions.delete(tokenHash(token));
  }
  function browserRequest(req) {
    // Custom header rejects cross-site form submissions; CORS never grants credentials.
    return req.headers['x-admin-request'] === '1' &&
      (!req.headers['sec-fetch-site'] || req.headers['sec-fetch-site'] === 'same-origin');
  }
  function sessionValid(req) {
    const token = cookieToken(req);
    if (!token || !process.env.ADMIN_KEY) return false;
    const key = tokenHash(token);
    const session = sessions.get(key);
    if (!session) return false;
    if (session.expires <= Date.now() || session.keyDigest !== tokenHash(process.env.ADMIN_KEY)) {
      sessions.delete(key);
      return false;
    }
    return true;
  }
  app.use('/admin', (req, res, next) => {
    res.set('Cache-Control', 'no-store');
    next();
  });
  app.post('/admin/login', (req, res) => {
    if (!browserRequest(req)) return res.status(403).json({ error: 'Unzulässige Anfrage' });
    if (!process.env.ADMIN_KEY) return res.status(503).json({ error: 'Admin-Anmeldung nicht konfiguriert' });
    clean();
    const ip = req.ip || req.socket.remoteAddress || 'unknown';
    const attempt = attempts.get(ip);
    if (attempt && attempt.count >= 10) {
      res.set('Retry-After', String(Math.ceil((attempt.expires - Date.now()) / 1000)));
      return res.status(429).json({ error: 'Zu viele Versuche. Bitte später erneut versuchen.' });
    }
    const password = req.body && req.body.password;
    if (typeof password !== 'string' || !password || password.length > 1024 ||
        !crypto.timingSafeEqual(digest(password), digest(process.env.ADMIN_KEY))) {
      if (!attempt && attempts.size >= 10000) return res.status(429).json({ error: 'Bitte später erneut versuchen.' });
      attempts.set(ip, { count: (attempt ? attempt.count : 0) + 1, expires: attempt ? attempt.expires : Date.now() + windowMs });
      return res.status(401).json({ error: 'Ungültiger Admin-Key' });
    }
    attempts.delete(ip);
    if (sessions.size >= 10000) return res.status(503).json({ error: 'Bitte später erneut versuchen.' });
    revoke(req);
    const token = crypto.randomBytes(32).toString('hex');
    sessions.set(tokenHash(token), { expires: Date.now() + ttl, keyDigest: tokenHash(process.env.ADMIN_KEY) });
    res.cookie(cookieName, token, { ...cookieOptions, maxAge: ttl });
    res.json({ authenticated: true });
  });
  app.get('/admin/session', (req, res) => {
    if (!browserRequest(req)) return res.status(403).json({ authenticated: false });
    res.json({ authenticated: sessionValid(req) });
  });
  app.post('/admin/logout', (req, res) => {
    if (!browserRequest(req)) return res.status(403).json({ error: 'Unzulässige Anfrage' });
    revoke(req);
    res.clearCookie(cookieName, cookieOptions);
    res.json({ authenticated: false });
  });
  return function checkAdmin(req, res, next) {
    if (browserRequest(req) && sessionValid(req)) return next();
    // Preserve existing non-browser API clients. Browser code never contains this key.
    const supplied = req.headers['x-admin-key'];
    if (!req.headers.origin && !req.headers['sec-fetch-site'] && process.env.ADMIN_KEY &&
        typeof supplied === 'string' && supplied.length <= 1024 &&
        crypto.timingSafeEqual(digest(supplied), digest(process.env.ADMIN_KEY))) return next();
    return res.status(401).json({ error: 'Admin-Anmeldung erforderlich' });
  };
};
