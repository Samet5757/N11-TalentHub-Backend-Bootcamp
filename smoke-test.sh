#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_USER="${AUTH_USER:-goldenuser}"
AUTH_PASS="${AUTH_PASS:-Pass123!}"
AUTH_ROLE="${AUTH_ROLE:-CUSTOMER}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

if ! command -v jq >/dev/null 2>&1; then
  echo -e "${RED}Hata: jq yüklü değil. Lütfen jq kurun ve tekrar deneyin.${NC}"
  exit 1
fi

echo "[1/5] Gateway üzerinden login alınıyor..."
LOGIN_PAYLOAD=$(jq -n --arg u "$AUTH_USER" --arg p "$AUTH_PASS" '{username:$u,password:$p}')
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_PAYLOAD")

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')

if [[ -z "$TOKEN" ]]; then
  echo "Kullanıcı bulunamadı/şifre hatalı, register deneniyor..."
  REGISTER_PAYLOAD=$(jq -n --arg u "$AUTH_USER" --arg p "$AUTH_PASS" --arg r "$AUTH_ROLE" \
    '{username:$u,password:$p,role:$r}')
  curl -s -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "$REGISTER_PAYLOAD" >/dev/null

  LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_PAYLOAD")
  TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty')
fi

if [[ -z "$TOKEN" ]]; then
  echo -e "${RED}Login başarısız. Token alınamadı.${NC}"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer $TOKEN"

echo "[2/5] Sipariş oluşturuluyor..."
ORDER_PAYLOAD=$(jq -n '{
  customerId: 1,
  sellerId: 10,
  totalAmount: 100,
  discountAmount: 0,
  items: [
    {productId: 1001, quantity: 1, unitPrice: 100}
  ]
}')

ORDER_RESPONSE=$(curl -sS -X POST "$BASE_URL/orders" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "$ORDER_PAYLOAD")

ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id // empty')
if [[ -z "$ORDER_ID" ]]; then
  echo -e "${RED}Sipariş oluşturulamadı. Yanıt: $ORDER_RESPONSE${NC}"
  exit 1
fi
echo "Sipariş oluşturuldu. orderId=$ORDER_ID"

echo "[3/5] Ödeme işlemi tetikleniyor..."
PAYMENT_PAYLOAD=$(jq -n --argjson oid "$ORDER_ID" '{
  orderId: $oid,
  cardNumber: "4242123412341234",
  amount: 100
}')

PAYMENT_RESPONSE=$(curl -sS -X POST "$BASE_URL/payments/pay" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d "$PAYMENT_PAYLOAD")

PAYMENT_STATUS=$(echo "$PAYMENT_RESPONSE" | jq -r '.paymentStatus // empty')
if [[ "$PAYMENT_STATUS" != "SUCCESS" ]]; then
  echo -e "${RED}Ödeme başarısız. Yanıt: $PAYMENT_RESPONSE${NC}"
  exit 1
fi

echo "[4/5] Sipariş güncel durumu çekiliyor..."
ORDER_AFTER_PAYMENT=$(curl -sS -X GET "$BASE_URL/orders/$ORDER_ID" \
  -H "$AUTH_HEADER")

ORDER_STATUS=$(echo "$ORDER_AFTER_PAYMENT" | jq -r '.status // empty')

if [[ "$ORDER_STATUS" == "PAID" ]]; then
  echo -e "${GREEN}Ödeme Başarılı! Sipariş Statüsü: PAID${NC}"
else
  echo -e "${RED}Ödeme sonrası statü beklenenden farklı: $ORDER_STATUS${NC}"
  echo "Yanıt: $ORDER_AFTER_PAYMENT"
  exit 1
fi

echo "[5/5] Golden Path smoke test tamamlandı."
