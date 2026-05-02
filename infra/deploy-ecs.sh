#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   AWS_REGION=eu-north-1 \
#   AWS_ACCOUNT_ID=123456789012 \
#   ECS_CLUSTER=my-cluster \
#   IMAGE_TAG=2026-05-03-fix1 \
#   ./infra/deploy-ecs.sh
#
# Optional:
#   SERVICES="discovery-server auth-service product-service cart-service order-service payment-service seller-service campaign-service api-gateway frontend"
#   SKIP_MVN_BUILD=true

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${ECS_CLUSTER:?ECS_CLUSTER is required}"

IMAGE_TAG="${IMAGE_TAG:-latest}"
SKIP_MVN_BUILD="${SKIP_MVN_BUILD:-false}"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

DEFAULT_SERVICES=(
  discovery-server
  auth-service
  product-service
  cart-service
  order-service
  payment-service
  seller-service
  campaign-service
  api-gateway
  frontend
)

if [[ -n "${SERVICES:-}" ]]; then
  # shellcheck disable=SC2206
  SERVICE_LIST=(${SERVICES})
else
  SERVICE_LIST=("${DEFAULT_SERVICES[@]}")
fi

echo "==> AWS login"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY"

if [[ "$SKIP_MVN_BUILD" != "true" ]]; then
  echo "==> Maven build"
  mvn -U clean package -DskipTests
fi

build_and_push() {
  local service="$1"
  local repo="${service}"
  local image="${ECR_REGISTRY}/${repo}:${IMAGE_TAG}"

  echo "==> Building ${service} -> ${image}"
  if [[ "$service" == "frontend" ]]; then
    docker build -t "$image" -f "${service}/Dockerfile" .
  else
    docker build -t "$image" \
      --build-arg MODULE="$service" \
      -f "${service}/Dockerfile" .
  fi

  echo "==> Pushing ${image}"
  docker push "$image"
}

for s in "${SERVICE_LIST[@]}"; do
  build_and_push "$s"
done

for s in "${SERVICE_LIST[@]}"; do
  echo "==> Force new deployment: ${s}"
  aws ecs update-service \
    --region "$AWS_REGION" \
    --cluster "$ECS_CLUSTER" \
    --service "$s" \
    --force-new-deployment >/dev/null
done

echo "==> Waiting for services to become stable"
for s in "${SERVICE_LIST[@]}"; do
  echo "   - ${s}"
  aws ecs wait services-stable \
    --region "$AWS_REGION" \
    --cluster "$ECS_CLUSTER" \
    --services "$s"
done

echo "Done. Deployed services: ${SERVICE_LIST[*]} (tag=${IMAGE_TAG})"
