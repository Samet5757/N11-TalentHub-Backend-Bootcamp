import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { categories, categoryLabel } from '../lib/categories';

export default function HomePage({ onQuickAdd }) {
  const [products, setProducts] = useState([]);
  const [query, setQuery] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function load() {
    setLoading(true);
    setError('');
    try {
      const page = await api.products({
        search: query,
        categoryId: categoryId || undefined,
        page: 0,
        size: 200,
        sortBy: 'id',
        sortDir: 'asc'
      });
      setProducts(page.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="grid">
      <div className="card">
        <h1 className="h1">Ana Sayfa / Urun Arama</h1>
        <div className="row">
          <input placeholder="Urun ara..." value={query} onChange={(e) => setQuery(e.target.value)} />
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Tum Kategoriler</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
          <button className="btn primary" onClick={load}>Ara</button>
        </div>
        {error && <div className="error">{error}</div>}
      </div>

      <div className="grid products">
        {loading && <div className="meta">Yukleniyor...</div>}
        {products.map((p) => (
          <article className="card" key={p.id}>
            <img className="product-image" src={p.imageUrl || 'https://via.placeholder.com/400x260?text=Product'} alt={p.name} />
            <div className="tag">Stok: {p.stock}</div>
            <h3>{p.name}</h3>
            <div className="meta">{p.brand} | {categoryLabel(p.categoryId)} | Seller #{p.sellerId}</div>
            <p className="meta">{p.description?.slice(0, 80)}</p>
            <div className="space">
              <strong>{p.price} TL</strong>
              <div className="row">
                <Link className="btn" to={`/products/${p.id}`}>Detay</Link>
                <button
                  className={`btn ${p.stock > 0 ? 'primary' : 'soldout'}`}
                  onClick={() => onQuickAdd(p)}
                  disabled={p.stock <= 0}
                >
                  {p.stock <= 0 ? 'Tukendi' : 'Sepete Ekle'}
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
