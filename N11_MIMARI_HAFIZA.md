# n11 TalentHub Mikroservis Projesi - Güncel Durum

**Sistem Altyapısı:**
- Java 21, Spring Boot 3.2.0, Spring Cloud (2023.0.0)
- PostgreSQL db'ler Docker üzerinde çalışıyor (auth, product, order).

**Şu Ana Kadar Bitirilenler (BUNLARI TEKRAR YAZMA):**
1. Product-service (DTO, Pagination, Exception Handling, Unit Testler) bitti.
2. Auth-service (JWT üretimi, BCrypt, CUSTOMER/SELLER/ADMIN rolleri) bitti.
3. Servislerin Eureka kayıt ayarları yapıldı.

**Sıradaki Bekleyen Görevler:**
1. `api-gateway` içine JWT doğrulama (JwtAuthenticationFilter) eklenecek.
2. `seller-service` (Mağaza altyapısı, port: 8086) sıfırdan oluşturulacak.