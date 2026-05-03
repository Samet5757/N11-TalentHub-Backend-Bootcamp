import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:3000/api";
const USERNAME = __ENV.AUTH_USER || "customer1";
const PASSWORD = __ENV.AUTH_PASS || "pass123";

export const options = {
  stages: [
    { duration: "30s", target: 100 },
    { duration: "5m", target: 100 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1500", "avg<500"],
    checks: ["rate>0.99"],
  },
};

function jsonHeaders(token) {
  return {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  };
}

function login() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(res, { "login status 200": (r) => r.status === 200 });
  const body = res.json();
  return body && body.token ? body.token : null;
}

export default function () {
  const token = login();
  check(token, { "token exists": (t) => !!t });
  if (!token) return;

  const me = http.get(`${BASE_URL}/auth/me`, jsonHeaders(token));
  check(me, { "me status 200": (r) => r.status === 200 });

  const products = http.get(
    `${BASE_URL}/products?page=0&size=10&sortBy=id&sortDir=asc`,
    jsonHeaders(token),
  );
  check(products, { "products status 200": (r) => r.status === 200 });

  const productList = products.json();
  let productId = null;
  if (productList && Array.isArray(productList.content) && productList.content.length > 0) {
    productId = productList.content[0].id;
  }

  if (productId) {
    const detail = http.get(`${BASE_URL}/products/${productId}`, jsonHeaders(token));
    check(detail, { "product detail status 200": (r) => r.status === 200 });
  }

  sleep(1);
}

