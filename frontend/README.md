# Frontend (React + Vite)

## Calistirma (lokal dev)

1. Backend servislerini (ozellikle `api-gateway` 8080) ayaga kaldir.
2. Frontend bagimliliklarini yukle:

```bash
cd frontend
npm install
```

3. Gelistirme sunucusu:

```bash
npm run dev
```

Vite proxy uzerinden `/auth`, `/products`, `/carts`, `/orders`, `/payments` istekleri `http://localhost:8080` gateway'e yonlenir.

## Docker ile

Tum sistemi docker ile ayaga kaldirmak icin repo kokunden:

```bash
docker compose -f docker/docker-compose.yml up --build -d
```

Frontend: `http://localhost:3000`  
Gateway: `http://localhost:8080`

## Ekranlar

- `/login`: Auth login
- `/`: Ana sayfa + urun arama/listeleme
- `/products/:id`: Urun detay
- `/cart`: Sepet + kupon + checkout (order + payment intent confirm)
- `/orders`: Musteri siparisleri
