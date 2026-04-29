import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import { getUserContext } from '../lib/auth';

export default function OrdersPage({ user }) {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedOrderId, setSelectedOrderId] = useState(null);

  const customerId = user?.id || getUserContext()?.userId;
  const statusOptions = useMemo(() => {
    const unique = new Set(orders.map((o) => o.status).filter(Boolean));
    return ['ALL', ...Array.from(unique)];
  }, [orders]);

  useEffect(() => {
    if (!customerId) return;
    setLoading(true);
    setError('');
    api.ordersByCustomer(customerId)
      .then((data) => setOrders(Array.isArray(data) ? data : []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [customerId]);

  const visibleOrders = useMemo(() => {
    const filtered = statusFilter === 'ALL' ? orders : orders.filter((o) => o.status === statusFilter);
    return filtered.sort((a, b) => {
      const diff = new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
      if (diff !== 0) return diff;
      return (b.id || 0) - (a.id || 0);
    });
  }, [orders, statusFilter]);

  function statusLabel(status) {
    const map = {
      PENDING: 'Onay Bekliyor',
      INVENTORY_RESERVED: 'Stok Ayrildi',
      PAYMENT_PENDING: 'Odeme Bekleniyor',
      PAYMENT_AUTHORIZED: 'Odeme Alindi',
      APPROVED: 'Onaylandi',
      SHIPPED: 'Kargoda',
      DELIVERED: 'Teslim Edildi',
      COMPLETED: 'Tamamlandi',
      FAILED: 'Basarisiz',
      CANCELLED: 'Iptal'
    };
    return map[status] || status;
  }

  return (
    <div className="card">
      <div className="space">
        <h1 className="h1">Siparislerim</h1>
        <select style={{ maxWidth: 220 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          {statusOptions.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'Tum Durumlar' : statusLabel(s)}</option>)}
        </select>
      </div>
      {error && <div className="error">{error}</div>}
      {loading && <div className="meta">Siparisler yukleniyor...</div>}
      <div className="list">
        {visibleOrders.map((o) => (
          <article key={o.id} className="card order-card">
            <div className="space">
              <strong>Siparis #{o.id}</strong>
              <span className={`tag order-tag ${String(o.status || '').toLowerCase()}`}>{statusLabel(o.status)}</span>
            </div>
            <div className="meta">
              {new Date(o.createdAt).toLocaleString('tr-TR')} | Satici #{o.sellerId}
            </div>
            <div className="meta">{o.items?.length || 0} urun | Toplam: {o.finalAmount} TL</div>
            <div style={{ marginTop: 10 }}>
              <button className="btn" onClick={() => setSelectedOrderId(selectedOrderId === o.id ? null : o.id)}>
                {selectedOrderId === o.id ? 'Detayi Gizle' : 'Siparis Detayi'}
              </button>
            </div>
            {selectedOrderId === o.id && (
              <div className="order-detail">
                <div className="meta">Ara Toplam: {o.totalAmount} TL</div>
                <div className="meta">Indirim: {o.discountAmount} TL</div>
                <div className="meta"><strong>Genel Toplam: {o.finalAmount} TL</strong></div>
                <hr />
                <div className="list">
                  {(o.items || []).map((item) => (
                    <div key={item.id || `${o.id}-${item.productId}`} className="space">
                      <span>Urun #{item.productId}</span>
                      <span>{item.quantity} x {item.unitPrice} TL</span>
                    </div>
                  ))}
                  {!(o.items || []).length && <div className="meta">Bu sipariste urun satiri bulunamadi.</div>}
                </div>
              </div>
            )}
          </article>
        ))}
        {!loading && !visibleOrders.length && <div className="meta">Henuz siparis yok.</div>}
      </div>
    </div>
  );
}
