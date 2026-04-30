import React, { useEffect, useMemo, useState } from 'react';
import { api } from '../lib/api';
import { getUserContext } from '../lib/auth';

export default function OrdersPage({ user }) {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedOrderId, setSelectedOrderId] = useState(null);
  const [quickFilter, setQuickFilter] = useState('ALL');

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
    const quickFiltered = quickFilter === 'ALL'
      ? orders
      : orders.filter((o) => {
          if (quickFilter === 'ACTIVE') return ['PENDING', 'INVENTORY_RESERVED', 'PAYMENT_PENDING', 'PAYMENT_AUTHORIZED', 'APPROVED', 'SHIPPED'].includes(o.status);
          if (quickFilter === 'DONE') return ['DELIVERED', 'COMPLETED'].includes(o.status);
          if (quickFilter === 'CANCELLED') return ['FAILED', 'CANCELLED'].includes(o.status);
          return true;
        });
    const filtered = statusFilter === 'ALL' ? quickFiltered : quickFiltered.filter((o) => o.status === statusFilter);
    return filtered.sort((a, b) => {
      const diff = new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
      if (diff !== 0) return diff;
      return (b.id || 0) - (a.id || 0);
    });
  }, [orders, statusFilter, quickFilter]);

  const summary = useMemo(() => ({
    all: orders.length,
    active: orders.filter((o) => ['PENDING', 'INVENTORY_RESERVED', 'PAYMENT_PENDING', 'PAYMENT_AUTHORIZED', 'APPROVED', 'SHIPPED'].includes(o.status)).length,
    done: orders.filter((o) => ['DELIVERED', 'COMPLETED'].includes(o.status)).length,
    cancelled: orders.filter((o) => ['FAILED', 'CANCELLED'].includes(o.status)).length
  }), [orders]);

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

  function statusStage(status) {
    if (['FAILED', 'CANCELLED'].includes(status)) return 'Akis Sonlandi';
    if (['DELIVERED', 'COMPLETED'].includes(status)) return 'Teslimat Tamamlandi';
    if (['SHIPPED'].includes(status)) return 'Kargoda';
    if (['APPROVED', 'PAYMENT_AUTHORIZED'].includes(status)) return 'Hazirlaniyor';
    return 'Islemde';
  }

  const tl = (value) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY', maximumFractionDigits: 2 }).format(value ?? 0);
  const canCancel = (status) => ['PENDING', 'INVENTORY_RESERVED', 'PAYMENT_PENDING', 'PAYMENT_AUTHORIZED', 'APPROVED'].includes(status);

  async function handleCancel(orderId) {
    try {
      const updated = await api.cancelOrder(orderId);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? updated : o)));
    } catch (e) {
      setError(e.message || 'Siparis iptal edilemedi');
    }
  }

  return (
    <div className="card">
      <div className="space">
        <h1 className="h1">Siparislerim</h1>
        <select style={{ maxWidth: 220 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          {statusOptions.map((s) => <option key={s} value={s}>{s === 'ALL' ? 'Tum Durumlar' : statusLabel(s)}</option>)}
        </select>
      </div>
      <div className="row order-filters">
        <button className={`btn ${quickFilter === 'ALL' ? 'primary' : ''}`} onClick={() => setQuickFilter('ALL')}>Tumu ({summary.all})</button>
        <button className={`btn ${quickFilter === 'ACTIVE' ? 'primary' : ''}`} onClick={() => setQuickFilter('ACTIVE')}>Aktif ({summary.active})</button>
        <button className={`btn ${quickFilter === 'DONE' ? 'primary' : ''}`} onClick={() => setQuickFilter('DONE')}>Tamamlanan ({summary.done})</button>
        <button className={`btn ${quickFilter === 'CANCELLED' ? 'primary' : ''}`} onClick={() => setQuickFilter('CANCELLED')}>Iptal/Basarisiz ({summary.cancelled})</button>
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
            <div className="meta">{o.items?.length || 0} urun | Toplam: {tl(o.finalAmount)}</div>
            <div className="meta">Durum Akisi: {statusStage(o.status)}</div>
            {o.status === 'COMPLETED' && (
              <div className="meta">Bilgi: Siparis onay/fatura e-postasi sistem tarafinda otomatik gonderilir.</div>
            )}
            <div style={{ marginTop: 10 }}>
              <button className="btn" onClick={() => setSelectedOrderId(selectedOrderId === o.id ? null : o.id)}>
                {selectedOrderId === o.id ? 'Detayi Gizle' : 'Siparis Detayi'}
              </button>
              {canCancel(o.status) && (
                <button className="btn" style={{ marginLeft: 8 }} onClick={() => handleCancel(o.id)}>
                  Siparisi Iptal Et
                </button>
              )}
            </div>
            {selectedOrderId === o.id && (
              <div className="order-detail">
                <div className="meta">Ara Toplam: {tl(o.totalAmount)}</div>
                <div className="meta">Indirim: {tl(o.discountAmount)}</div>
                <div className="meta"><strong>Genel Toplam: {tl(o.finalAmount)}</strong></div>
                <hr />
                <div className="list">
                  {(o.items || []).map((item) => (
                    <div key={item.id || `${o.id}-${item.productId}`} className="space">
                      <span>Urun #{item.productId}</span>
                      <span>{item.quantity} x {tl(item.unitPrice)}</span>
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
