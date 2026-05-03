import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:3000/api";
const USERNAME = __ENV.AUTH_USER || "customer1";
const PASSWORD = __ENV.AUTH_PASS || "pass123";
const TARGET_PRODUCT_ID = __ENV.TARGET_PRODUCT_ID ? Number(__ENV.TARGET_PRODUCT_ID) : null;
const raceOrderCreated = new Counter("race_order_created");
const raceOrderRejected = new Counter("race_order_rejected");
const raceAddRejected = new Counter("race_add_rejected");

export const options = {
  scenarios: {
    race_last_item: {
      executor: "per-vu-iterations",
      vus: 5,
      iterations: 1,
      maxDuration: "2m",
    },
  },
  thresholds: {
    checks: ["rate>0.90"],
    // Rejected requests are expected in this race test.
    http_req_failed: ["rate<0.90"],
    race_order_created: ["count==1"],
    race_order_rejected: ["count==4"],
  },
};

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  };
}

function login() {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(res, { "login status is 200": (r) => r.status === 200 });
  const data = res.json();
  return data && data.token ? data.token : null;
}

function getCustomerId(token) {
  const res = http.get(`${BASE_URL}/auth/me`, authHeaders(token));
  check(res, { "me status is 200": (r) => r.status === 200 });
  const data = res.json();
  return data && data.id ? data.id : null;
}

function ensureCart(token, customerId) {
  const createRes = http.post(
    `${BASE_URL}/carts`,
    JSON.stringify({ customerId, totalAmount: 0 }),
    authHeaders(token),
  );
  check(createRes, { "create cart status ok": (r) => r.status === 200 || r.status === 201 });
  const created = createRes.json();
  return created && created.id ? created.id : null;
}

function createOrder(token, cartAfterAdd, customerId, sellerId) {
  const payload = {
    customerId,
    sellerId,
    totalAmount: cartAfterAdd.totalAmount || 0,
    discountAmount: cartAfterAdd.discountAmount || 0,
    items: (cartAfterAdd.items || []).map((i) => ({
      productId: i.productId,
      quantity: i.quantity,
      unitPrice: i.unitPrice,
    })),
  };
  return http.post(`${BASE_URL}/orders`, JSON.stringify(payload), authHeaders(token));
}

export function setup() {
  const token = login();
  if (!token) {
    throw new Error("Setup login failed.");
  }

  let productId = TARGET_PRODUCT_ID;
  if (!productId) {
    throw new Error("TARGET_PRODUCT_ID zorunlu. Ornek: TARGET_PRODUCT_ID=5");
  }

  const detailRes = http.get(`${BASE_URL}/products/${productId}`, authHeaders(token));
  check(detailRes, { "target product detail status is 200": (r) => r.status === 200 });
  const product = detailRes.json();
  if (!product || !product.id) {
    throw new Error("Target product bulunamadi.");
  }
  if (Number(product.stock || 0) !== 1) {
    throw new Error(`Test onkosulu saglanmadi. Urun stock=1 olmali, mevcut stock=${product.stock}`);
  }

  return {
    productId: product.id,
    price: product.price,
    sellerId: product.sellerId,
  };
}

export default function (data) {
  if (exec.vu.iterationInScenario !== 0) {
    return;
  }

  const token = login();
  check(token, { "token exists": (t) => !!t });
  if (!token) return;

  const customerId = getCustomerId(token);
  check(customerId, { "customer id exists": (c) => !!c });
  if (!customerId) return;

  const cartId = ensureCart(token, customerId);
  check(cartId, { "cart id exists": (c) => !!c });
  if (!cartId) return;

  const addRes = http.post(
    `${BASE_URL}/carts/${cartId}/items`,
    JSON.stringify({
      productId: data.productId,
      quantity: 1,
      unitPrice: data.price,
    }),
    authHeaders(token),
  );

  const added = addRes.json() || {};
  const hasItems = Array.isArray(added.items) && added.items.length > 0;
  const isOutOfStock = addRes.status === 409 || addRes.status === 400;

  if (hasItems) {
    const orderRes = createOrder(token, added, customerId, data.sellerId);
    if (orderRes.status === 200 || orderRes.status === 201) {
      raceOrderCreated.add(1);
    } else if (orderRes.status === 400 || orderRes.status === 409) {
      raceOrderRejected.add(1);
    }
    check(orderRes, {
      "order created or rejected safely": (r) =>
        r.status === 200 || r.status === 201 || r.status === 400 || r.status === 409,
    });
  } else {
    if (isOutOfStock) {
      raceAddRejected.add(1);
      raceOrderRejected.add(1);
    }
    check(isOutOfStock, { "safe stock rejection": (v) => v === true });
  }

  sleep(0.2);
}
