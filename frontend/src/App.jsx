import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import HomePage from './pages/HomePage';
import ProductPage from './pages/ProductPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import { api } from './lib/api';
import { getToken, getUserContext } from './lib/auth';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

function Protected({ token, children }) {
  return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const navigate = useNavigate();
  const [token, setTokenState] = useState(getToken());
  const [user, setUser] = useState(null);
  const [cart, setCart] = useState(null);
  const [checkoutState, setCheckoutState] = useState({ error: '', ok: '' });
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const context = useMemo(() => getUserContext(), [token]);

  async function loadUser() {
    if (!getToken()) return;
    try {
      const me = await api.me();
      setUser(me);
    } catch {
      setUser(null);
      setTokenState(null);
    }
  }

  async function ensureCart(userId) {
    const carts = await api.cartsByCustomer(userId);
    if (carts.length) {
      setCart(carts[0]);
      return carts[0];
    }
    const created = await api.createCart(userId);
    setCart(created);
    return created;
  }

  useEffect(() => {
    setTokenState(getToken());
    loadUser();
  }, []);

  useEffect(() => {
    if (!context?.userId) return;
    ensureCart(context.userId).catch(() => {});
  }, [context?.userId]);

  async function reloadCart() {
    if (!context?.userId) return;
    await ensureCart(context.userId);
  }

  async function addToCart(product) {
    try {
      if (!product || product.stock <= 0) {
        toast.info('Bu urun tukendi.');
        return;
      }
      const activeCart = cart || (await ensureCart(context.userId));
      const existing = activeCart.items?.find((i) => i.productId === product.id);
      const next = existing
        ? await api.updateItem(activeCart.id, existing.id, { productId: product.id, quantity: existing.quantity + 1, unitPrice: product.price })
        : await api.addItem(activeCart.id, { productId: product.id, quantity: 1, unitPrice: product.price });
      setCart(next);
      toast.success('Urun sepete eklendi.');
      navigate('/cart');
    } catch (err) {
      if (err.message?.includes('Optimistic') || err.message?.includes('stok bilgisi')) {
        toast.error('Urun stok bilgisi guncellendi, lutfen sayfayi yenileyin');
      } else {
        toast.error(err.message || 'Sepete eklenirken hata olustu');
      }
      setCheckoutState({ error: err.message, ok: '' });
    }
  }

  async function checkout(cardNumber) {
    if (!cart?.items?.length || isCheckingOut) return;
    setCheckoutState({ error: '', ok: '' });
    setIsCheckingOut(true);
    try {
      const detailProducts = await Promise.all(cart.items.map((i) => api.product(i.productId)));
      const sellerId = detailProducts[0]?.sellerId;
      const orderPayload = {
        customerId: cart.customerId,
        sellerId,
        totalAmount: cart.totalAmount ?? 0,
        discountAmount: cart.discountAmount ?? 0,
        items: cart.items.map((i) => ({ productId: i.productId, quantity: i.quantity, unitPrice: i.unitPrice }))
      };
      const order = await api.createOrder(orderPayload);
      const key = `checkout-${order.id}-${Date.now()}`;
      const intent = await api.createPaymentIntent({ orderId: order.id, amount: order.finalAmount ?? cart.finalAmount ?? cart.totalAmount }, key);
      const payment = await api.confirmPayment(intent.paymentIntentId, cardNumber, `${key}-confirm`);
      await api.deleteCart(cart.id);
      await ensureCart(context.userId);
      setCheckoutState({ error: '', ok: `Odeme tamamlandi. Payment #${payment.id}` });
      toast.success(`Odeme tamamlandi. Payment #${payment.id}`);
    } catch (err) {
      setCheckoutState({ error: err.message, ok: '' });
      toast.error(err.message || 'Odeme sirasinda hata olustu');
    } finally {
      setIsCheckingOut(false);
    }
  }

  return (
    <Layout user={user} setUser={setUser}>
      <Routes>
        <Route path="/login" element={<LoginPage setUser={setUser} onLoggedIn={() => setTokenState(getToken())} />} />
        <Route path="/" element={<Protected token={token}><HomePage onQuickAdd={addToCart} /></Protected>} />
        <Route path="/products/:id" element={<Protected token={token}><ProductPage onQuickAdd={addToCart} /></Protected>} />
        <Route path="/cart" element={<Protected token={token}><CartPage
          cart={cart}
          onReloadCart={reloadCart}
          onRemoveItem={async (itemId) => { const next = await api.removeItem(cart.id, itemId); setCart(next); }}
          onUpdateQty={async (item, quantity) => { const next = await api.updateItem(cart.id, item.id, { productId: item.productId, quantity, unitPrice: item.unitPrice }); setCart(next); }}
          onApplyCoupon={async (code) => { const next = await api.applyCoupon(cart.id, code); setCart(next); }}
          onRemoveCoupon={async () => { const next = await api.removeCoupon(cart.id); setCart(next); }}
          onCheckout={checkout}
          isCheckingOut={isCheckingOut}
          checkoutState={checkoutState}
        /></Protected>} />
        <Route path="/orders" element={<Protected token={token}><OrdersPage user={user} /></Protected>} />
      </Routes>
      <ToastContainer position="top-right" autoClose={2500} hideProgressBar={false} newestOnTop />
    </Layout>
  );
}
