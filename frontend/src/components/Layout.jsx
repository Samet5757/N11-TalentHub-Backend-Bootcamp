import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { clearToken } from '../lib/auth';
import { api } from '../lib/api';

export default function Layout({ user, children, setUser, cartItemCount = 0 }) {
  const navigate = useNavigate();

  async function handleLogout() {
    try { await api.logout(); } catch {}
    clearToken();
    setUser(null);
    navigate('/login');
  }

  return (
    <>
      <header className="nav">
        <div className="container nav-inner">
          <div className="brand-block" onClick={() => navigate('/')}>
            <div className="brand">n11 Micro UI</div>
            <div className="brand-sub">Kurumsal E-Ticaret Deneyimi</div>
          </div>
          <div className="actions">
            {user?.role === 'ADMIN' ? (
              <>
                <Link className="btn" to="/admin">Dashboard</Link>
                <Link className="btn primary" to="/admin">Admin Paneli</Link>
              </>
            ) : (
              <>
                <Link className="btn" to="/">Ana Sayfa</Link>
                <Link className="btn" to="/cart">Sepet{cartItemCount > 0 ? ` (${cartItemCount})` : ''}</Link>
                <Link className="btn" to="/orders">Siparisler</Link>
              </>
            )}
            {user ? (
              <button className="btn accent" onClick={handleLogout}>Cikis</button>
            ) : (
              <>
                <Link className="btn" to="/register">Kayit Ol</Link>
                <Link className="btn primary" to="/login">Login</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="container">{children}</main>
    </>
  );
}
