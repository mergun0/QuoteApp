const crypto = require("crypto");

const SESSION_COOKIE = "qa_admin_session";
const CSRF_COOKIE = "qa_admin_csrf";
const sessions = new Map();

function parseCookies(cookieHeader = "") {
  return cookieHeader.split(";").reduce((cookies, part) => {
    const [rawKey, ...rawValue] = part.trim().split("=");
    if (!rawKey) {
      return cookies;
    }
    cookies[rawKey] = decodeURIComponent(rawValue.join("=") || "");
    return cookies;
  }, {});
}

function serializeCookie(name, value, options = {}) {
  const parts = [`${name}=${encodeURIComponent(value)}`];
  parts.push("HttpOnly");
  parts.push("SameSite=Strict");
  parts.push(`Path=${options.path || "/"}`);
  if (options.maxAge) {
    parts.push(`Max-Age=${options.maxAge}`);
  }
  if (options.secure) {
    parts.push("Secure");
  }
  return parts.join("; ");
}

function createSession() {
  const sessionId = crypto.randomBytes(32).toString("hex");
  const csrfToken = crypto.randomBytes(32).toString("hex");
  sessions.set(sessionId, {
    csrfToken,
    createdAt: Date.now(),
  });
  return { sessionId, csrfToken };
}

function destroySession(sessionId) {
  if (sessionId) {
    sessions.delete(sessionId);
  }
}

function sessionFromRequest(req) {
  const cookies = parseCookies(req.headers.cookie);
  const sessionId = cookies[SESSION_COOKIE];
  const session = sessionId ? sessions.get(sessionId) : null;
  return session ? { sessionId, session } : null;
}

function requireSession(req, res, next) {
  const current = sessionFromRequest(req);
  if (!current) {
    res.redirect("/login");
    return;
  }
  req.adminSessionId = current.sessionId;
  req.adminSession = current.session;
  next();
}

function requireCsrf(req, res, next) {
  const current = sessionFromRequest(req);
  const token = req.body && req.body._csrf;
  if (!current || !token || token !== current.session.csrfToken) {
    res.status(403).send("CSRF doğrulaması başarısız.");
    return;
  }
  req.adminSessionId = current.sessionId;
  req.adminSession = current.session;
  next();
}

function setSessionCookies(res, sessionId, csrfToken) {
  res.setHeader("Set-Cookie", [
    serializeCookie(SESSION_COOKIE, sessionId, { maxAge: 60 * 60 * 8 }),
    serializeCookie(CSRF_COOKIE, csrfToken, { maxAge: 60 * 60 * 8 }),
  ]);
}

function clearSessionCookies(res) {
  res.setHeader("Set-Cookie", [
    serializeCookie(SESSION_COOKIE, "", { maxAge: 0 }),
    serializeCookie(CSRF_COOKIE, "", { maxAge: 0 }),
  ]);
}

function verifyPassword(input, expected) {
  if (!expected || expected.length < 16) {
    throw new Error("LOCAL_ADMIN_PASSWORD must be set and at least 16 characters long.");
  }
  const inputBuffer = Buffer.from(input || "");
  const expectedBuffer = Buffer.from(expected);
  if (inputBuffer.length !== expectedBuffer.length) {
    return false;
  }
  return crypto.timingSafeEqual(inputBuffer, expectedBuffer);
}

module.exports = {
  SESSION_COOKIE,
  CSRF_COOKIE,
  parseCookies,
  createSession,
  destroySession,
  sessionFromRequest,
  requireSession,
  requireCsrf,
  setSessionCookies,
  clearSessionCookies,
  verifyPassword,
};
