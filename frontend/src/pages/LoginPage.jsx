import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { setToken } from '../lib/auth';

export default function LoginPage({ setUser, onLoggedIn }) {
  const [username, setUsername] = useState('customer1');
  const [password, setPassword] = useState('pass123');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function submit(e) {
    e.preventDefault();
    setError('');
    try {
      const res = await api.login(username, password);
      setToken(res.token);
      onLoggedIn?.();
      const me = await api.me();
      setUser(me);
      navigate(me?.role === 'ADMIN' ? '/admin' : me?.role === 'SELLER' ? '/seller' : '/');
    } catch (err) {
      const message = err?.message || '';
      if (message.includes('401') || message.toLowerCase().includes('unauthorized')) {
        setError('Kullanici adi veya sifre hatali.');
      } else {
        setError(message || 'Giris yapilamadi. Lutfen tekrar deneyin.');
      }
    }
  }

  return (
    <div className="card login-card">
      <div className="login-badge">Secure Session</div>
      <h1 className="h1">Login</h1>
      <p className="meta">Guvenli giris ile sepet ve siparislerinize erisin.</p>
      <form onSubmit={submit} className="grid">
        <label>
          Kullanici Adi
          <input value={username} onChange={(e) => setUsername(e.target.value)} required />
        </label>
        <label>
          Sifre
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        {error && <div className="error">{error}</div>}
        <button className="btn primary" type="submit">Giris Yap</button>
        <div className="meta">Hesabiniz yok mu? <Link to="/register" className="link-inline">Kayit Ol</Link></div>
      </form>
    </div>
  );
}
