import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import { categoryLabel } from '../lib/categories';

export default function SellerDashboard({ user }) {
  const [seller, setSeller] = useState(null);
  const [products, setProducts] = useState([]);
  const [selectedSellerId, setSelectedSellerId] = useState('');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const activeSellerId = useMemo(() => {
    if (selectedSellerId) return Number(selectedSellerId);
    if (seller?.id) return seller.id;
    if (user?.id) return user.id;
    return null;
  }, [selectedSellerId, seller?.id, user?.id]);

  async function loadSellerProfile() {
    if (!user?.id) return;
    try {
      const mine = await api.seller(user.id);
      setSeller(mine);
    } catch {
      setSeller(null);
    }
  }

  async function loadProducts() {
    if (!activeSellerId) return;
    setLoading(true);
    setError('');
    try {
      const response = await api.products({
        sellerId: activeSellerId,
        search: query || undefined,
        page: 0,
        size: 50,
        sortBy: 'id',
        sortDir: 'asc'
      });
      setProducts(response.content || []);
    } catch (err) {
      setError(err.message || 'Urunler yuklenemedi.');
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadSellerProfile();
  }, [user?.id]);

  useEffect(() => {
    loadProducts();
  }, [activeSellerId]);

  return (
    <div className="grid">
      <div className="card hero">
        <h1 className="h1">Satici Paneli</h1>
        <p className="meta">Saticiya ozel urun listesini goruntuleyebilir, stok ve fiyatlari takip edebilirsiniz.</p>
        <div className="hero-kpis">
          <div className="kpi-chip">
            <span>Aktif Satici ID</span>
            <strong>{activeSellerId || '-'}</strong>
          </div>
          <div className="kpi-chip">
            <span>Toplam Urun</span>
            <strong>{products.length}</strong>
          </div>
          <div className="kpi-chip">
            <span>Profil</span>
            <strong>{seller ? 'Eslesti' : 'Bulunamadi'}</strong>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="row">
          <input
            type="number"
            min="1"
            placeholder="Satici ID (opsiyonel)"
            value={selectedSellerId}
            onChange={(e) => setSelectedSellerId(e.target.value)}
          />
          <input
            placeholder="Urun ara..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button className="btn primary" onClick={loadProducts} disabled={!activeSellerId || loading}>
            Filtrele
          </button>
        </div>
        {error && <div className="error">{error}</div>}
      </div>

      {seller && (
        <div className="card">
          <h2 className="section-title">{seller.storeName}</h2>
          <p className="meta">Vergi No: {seller.taxNumber} | Puan: {seller.ratingAverage} | {seller.isOfficialStore ? 'Resmi Magaza' : 'Standart Magaza'}</p>
        </div>
      )}

      <div className="grid products">
        {loading && <div className="meta">Yukleniyor...</div>}
        {products.map((p) => (
          <article className="card product-card" key={p.id}>
            <img className="product-image" src={p.imageUrl} alt={p.name} />
            <div className="tag">Stok: {p.stock}</div>
            <h3>{p.name}</h3>
            <div className="meta">{p.brand} | {categoryLabel(p.categoryId)} | Seller #{p.sellerId}</div>
            <p className="meta">{p.description?.slice(0, 90)}</p>
            <div className="space">
              <strong>{p.price} TL</strong>
              <span className="meta">{p.active ? 'Yayinda' : 'Pasif'}</span>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
