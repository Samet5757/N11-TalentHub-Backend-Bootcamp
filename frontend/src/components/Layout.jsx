import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { clearToken } from '../lib/auth';
import { api } from '../lib/api';

export default function Layout({ user, children, setUser }) {
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
          <div className="brand" onClick={() => navigate('/')}>n11 Micro UI</div>
          <div className="actions">
            <Link className="btn" to="/">Ana Sayfa</Link>
            <Link className="btn" to="/cart">Sepet</Link>
            <Link className="btn" to="/orders">Siparisler</Link>
            {user ? <button className="btn accent" onClick={handleLogout}>Cikis</button> : <Link className="btn primary" to="/login">Login</Link>}
          </div>
        </div>
      </header>
      <main className="container">{children}</main>
    </>
  );
}
