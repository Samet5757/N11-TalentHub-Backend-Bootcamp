import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
      navigate('/');
    } catch (err) {
      setError(err.message);
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
      </form>
    </div>
  );
}
