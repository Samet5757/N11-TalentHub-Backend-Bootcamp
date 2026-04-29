import React, { useState } from 'react';

export default function CartPage({ cart, onReloadCart, onRemoveItem, onUpdateQty, onApplyCoupon, onRemoveCoupon, onCheckout, isCheckingOut, checkoutState }) {
  const [coupon, setCoupon] = useState('');
  const [cardNumber, setCardNumber] = useState('5528790000000008');

  if (!cart) {
    return <div className="card"><p>Aktif sepet bulunamadi.</p></div>;
  }

  return (
    <div className="grid">
      <div className="card">
        <h1 className="h1">Sepet</h1>
        <div className="meta">Cart ID: {cart.id} | Customer: {cart.customerId}</div>
        <button className="btn" onClick={onReloadCart}>Yenile</button>
      </div>

      <div className="card list">
        {cart.items?.length ? cart.items.map((item) => (
          <div key={item.id} className="space">
            <div>
              <strong>Urun #{item.productId}</strong>
              <div className="meta">Birim: {item.unitPrice} TL | Adet: {item.quantity}</div>
            </div>
            <div className="row">
              <button className="btn" onClick={() => onUpdateQty(item, item.quantity - 1)} disabled={item.quantity <= 1}>-</button>
              <button className="btn" onClick={() => onUpdateQty(item, item.quantity + 1)}>+</button>
              <button className="btn" onClick={() => onRemoveItem(item.id)}>Sil</button>
            </div>
          </div>
        )) : <div className="meta">Sepet bos.</div>}

        <hr />
        <div className="space"><span>Toplam</span><strong>{cart.totalAmount ?? 0} TL</strong></div>
        <div className="space"><span>Indirim</span><strong>{cart.discountAmount ?? 0} TL</strong></div>
        <div className="space"><span>Net</span><strong>{cart.finalAmount ?? cart.totalAmount ?? 0} TL</strong></div>

        <div className="row">
          <input placeholder="Kampanya kodu" value={coupon} onChange={(e) => setCoupon(e.target.value)} />
          <button className="btn" onClick={() => onApplyCoupon(coupon)}>Kupon Uygula</button>
          <button className="btn" onClick={onRemoveCoupon}>Kuponu Kaldir</button>
        </div>
      </div>

      <div className="card grid">
        <h2>Checkout</h2>
        <label>
          Kart Numarasi
          <input value={cardNumber} onChange={(e) => setCardNumber(e.target.value)} />
        </label>
        {checkoutState.error && <div className="error">{checkoutState.error}</div>}
        {checkoutState.ok && <div className="ok">{checkoutState.ok}</div>}
        <button className="btn accent" onClick={() => onCheckout(cardNumber)} disabled={!cart.items?.length || isCheckingOut}>
          {isCheckingOut ? <span className="spinner" /> : null}
          {isCheckingOut ? 'Odeme Aliniyor...' : 'Siparisi Olustur ve Odemeyi Tamamla'}
        </button>
      </div>
    </div>
  );
}
