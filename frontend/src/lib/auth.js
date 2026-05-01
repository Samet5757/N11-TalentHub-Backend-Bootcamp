const TOKEN_KEY = 'n11_token';
const TOKEN_EVENT = 'n11-token-changed';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
  window.dispatchEvent(new CustomEvent(TOKEN_EVENT));
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  window.dispatchEvent(new CustomEvent(TOKEN_EVENT));
}

export function onTokenChange(listener) {
  window.addEventListener(TOKEN_EVENT, listener);
  window.addEventListener('storage', listener);
  return () => {
    window.removeEventListener(TOKEN_EVENT, listener);
    window.removeEventListener('storage', listener);
  };
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
  const role = payload.role || (payload.roleCode === 'ROLE_ADMIN' ? 'ADMIN' : 'CUSTOMER');
  return {
    userId: Number(payload.userId ?? payload.sub),
    role,
    roleCode: payload.roleCode || (role === 'ADMIN' ? 'ROLE_ADMIN' : 'ROLE_USER'),
    username: payload.sub
  };
}
