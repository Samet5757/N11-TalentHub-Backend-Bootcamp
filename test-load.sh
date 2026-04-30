#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
PRODUCT_ID="${PRODUCT_ID:-1}"
CUSTOMER_ID="${CUSTOMER_ID:-2}"
USERNAME="${USERNAME:-customer1}"
PASSWORD="${PASSWORD:-pass123}"
TOTAL_REQUESTS=100
TARGET_STOCK=10
WAIT_SECONDS=15

WORK_DIR="/tmp/load-test-$RANDOM-$$"
mkdir -p "$WORK_DIR"
trap 'rm -rf "$WORK_DIR"' EXIT

LOGIN_JSON=$(curl -s -X POST "$BASE_URL/auth/login" -H 'Content-Type: application/json' -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$LOGIN_JSON" | jq -r '.token // empty')
if [ -z "$TOKEN" ]; then
  echo "Login failed"
  exit 1
fi

PRODUCT_JSON=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/products/$PRODUCT_ID")
UNIT_PRICE=$(echo "$PRODUCT_JSON" | jq -r '.price // empty')
if [ -z "$UNIT_PRICE" ] || [ "$UNIT_PRICE" = "null" ]; then
  UNIT_PRICE="100.00"
fi

# Authoritative stock reset via DB (customer token cannot update stock through gateway).
docker exec -i product-db psql -U user -d product_db -c "UPDATE products SET stock = $TARGET_STOCK WHERE id = $PRODUCT_ID;" >/dev/null

for i in $(seq 1 "$TOTAL_REQUESTS"); do
  (
    curl -s -X POST "$BASE_URL/orders" \
      -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' \
      -d "{\"customerId\":$CUSTOMER_ID,\"sellerId\":1,\"totalAmount\":$UNIT_PRICE,\"discountAmount\":0,\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":1,\"unitPrice\":$UNIT_PRICE}]}" \
      > "$WORK_DIR/resp_$i.json"
  ) &
done
wait

sleep "$WAIT_SECONDS"

success_count=0
failed_count=0

for i in $(seq 1 "$TOTAL_REQUESTS"); do
  order_id=$(jq -r '.id // empty' "$WORK_DIR/resp_$i.json" 2>/dev/null || true)
  if [ -z "$order_id" ]; then
    failed_count=$((failed_count + 1))
    continue
  fi

  status=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/orders/$order_id" | jq -r '.status // empty')
  case "$status" in
    INVENTORY_RESERVED|PAYMENT_PENDING|PAYMENT_AUTHORIZED|APPROVED|SHIPPED|DELIVERED|COMPLETED)
      success_count=$((success_count + 1))
      ;;
    *)
      failed_count=$((failed_count + 1))
      ;;
  esac
done

stock_after=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/products/$PRODUCT_ID" | jq -r '.stock // "unknown"')

echo "Toplam İstek: $TOTAL_REQUESTS | Başarılı Sipariş: $success_count | İptal/Failed Sipariş: $failed_count | Kalan Stok: $stock_after"
