import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../lib/api';
import { categoryLabel } from '../lib/categories';
import { presentProduct } from '../lib/productPresentation';

export default function ProductPage({ onQuickAdd }) {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.product(id).then(setProduct).catch((e) => setError(e.message));
  }, [id]);

  if (error) return <div className="error">{error}</div>;
  if (!product) return <div className="meta">Yukleniyor...</div>;
  const displayProduct = presentProduct(product);

  return (
    <div className="card">
      <h1 className="h1">{displayProduct.displayName}</h1>
      <div className="grid product-detail-grid">
        <img className="product-image" style={{ height: 320 }} src={displayProduct.displayImageUrl} alt={displayProduct.displayName} />
        <div className="grid">
          <div><strong>Fiyat:</strong> {product.price} TL</div>
          <div><strong>Marka:</strong> {product.brand}</div>
          <div><strong>Kategori:</strong> {categoryLabel(product.categoryId)}</div>
          <div><strong>Stok:</strong> {product.stock}</div>
          <div><strong>Seller:</strong> #{product.sellerId}</div>
          <div className="meta">{product.description}</div>
          <button
            className={`btn ${product.stock > 0 ? 'primary' : 'soldout'}`}
            onClick={() => onQuickAdd(product)}
            disabled={product.stock <= 0}
          >
            {product.stock <= 0 ? 'Tukendi' : 'Sepete Ekle'}
          </button>
        </div>
      </div>
    </div>
  );
}
