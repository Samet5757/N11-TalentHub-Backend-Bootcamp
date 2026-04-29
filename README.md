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

## Test Notu

Projede `order-service`, `payment-service` ve `cart-service` icin yeni servis testleri eklidir.
Eger ortamda kurumsal Maven mirror DNS erisimi yoksa test bagimlilik indirmesi hata verebilir.

## Bilinen Notlar

- `CODEX_HANDOVER.md` operasyonel takip dosyasidir ve git ignore altindadir.
- Frontend tarafinda API cagrilari `/api/*` uzerinden proxylenir (React route cakismasi engellenmistir).
