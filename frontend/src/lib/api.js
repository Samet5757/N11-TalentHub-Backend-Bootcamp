import { clearToken, getToken } from './auth';
const API_PREFIX = '/api';

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const url = `${API_PREFIX}${path.startsWith('/') ? path : `/${path}`}`;
  const res = await fetch(url, { ...options, headers });
  if (res.status === 401) {
    clearToken();
  }

  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { raw: text };
    }
  }

  if (!res.ok) {
    throw new Error(data?.message || data?.error || data?.raw || `Request failed: ${res.status}`);
  }
  return data;
}

function toQuery(params = {}) {
  const clean = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '');
  return new URLSearchParams(clean).toString();
}

export const api = {
  login: (username, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  me: () => request('/auth/me'),
  refresh: () => request('/auth/refresh', { method: 'POST' }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  products: (params) => request(`/products?${toQuery(params)}`),
  product: (id) => request(`/products/${id}`),
  cartsByCustomer: (customerId) => request(`/carts/customer/${customerId}`),
  createCart: (customerId) => request('/carts', { method: 'POST', body: JSON.stringify({ customerId, totalAmount: 0 }) }),
  addItem: (cartId, payload) => request(`/carts/${cartId}/items`, { method: 'POST', body: JSON.stringify(payload) }),
  updateItem: (cartId, itemId, payload) => request(`/carts/${cartId}/items/${itemId}`, { method: 'PUT', body: JSON.stringify(payload) }),
  removeItem: (cartId, itemId) => request(`/carts/${cartId}/items/${itemId}`, { method: 'DELETE' }),
  applyCoupon: (cartId, code) => request(`/carts/${cartId}/apply-coupon?code=${encodeURIComponent(code)}`, { method: 'POST' }),
  removeCoupon: (cartId) => request(`/carts/${cartId}/coupon`, { method: 'DELETE' }),
  deleteCart: (cartId) => request(`/carts/${cartId}`, { method: 'DELETE' }),
  createOrder: (payload) => request('/orders', { method: 'POST', body: JSON.stringify(payload) }),
  ordersByCustomer: (customerId) => request(`/orders/customer/${customerId}`),
  cancelOrder: (orderId) => request(`/orders/${orderId}/cancel`, { method: 'PUT' }),
  createPaymentIntent: (payload, key) => request('/payments/intents', { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(payload) }),
  confirmPayment: (paymentIntentId, cardNumber, key) => request(`/payments/intents/${paymentIntentId}/confirm`, {
    method: 'POST',
    headers: { 'Idempotency-Key': key },
    body: JSON.stringify({ cardNumber })
  })
};
