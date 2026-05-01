import React, { useEffect, useState } from 'react';
import { api } from '../lib/api';

const emptyForm = {
  name: '',
  description: '',
  brand: '',
  price: '',
  stock: '',
  categoryId: '',
  sellerId: '',
  imageUrl: '',
  badgeType: '',
  active: true
};

export default function AdminDashboard() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [filter, setFilter] = useState('ALL');

  async function loadProducts() {
    setLoading(true);
    setError('');
    try {
      const res = await api.products({ page: 0, size: 100, sortBy: 'id', sortDir: 'desc' });
      setProducts(res.content || []);
    } catch (err) {
      setError(err.message || 'Urunler yuklenemedi');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadProducts(); }, []);

  function onChange(e) {
    const { name, value, type, checked } = e.target;
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    try {
      await api.createProduct({
        ...form,
        price: Number(form.price),
        stock: Number(form.stock),
        categoryId: Number(form.categoryId),
        sellerId: Number(form.sellerId)
      });
      setForm(emptyForm);
      await loadProducts();
    } catch (err) {
      setError(err.message || 'Urun eklenemedi');
    }
  }

  async function handleDelete(id) {
    setError('');
    try {
      await api.deleteProduct(id);
      await loadProducts();
    } catch (err) {
      setError(err.message || 'Urun silinemedi');
    }
  }

  async function handleStockUpdate(id, currentStock) {
    const input = window.prompt('Yeni stok miktari', String(currentStock ?? 0));
    if (input === null) return;
    const nextStock = Number(input);
    if (Number.isNaN(nextStock) || nextStock < 0) {
      setError('Gecerli bir stok degeri girin');
      return;
    }
    setError('');
    try {
      await api.updateProductStock(id, nextStock);
      await loadProducts();
    } catch (err) {
      setError(err.message || 'Stok guncellenemedi');
    }
  }

  async function handleQuickUpdate(product) {
    setError('');
    try {
      await api.updateProduct(product.id, {
        name: product.name,
        description: product.description,
        brand: product.brand,
        price: Number(product.price),
        stock: Number(product.stock),
        categoryId: Number(product.categoryId),
        sellerId: Number(product.sellerId),
        imageUrl: product.imageUrl,
        badgeType: product.badgeType,
        active: product.active
      });
      await loadProducts();
    } catch (err) {
      setError(err.message || 'Urun guncellenemedi');
    }
  }

  const metrics = {
    total: products.length,
    active: products.filter((p) => p.active).length,
    outOfStock: products.filter((p) => (p.stock || 0) <= 0).length
  };

  const visibleProducts = products.filter((p) => {
    if (filter === 'ACTIVE') return p.active;
    if (filter === 'OUT') return (p.stock || 0) <= 0;
    return true;
  });

  return (
    <div className="grid">
      <section className="card">
        <div className="space">
          <h1 className="h1" style={{ margin: 0 }}>Admin Paneli</h1>
          <button className="btn" onClick={loadProducts} disabled={loading}>{loading ? 'Yukleniyor...' : 'Yenile'}</button>
        </div>
        <p className="meta">Urun yonetimi sadece admin kullanicilar icin aktiftir.</p>
        <div className="admin-metrics">
          <div className="kpi-chip"><span>Toplam Urun</span><strong>{metrics.total}</strong></div>
          <div className="kpi-chip"><span>Aktif Urun</span><strong>{metrics.active}</strong></div>
          <div className="kpi-chip"><span>Stokta Yok</span><strong>{metrics.outOfStock}</strong></div>
        </div>
        <div className="row">
          <button className={`btn ${filter === 'ALL' ? 'primary' : ''}`} onClick={() => setFilter('ALL')}>Tum Urunler</button>
          <button className={`btn ${filter === 'ACTIVE' ? 'primary' : ''}`} onClick={() => setFilter('ACTIVE')}>Sadece Aktif</button>
          <button className={`btn ${filter === 'OUT' ? 'primary' : ''}`} onClick={() => setFilter('OUT')}>Stokta Olmayanlar</button>
        </div>
        {error && <div className="error">{error}</div>}
      </section>

      <section className="card">
        <div className="space"><h2 className="section-title">Yeni Urun Ekle</h2></div>
        <form className="grid admin-form" onSubmit={handleCreate}>
          <input name="name" placeholder="Urun Adi" value={form.name} onChange={onChange} required />
          <input name="brand" placeholder="Marka" value={form.brand} onChange={onChange} required />
          <input name="price" type="number" step="0.01" min="0" placeholder="Fiyat" value={form.price} onChange={onChange} required />
          <input name="stock" type="number" min="0" placeholder="Stok" value={form.stock} onChange={onChange} required />
          <input name="categoryId" type="number" min="1" placeholder="Kategori ID" value={form.categoryId} onChange={onChange} required />
          <input name="sellerId" type="number" min="1" placeholder="Satici ID" value={form.sellerId} onChange={onChange} required />
          <input name="imageUrl" placeholder="Gorsel URL" value={form.imageUrl} onChange={onChange} />
          <input name="badgeType" placeholder="Badge" value={form.badgeType} onChange={onChange} />
          <textarea name="description" placeholder="Aciklama" value={form.description} onChange={onChange} rows={3} />
          <label className="meta"><input name="active" type="checkbox" checked={form.active} onChange={onChange} /> Aktif</label>
          <button className="btn primary" type="submit">Yeni Urun Ekle</button>
        </form>
      </section>

      <section className="card admin-table-wrap">
        <div className="space"><h2 className="section-title">Mevcut Urunler</h2></div>
        <div className="admin-table-scroll">
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Urun</th>
                <th>Marka</th>
                <th>Fiyat</th>
                <th>Stok</th>
                <th>Kategori</th>
                <th>Satici</th>
                <th>Islemler</th>
              </tr>
            </thead>
            <tbody>
              {visibleProducts.map((p) => (
                <tr key={p.id}>
                  <td>#{p.id}</td>
                  <td>{p.name}</td>
                  <td>{p.brand}</td>
                  <td>{p.price} TL</td>
                  <td>{p.stock}</td>
                  <td>{p.categoryId}</td>
                  <td>{p.sellerId}</td>
                  <td>
                    <div className="row">
                      <button className="btn" onClick={() => handleStockUpdate(p.id, p.stock)}>Stogu Guncelle</button>
                      <button className="btn" onClick={() => handleQuickUpdate(p)}>Guncelle</button>
                      <button className="btn" onClick={() => handleDelete(p.id)}>Sil</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
