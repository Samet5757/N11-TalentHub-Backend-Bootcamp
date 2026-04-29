const TOKEN_KEY = 'n11_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function parseJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split('.')[1];
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function getUserContext() {
  const token = getToken();
  const payload = parseJwt(token);
  if (!payload) return null;
  return {
    userId: Number(payload.userId ?? payload.sub),
    role: payload.role,
    username: payload.sub
  };
}
