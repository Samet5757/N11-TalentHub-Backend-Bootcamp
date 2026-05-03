import React, { useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import HomePage from './pages/HomePage';
import ProductPage from './pages/ProductPage';
import CartPage from './pages/CartPage';
import OrdersPage from './pages/OrdersPage';
import AdminDashboard from './pages/AdminDashboard';
import SellerDashboard from './pages/SellerDashboard';
import AppErrorBoundary from './components/AppErrorBoundary';
import { api } from './lib/api';
import { getToken, getUserContext, onTokenChange } from './lib/auth';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

function Protected({ token, children }) {
  return token ? children : <Navigate to="/login" replace />;
}

function AdminProtected({ token, user, children }) {
  if (!token) return <Navigate to="/login" replace />;
  if (!user) return <div className="meta">Yukleniyor...</div>;
  if (!user || user.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

function CustomerProtected({ token, user, children }) {
  if (!token) return <Navigate to="/login" replace />;
  if (!user) return <div className="meta">Yukleniyor...</div>;
  if (user.role !== 'CUSTOMER') return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;
  return children;
}

function SellerProtected({ token, user, children }) {
  if (!token) return <Navigate to="/login" replace />;
  if (!user) return <div className="meta">Yukleniyor...</div>;
  if (user.role !== 'SELLER') return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;
  return children;
}

export default function App() {
  const navigate = useNavigate();
  const location = useLocation();
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
      return me;
    } catch {
      setUser(null);
      setTokenState(null);
      return null;
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
    const syncAuth = async () => {
      const nextToken = getToken();
      setTokenState(nextToken);
      if (!nextToken) {
        setUser(null);
        setCart(null);
        if (window.location.pathname !== '/register') {
          navigate('/login', { replace: true });
        }
        return;
      }
      const me = await loadUser();
      if (me?.role === 'ADMIN') {
        setCart(null);
      }
    };

    const unsubscribe = onTokenChange(syncAuth);
    syncAuth();
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (!context?.userId || user?.role !== 'CUSTOMER') return;
    ensureCart(context.userId).catch(() => {});
  }, [context?.userId, user?.role]);

  useEffect(() => {
    if (!token || !user) return;
    if (user.role === 'ADMIN' && (location.pathname === '/' || location.pathname === '/cart' || location.pathname === '/orders')) {
      navigate('/admin', { replace: true });
      return;
    }
    if (user.role === 'SELLER' && (location.pathname === '/' || location.pathname === '/cart' || location.pathname === '/orders')) {
      navigate('/seller', { replace: true });
      return;
    }
    if (user.role === 'CUSTOMER' && location.pathname === '/admin') {
      navigate('/', { replace: true });
    }
    if ((user.role === 'CUSTOMER' || user.role === 'ADMIN') && location.pathname === '/seller') {
      navigate(user.role === 'ADMIN' ? '/admin' : '/', { replace: true });
    }
  }, [token, user, location.pathname, navigate]);

  async function reloadCart() {
    if (!context?.userId || user?.role !== 'CUSTOMER') return;
    await ensureCart(context.userId);
  }

  async function addToCart(product) {
    if (user?.role !== 'CUSTOMER') {
      toast.info('Sepet ve checkout sadece musteri hesabi icin aciktir.');
      return;
    }
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
    } catch (err) {
      if (err.message?.includes('Optimistic') || err.message?.includes('stok bilgisi')) {
        toast.error('Urun stok bilgisi guncellendi, lutfen sayfayi yenileyin');
      } else {
        toast.error(err.message || 'Sepete eklenirken hata olustu');
      }
      setCheckoutState({ error: err.message, ok: '' });
    }
  }

  async function checkout(paymentForm) {
    if (user?.role !== 'CUSTOMER') {
      toast.info('Checkout sadece musteri hesabi icin aciktir.');
      return;
    }
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
      const payment = await api.confirmPayment(intent.paymentIntentId, paymentForm.cardNumber, `${key}-confirm`);
      await api.deleteCart(cart.id);
      await ensureCart(context.userId);
      const successMsg = `Odeme tamamlandi. Siparis #${order.id}, Payment #${payment.id}. Siparisiniz tamamlandiginda e-posta bildirimi gonderilecektir.`;
      setCheckoutState({ error: '', ok: successMsg });
      toast.success(`Odeme tamamlandi. Siparis #${order.id}`);
    } catch (err) {
      setCheckoutState({ error: err.message, ok: '' });
      toast.error(err.message || 'Odeme sirasinda hata olustu');
    } finally {
      setIsCheckingOut(false);
    }
  }

  const cartItemCount = user?.role === 'CUSTOMER'
    ? (cart?.items?.reduce((sum, i) => sum + (i.quantity || 0), 0) || 0)
    : 0;
  const cartProductIds = [...new Set((cart?.items || []).map((i) => i.productId).filter(Boolean))];

  return (
    <Layout user={user} setUser={setUser} cartItemCount={cartItemCount}>
      <AppErrorBoundary>
        <Routes>
          <Route path="/login" element={<LoginPage setUser={setUser} onLoggedIn={() => setTokenState(getToken())} />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/" element={<Protected token={token}><HomePage onQuickAdd={addToCart} /></Protected>} />
          <Route path="/products/:id" element={<Protected token={token}><ProductPage onQuickAdd={addToCart} /></Protected>} />
          <Route path="/cart" element={<CustomerProtected token={token} user={user}><CartPage
            cart={cart}
            onReloadCart={reloadCart}
            onRemoveItem={async (itemId) => { const next = await api.removeItem(cart.id, itemId); setCart(next); }}
            onUpdateQty={async (item, quantity) => { const next = await api.updateItem(cart.id, item.id, { productId: item.productId, quantity, unitPrice: item.unitPrice }); setCart(next); }}
            onApplyCoupon={async (code) => { const next = await api.applyCoupon(cart.id, code); setCart(next); }}
            onRemoveCoupon={async () => { const next = await api.removeCoupon(cart.id); setCart(next); }}
            productIds={cartProductIds}
            fetchProductById={api.product}
            onCheckout={checkout}
            isCheckingOut={isCheckingOut}
            checkoutState={checkoutState}
          /></CustomerProtected>} />
          <Route path="/orders" element={<CustomerProtected token={token} user={user}><OrdersPage user={user} /></CustomerProtected>} />
          <Route path="/admin" element={<AdminProtected token={token} user={user}><AdminDashboard /></AdminProtected>} />
          <Route path="/seller" element={<SellerProtected token={token} user={user}><SellerDashboard user={user} /></SellerProtected>} />
        </Routes>
      </AppErrorBoundary>
      <ToastContainer position="top-right" autoClose={2500} hideProgressBar={false} newestOnTop />
    </Layout>
  );
}
