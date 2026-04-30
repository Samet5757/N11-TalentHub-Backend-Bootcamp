import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { categories, categoryLabel } from '../lib/categories';
import { presentProduct } from '../lib/productPresentation';

export default function HomePage({ onQuickAdd }) {
  const [products, setProducts] = useState([]);
  const [query, setQuery] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const featuredProducts = products.slice(0, 4);
  const dealProducts = products.slice(4, 10);

  async function load(nextPage = page) {
    setLoading(true);
    setError('');
    try {
      const response = await api.products({
        search: query,
        categoryId: categoryId || undefined,
        page: nextPage,
        size,
        sortBy: 'id',
        sortDir: 'asc'
      });
      setProducts(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
      setPage(nextPage);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(0); }, []);

  function search() {
    load(0);
  }

  function prevPage() {
    if (page <= 0 || loading) return;
    load(page - 1);
  }

  function nextPage() {
    if (loading || page + 1 >= totalPages) return;
    load(page + 1);
  }

  return (
    <div className="grid">
      <section className="home-banners">
        <div className="banner-main card">
          <div className="banner-badge">Mega Firsatlar</div>
          <h1>Super Market Place Deneyimi</h1>
          <p>Bugune ozel secili urunlerde hizli teslimat ve avantajli fiyatlar.</p>
          <div className="row">
            <button className="btn primary" onClick={search}>Kampanyalari Kesfet</button>
            <span className="meta">Ayni gun kargo uygun urunler</span>
          </div>
        </div>
        <div className="banner-side card">
          <div className="mini-banner">
            <strong>Elektronik Haftasi</strong>
            <span>Telefon ve tablette ekstra kuponlar</span>
          </div>
          <div className="mini-banner">
            <strong>Sepette Avantaj</strong>
            <span>Coklu urun aliminda ek indirim</span>
          </div>
        </div>
      </section>

      <div className="card hero">
        <h1 className="h1">Ana Sayfa / Urun Arama</h1>
        <p className="meta">Trend urunleri inceleyin, kategori ve metin arama ile listeyi hizla daraltin.</p>
        <div className="row">
          <input placeholder="Urun ara..." value={query} onChange={(e) => setQuery(e.target.value)} />
          <div className="select-wrap">
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">Tum Kategoriler</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
            </select>
            <span className="select-arrow">▼</span>
          </div>
          <button className="btn primary" onClick={search}>Ara</button>
        </div>
        {error && <div className="error">{error}</div>}
      </div>

      {!!featuredProducts.length && (
        <section className="card">
          <div className="space">
            <h2 className="section-title">One Cikan Urunler</h2>
            <span className="meta">Trend ve yuksek talepli urunler</span>
          </div>
          <div className="featured-grid">
            {featuredProducts.map((p) => {
              const displayProduct = presentProduct(p);
              return (
                <article key={`featured-${p.id}`} className="featured-item">
                  <img className="product-image" src={displayProduct.displayImageUrl} alt={displayProduct.displayName} />
                  <h3>{displayProduct.displayName}</h3>
                  <strong>{p.price} TL</strong>
                  <button
                    className={`btn ${p.stock > 0 ? 'primary' : 'soldout'}`}
                    onClick={() => onQuickAdd(displayProduct)}
                    disabled={p.stock <= 0}
                  >
                    {p.stock <= 0 ? 'Tukendi' : 'Sepete Ekle'}
                  </button>
                </article>
              );
            })}
          </div>
        </section>
      )}

      {!!dealProducts.length && (
        <section className="card deal-strip">
          <h2 className="section-title">Gunun Firsatlari</h2>
          <div className="deal-list">
            {dealProducts.map((p) => {
              const displayProduct = presentProduct(p);
              return (
                <button key={`deal-${p.id}`} className="deal-chip" onClick={() => onQuickAdd(displayProduct)} disabled={p.stock <= 0}>
                  <span>{displayProduct.displayName}</span>
                  <strong>{p.price} TL</strong>
                </button>
              );
            })}
          </div>
        </section>
      )}

      <div className="grid products">
        {loading && <div className="meta">Yukleniyor...</div>}
        {products.map((p) => {
          const displayProduct = presentProduct(p);
          return (
          <article className="card product-card" key={p.id}>
            <img className="product-image" src={displayProduct.displayImageUrl} alt={displayProduct.displayName} />
            <div className="tag">Stok: {p.stock}</div>
            <h3>{displayProduct.displayName}</h3>
            <div className="meta product-meta-line">{p.brand} | {categoryLabel(p.categoryId)} | Seller #{p.sellerId}</div>
            <p className="meta">{p.description?.slice(0, 80)}</p>
            <div className="space">
              <strong>{p.price} TL</strong>
              <div className="row">
                <Link className="btn" to={`/products/${p.id}`}>Detay</Link>
                <button
                  className={`btn ${p.stock > 0 ? 'primary' : 'soldout'}`}
                  onClick={() => onQuickAdd(displayProduct)}
                  disabled={p.stock <= 0}
                >
                  {p.stock <= 0 ? 'Tukendi' : 'Sepete Ekle'}
                </button>
              </div>
            </div>
          </article>
          );
        })}
      </div>
      <div className="space pagination-meta-bottom">
        <div className="meta">Toplam {totalElements} urun</div>
        <div className="meta">Sayfa {totalPages ? page + 1 : 0}/{totalPages}</div>
      </div>
      <div className="row pagination pagination-bottom">
        <button className="btn" disabled={loading || page === 0} onClick={prevPage}>Onceki</button>
        <button className="btn" disabled={loading || totalPages === 0 || page + 1 >= totalPages} onClick={nextPage}>Sonraki</button>
      </div>
    </div>
  );
}
