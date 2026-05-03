# Ecommerce Microservices

Spring Boot mikroservisleri + React frontend ile gelistirilmis fullstack e-ticaret bitirme projesi.

## Moduller

- `discovery-server`: Eureka service registry
- `api-gateway`: Tum backend giris noktasi + JWT filter
- `auth-service`: Login/JWT/rol bazli kimlik dogrulama
- `product-service`: Urun katalog, arama, filtre, pagination
- `cart-service`: Sepet item CRUD + kupon uygulama/kaldirma
- `order-service`: Siparis olusturma ve durum akisi
- `payment-service`: Payment intent + confirm akisi
- `seller-service`: Satici yonetimi
- `campaign-service`: Kampanya dogrulama servisi
- `frontend`: React (login, ana sayfa, urun detay, sepet, siparislerim)

## Hızlı Başlangıç (Docker)

Tum sistemi tek komutla kaldir:

```bash
docker compose -f docker/docker-compose.yml up --build -d
```

Ulasim:

- Frontend: `http://localhost:3000`
- API Gateway: `http://localhost:8080`

## Demo Kullanıcıları

- Musteri:
  - `username: customer1`
  - `password: pass123`
- Satici:
  - `username: seller1`
  - `password: pass123`

## Frontend Akışı

1. Login ol.
2. Ana sayfada urunleri listele, arama/kategori ile daralt.
3. Urun detay veya hizli buton ile sepete ekle.
4. Sepette kupon uygula/kaldir, adet guncelle.
5. Checkout ile siparis + odeme tamamla.
6. Siparislerim ekraninda filtrele ve detaylari ac.

## Kritik Komutlar

Yalnizca frontend guncelle:

```bash
docker compose -f docker/docker-compose.yml up --build -d frontend
```

Canli stack smoke testi:

```bash
bash ./smoke-test.sh
```

Bu script asagidaki zinciri test eder:

- login
- auth me
- urun secimi
- sepet bul/olustur + item ekle
- siparis olusturma
- payment intent + confirm
- siparislerimde yeni siparis dogrulama

## k6 Performans ve Yarış Testleri

Bu repoda teslim icin iki k6 senaryosu hazirdir:

- `tests/k6-last-item-5-user.js`: 5 kullanici ayni anda tek stoklu urune siparis dener.
- `tests/k6-load-100-user.js`: 100 kullanici login + urun listeleme + urun detay akisina yuk bindirir.

### k6 Kurulum

```bash
brew install k6
```

### 1) Son Urun Yarış Testi (5 Kullanici)

On kosul:

- Test edecegin urunun `stock=1` olmasi gerekir.

Calistirma:

```bash
TARGET_PRODUCT_ID=206 k6 run tests/k6-last-item-5-user.js
```

Beklenen sonuc:

- `race_order_created = 1`
- `race_order_rejected = 4`

Bu sonuc, tek stoklu urunde oversell (fazla satis) olmadigini gosterir.

### 2) 100 Kullanici Yuk Testi

Calistirma:

```bash
k6 run tests/k6-load-100-user.js
```

Varsayilan yuk profili:

- 30 sn icinde 100 VU ramp-up
- 5 dk 100 VU sabit yuk
- 30 sn ramp-down

Threshold'lar:

- `http_req_failed < %1`
- `http_req_duration p95 < 1500ms`
- `http_req_duration avg < 500ms`

### Ortam Degiskenleri

Gerekirse testleri su degiskenlerle override edebilirsin:

- `BASE_URL` (varsayilan: `http://localhost:3000/api`)
- `AUTH_USER` (varsayilan: `customer1`)
- `AUTH_PASS` (varsayilan: `pass123`)
- `TARGET_PRODUCT_ID` (yalnizca yaris testi icin zorunlu)

## Stok Tutarliligi Notu

- Siparis olusturma akisinda stok rezervasyonu senkron/atomik olarak `product-service` uzerinden yapilir.
- Stok yetersiz ise siparis olusturma reddedilir.
- Bu degisiklik ile 5 kullanicili yaris testinde beklenen `1 basari / 4 red` sonucu dogrulanmistir.

## Test Notu

Projede `order-service`, `payment-service` ve `cart-service` icin yeni servis testleri eklidir.
Eger ortamda kurumsal Maven mirror DNS erisimi yoksa test bagimlilik indirmesi hata verebilir.

## Bilinen Notlar

- `CODEX_HANDOVER.md` operasyonel takip dosyasidir ve git ignore altindadir.
- Frontend tarafinda API cagrilari `/api/*` uzerinden proxylenir (React route cakismasi engellenmistir).

## Prod Deployment (ECS + ECR)

Bu repo icinde prod hazirliklari eklidir:

- Servis bazli Dockerfile: `*/Dockerfile`
- Prod profile dosyalari: `*/src/main/resources/application-prod.properties`
- Merkezi prod compose: `docker-compose.yml`
- CI/CD workflow: `.github/workflows/deploy.yml`
- ECS task definition sablonlari: `infra/ecs/taskdefs/*.json`
- Ornek env: `.env.prod.example`

### Gerekli GitHub Secrets

- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `ECR_REGISTRY` (ornek: `123456789012.dkr.ecr.eu-north-1.amazonaws.com`)
- `ECS_CLUSTER`
- `ECS_EXECUTION_ROLE_ARN`
- `ECS_TASK_ROLE_ARN`
- `EUREKA_DEFAULT_ZONE`
- `JWT_SECRET`
- `RDS_HOST`
- `RDS_PORT` (opsiyonel, default `5432`)
- `RDS_USER`
- `RDS_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS` (opsiyonel)
- `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` (opsiyonel)
- `SPRING_CACHE_TYPE` (opsiyonel, default `redis`)
- `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD`
- `IYZICO_BASE_URL` / `IYZICO_API_KEY` / `IYZICO_SECRET_KEY`
- `AUTH_SERVICE_URL` / `PRODUCT_SERVICE_URL` / `CART_SERVICE_URL` / `ORDER_SERVICE_URL` / `PAYMENT_SERVICE_URL` / `SELLER_SERVICE_URL` (opsiyonel)

### Onemli Operasyon Notu

- Servis sifreleri ve anahtarlar repo icinde tutulmamali, AWS Secrets Manager veya SSM Parameter Store ile yonetilmelidir.
