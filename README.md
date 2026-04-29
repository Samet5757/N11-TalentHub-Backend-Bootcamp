# Ecommerce Microservices

n11 TalentHub bitirme projesi icin Spring Boot mikroservisleri ve ileride eklenecek React frontend uygulamasinin ana reposu.

## Moduller

- `discovery-server`: Eureka service registry.
- `api-gateway`: Backend servislerine giris noktasi.
- `auth-service`: Kullanici ve kimlik dogrulama.
- `product-service`: Urun katalog islemleri.
- `cart-service`: Sepet islemleri.
- `order-service`: Siparis islemleri.
- `payment-service`: Odeme islemleri.

## Lokal Dogrulama

```bash
mvn -q test
```

## Docker ile Tum Sistemi Ayaga Kaldirma

```bash
docker compose -f docker/docker-compose.yml up --build -d
```

- Frontend: `http://localhost:3000`
- API Gateway: `http://localhost:8080`

## PostgreSQL Dev Profili

Default profil hizli lokal dogrulama icin H2 kullanir. PostgreSQL ile calismak icin once veritabanlarini baslat:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Sonra ilgili servisi `dev` Spring profili ve `postgres` Maven profili ile calistir:

```bash
mvn -Ppostgres spring-boot:run -Dspring-boot.run.profiles=dev
```

Not: PostgreSQL driver'i ilk calistirmada Maven repository/mirror erisimi gerektirir.

## Uygulama Sirasi

1. Proje temeli, gateway/discovery ve PostgreSQL dev profilleri.
2. Product service: DTO, validation, pagination, category/seller alanlari.
3. Auth service: BCrypt, JWT, customer/seller/admin rolleri.
4. Cart service: kullanici bazli sepet, urun/stock kontrolu, kupon hesabi.
5. Order service: order item modeli ve n11'e yakin siparis status akisi.
6. Payment service: Iyzico checkout ve odeme koruma simulasyonu.
7. React frontend: urun liste/detay, sepet, checkout, login/register.
8. Nice-to-have: yorum, soru-cevap, magazalar ve kampanyalar.
