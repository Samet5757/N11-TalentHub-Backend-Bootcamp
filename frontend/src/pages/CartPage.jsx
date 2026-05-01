import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function CartPage({ cart, onReloadCart, onRemoveItem, onUpdateQty, onApplyCoupon, onRemoveCoupon, onCheckout, isCheckingOut, checkoutState, productIds = [], fetchProductById }) {
  const [coupon, setCoupon] = useState('');
  const [cardNumber, setCardNumber] = useState('5528790000000008');
  const [cardHolderName, setCardHolderName] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [productNames, setProductNames] = useState({});
  const cleanedCard = cardNumber.replace(/\s+/g, '');
  const cleanedCvv = cvv.replace(/\D+/g, '');
  const normalizedExpiry = expiry.replace(/\s+/g, '');
  const isExpiryValid = /^(0[1-9]|1[0-2])\/\d{2}$/.test(normalizedExpiry);
  const canCheckout = !!cart.items?.length
    && !isCheckingOut
    && cleanedCard.length >= 12
    && cardHolderName.trim().length >= 3
    && isExpiryValid
    && (cleanedCvv.length === 3 || cleanedCvv.length === 4);
  const hasCoupon = !!cart.couponCode;
  const checkoutError = checkoutState.error?.toLowerCase().includes('stok')
    ? 'Stok yetersiz. Bazi urunlerin adedini dusurup tekrar deneyin.'
    : checkoutState.error;

  const tl = (value) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(value ?? 0);

  useEffect(() => {
    let isMounted = true;
    async function fillNames() {
      if (!fetchProductById || !productIds.length) return;
      const missingIds = productIds.filter((id) => !productNames[id]);
      if (!missingIds.length) return;
      try {
        const products = await Promise.all(missingIds.map((id) => fetchProductById(id)));
        if (!isMounted) return;
        const mapped = {};
        products.forEach((p) => {
          if (p?.id && (p?.name || p?.title)) mapped[p.id] = p.name || p.title;
        });
        setProductNames((prev) => ({ ...prev, ...mapped }));
      } catch {
        // Best effort: if product fetch fails, fallback label remains.
      }
    }
    fillNames();
    return () => { isMounted = false; };
  }, [fetchProductById, productIds, productNames]);

  if (!cart) {
    return <div className="card"><p>Aktif sepet bulunamadi.</p></div>;
  }

  return (
    <div className="grid">
      <div className="card">
        <h1 className="h1">Sepet</h1>
        <button className="btn" onClick={onReloadCart}>Yenile</button>
      </div>

      <div className="card list">
        {cart.items?.length ? cart.items.map((item) => (
          <div key={item.id} className="space">
            <div>
              <strong>{productNames[item.productId] || `Urun #${item.productId}`}</strong>
              <div className="meta">Birim: {tl(item.unitPrice)} | Adet: {item.quantity}</div>
            </div>
            <div className="row">
              <button className="btn" onClick={() => onUpdateQty(item, item.quantity - 1)} disabled={item.quantity <= 1}>-</button>
              <button className="btn" onClick={() => onUpdateQty(item, item.quantity + 1)}>+</button>
              <button className="btn" onClick={() => onRemoveItem(item.id)}>Sil</button>
            </div>
          </div>
        )) : (
          <div className="empty-block">
            <div className="meta">Sepetiniz su an bos.</div>
            <Link className="btn primary" to="/">Alisverise Basla</Link>
          </div>
        )}

        <hr />
        <div className="space"><span>Toplam</span><strong>{tl(cart.totalAmount)}</strong></div>
        <div className="space"><span>Indirim</span><strong>{tl(cart.discountAmount)}</strong></div>
        <div className="space"><span>Net</span><strong>{tl(cart.finalAmount ?? cart.totalAmount)}</strong></div>

        <div className="row">
          <input placeholder="Kampanya kodu" value={coupon} onChange={(e) => setCoupon(e.target.value)} />
          <button className="btn" onClick={() => onApplyCoupon(coupon.trim())} disabled={!coupon.trim()}>Kupon Uygula</button>
          <button className="btn" onClick={onRemoveCoupon} disabled={!hasCoupon}>Kuponu Kaldir</button>
        </div>
      </div>

      <div className="card grid">
        <h2>Checkout</h2>
        <label>
          Kart Uzerindeki Isim
          <input value={cardHolderName} onChange={(e) => setCardHolderName(e.target.value)} placeholder="Ad Soyad" />
        </label>
        <label>
          Kart Numarasi
          <input value={cardNumber} onChange={(e) => setCardNumber(e.target.value)} />
        </label>
        <div className="row">
          <label style={{ flex: 1 }}>
            Son Kullanma Tarihi (MM/YY)
            <input value={expiry} onChange={(e) => setExpiry(e.target.value)} placeholder="12/29" maxLength={5} />
          </label>
          <label style={{ width: 140 }}>
            CVV
            <input value={cvv} onChange={(e) => setCvv(e.target.value.replace(/\D+/g, ''))} maxLength={4} placeholder="123" />
          </label>
        </div>
        <div className="meta">Kart no min 12 hane, son kullanma MM/YY, CVV 3-4 hane olmalidir.</div>
        {checkoutError && <div className="error">{checkoutError}</div>}
        {checkoutState.ok && <div className="ok">{checkoutState.ok}</div>}
        <button className="btn accent" onClick={() => onCheckout({
          cardHolderName: cardHolderName.trim(),
          cardNumber: cleanedCard,
          expiry: normalizedExpiry,
          cvv: cleanedCvv
        })} disabled={!canCheckout}>
          {isCheckingOut ? <span className="spinner" /> : null}
          {isCheckingOut ? 'Odeme Aliniyor...' : 'Siparisi Olustur ve Odemeyi Tamamla'}
        </button>
      </div>
    </div>
  );
}
