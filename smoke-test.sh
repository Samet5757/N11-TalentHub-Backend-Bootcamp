#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000/api}"
AUTH_USER="${AUTH_USER:-customer1}"
AUTH_PASS="${AUTH_PASS:-pass123}"
CARD_NUMBER="${CARD_NUMBER:-5528790000000008}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

if ! command -v jq >/dev/null 2>&1 || ! command -v curl >/dev/null 2>&1; then
  echo -e "${RED}Hata: jq ve curl kurulu olmalı.${NC}"
  exit 1
fi

echo "[1/8] Login..."
LOGIN_RESPONSE=$(curl -sS -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$AUTH_USER\",\"password\":\"$AUTH_PASS\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
if [[ -z "$TOKEN" ]]; then
  echo -e "${RED}Login basarisiz: $LOGIN_RESPONSE${NC}"
  exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"

echo "[2/8] Kullanici profili..."
ME_RESPONSE=$(curl -sS -X GET "$BASE_URL/auth/me" -H "$AUTH_HEADER")
CUSTOMER_ID=$(echo "$ME_RESPONSE" | jq -r '.id // empty')
if [[ -z "$CUSTOMER_ID" ]]; then
  echo -e "${RED}/auth/me basarisiz: $ME_RESPONSE${NC}"
  exit 1
fi

echo "[3/8] Urun listesi..."
PRODUCTS_RESPONSE=$(curl -sS -X GET "$BASE_URL/products?page=0&size=10&sortBy=id&sortDir=asc" -H "$AUTH_HEADER")
PRODUCT_ID=$(echo "$PRODUCTS_RESPONSE" | jq -r '.content[] | select(.stock > 0) | .id' | head -n 1)
if [[ -z "$PRODUCT_ID" ]]; then
  echo -e "${RED}Stokta urun bulunamadi.${NC}"
  exit 1
fi

PRODUCT_RESPONSE=$(curl -sS -X GET "$BASE_URL/products/$PRODUCT_ID" -H "$AUTH_HEADER")
SELLER_ID=$(echo "$PRODUCT_RESPONSE" | jq -r '.sellerId // empty')
UNIT_PRICE=$(echo "$PRODUCT_RESPONSE" | jq -r '.price // empty')
if [[ -z "$SELLER_ID" || -z "$UNIT_PRICE" ]]; then
  echo -e "${RED}Urun detayi alinamadi: $PRODUCT_RESPONSE${NC}"
  exit 1
fi

echo "[4/8] Aktif sepet bulunuyor/olusturuluyor..."
CARTS_RESPONSE=$(curl -sS -X GET "$BASE_URL/carts/customer/$CUSTOMER_ID" -H "$AUTH_HEADER")
CART_ID=$(echo "$CARTS_RESPONSE" | jq -r '.[0].id // empty')
if [[ -z "$CART_ID" ]]; then
  CREATE_CART=$(curl -sS -X POST "$BASE_URL/carts" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "{\"customerId\":$CUSTOMER_ID,\"totalAmount\":0}")
  CART_ID=$(echo "$CREATE_CART" | jq -r '.id // empty')
fi
if [[ -z "$CART_ID" ]]; then
  echo -e "${RED}Sepet olusturulamadi.${NC}"
  exit 1
fi

echo "[5/8] Sepete urun ekleme..."
CART_AFTER_ADD=$(curl -sS -X POST "$BASE_URL/carts/$CART_ID/items" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":$PRODUCT_ID,\"quantity\":1,\"unitPrice\":$UNIT_PRICE}")

ITEM_COUNT=$(echo "$CART_AFTER_ADD" | jq -r '.items | length')
if [[ "${ITEM_COUNT:-0}" -lt 1 ]]; then
  echo -e "${RED}Sepete urun eklenemedi: $CART_AFTER_ADD${NC}"
  exit 1
fi

echo "[6/8] Siparis olusturma..."
ORDER_PAYLOAD=$(echo "$CART_AFTER_ADD" | jq -c --argjson customerId "$CUSTOMER_ID" --argjson sellerId "$SELLER_ID" '{
  customerId: $customerId,
  sellerId: $sellerId,
  totalAmount: (.totalAmount // 0),
  discountAmount: (.discountAmount // 0),
  items: (.items // [] | map({productId, quantity, unitPrice}))
}')
ORDER_RESPONSE=$(curl -sS -X POST "$BASE_URL/orders" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "$ORDER_PAYLOAD")
ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')
if [[ -z "$ORDER_ID" ]]; then
  echo -e "${RED}Siparis olusturulamadi: $ORDER_RESPONSE${NC}"
  exit 1
fi

echo "[7/8] Odeme intent + confirm..."
ORDER_FINAL_AMOUNT=$(echo "$ORDER_RESPONSE" | jq -r '.finalAmount // .totalAmount // 0')
IDEMPOTENCY_KEY="smoke-${ORDER_ID}-$(date +%s)"
INTENT_RESPONSE=$(curl -sS -X POST "$BASE_URL/payments/intents" \
  -H "$AUTH_HEADER" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":$ORDER_ID,\"amount\":$ORDER_FINAL_AMOUNT}")
PAYMENT_INTENT_ID=$(echo "$INTENT_RESPONSE" | jq -r '.paymentIntentId // empty')
if [[ -z "$PAYMENT_INTENT_ID" ]]; then
  echo -e "${RED}Payment intent olusmadi: $INTENT_RESPONSE${NC}"
  exit 1
fi

PAYMENT_RESPONSE=$(curl -sS -X POST "$BASE_URL/payments/intents/$PAYMENT_INTENT_ID/confirm" \
  -H "$AUTH_HEADER" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}-confirm" \
  -H "Content-Type: application/json" \
  -d "{\"cardNumber\":\"$CARD_NUMBER\"}")

PAYMENT_STATUS=$(echo "$PAYMENT_RESPONSE" | jq -r '.paymentStatus // .status // empty')
if [[ "$PAYMENT_STATUS" != "SUCCESS" && "$PAYMENT_STATUS" != "COMPLETED" ]]; then
  echo -e "${RED}Odeme basarisiz: $PAYMENT_RESPONSE${NC}"
  exit 1
fi

echo "[8/8] Siparislerim kontrol..."
MAX_RETRY=10
SLEEP_SECONDS=2
FOUND_ORDER=0
FINAL_STATUS=""

for i in $(seq 1 "$MAX_RETRY"); do
  ORDERS_LIST=$(curl -sS -X GET "$BASE_URL/orders/customer/$CUSTOMER_ID" -H "$AUTH_HEADER")
  FOUND_ORDER=$(echo "$ORDERS_LIST" | jq -r --argjson oid "$ORDER_ID" '[.[] | select(.id == $oid)] | length')
  FINAL_STATUS=$(echo "$ORDERS_LIST" | jq -r --argjson oid "$ORDER_ID" '[.[] | select(.id == $oid)][0].status // empty')

  if [[ "${FOUND_ORDER:-0}" -ge 1 ]]; then
    break
  fi
  sleep "$SLEEP_SECONDS"
done

if [[ "${FOUND_ORDER:-0}" -lt 1 ]]; then
  echo -e "${RED}Siparis listesinde olusturulan siparis bulunamadi. sonResponse=$ORDERS_LIST${NC}"
  exit 1
fi

if [[ -z "$FINAL_STATUS" ]]; then
  echo -e "${RED}Siparis status bilgisi okunamadi. orderId=$ORDER_ID${NC}"
  exit 1
fi

echo -e "${GREEN}Smoke test basarili. orderId=$ORDER_ID, productId=$PRODUCT_ID, orderStatus=$FINAL_STATUS${NC}"
