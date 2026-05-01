import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { api } from '../lib/api';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  function onChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function validate() {
    if (form.firstName.trim().length < 2) return 'Ad en az 2 karakter olmalidir.';
    if (form.lastName.trim().length < 2) return 'Soyad en az 2 karakter olmalidir.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return 'Gecerli bir e-posta girin.';
    if (form.password.length < 6) return 'Sifre en az 6 karakter olmalidir.';
    if (form.password !== form.confirmPassword) return 'Sifre tekrari eslesmiyor.';
    return '';
  }

  async function submit(e) {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setIsSubmitting(true);
    setError('');
    try {
      await api.register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim().toLowerCase(),
        password: form.password
      });
      toast.success('Kayit basarili, giris yapabilirsiniz');
      navigate('/login');
    } catch (err) {
      setError(err.message || 'Kayit olusturulamadi');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="card login-card">
      <div className="login-badge">New Account</div>
      <h1 className="h1">Kayit Ol</h1>
      <p className="meta">Hemen hesap olusturun ve alisverise guvenli sekilde baslayin.</p>
      <form onSubmit={submit} className="grid">
        <div className="row">
          <label style={{ flex: 1 }}>
            Ad
            <input name="firstName" value={form.firstName} onChange={onChange} required />
          </label>
          <label style={{ flex: 1 }}>
            Soyad
            <input name="lastName" value={form.lastName} onChange={onChange} required />
          </label>
        </div>
        <label>
          E-posta
          <input type="email" name="email" value={form.email} onChange={onChange} required />
        </label>
        <label>
          Sifre
          <input type="password" name="password" value={form.password} onChange={onChange} required />
        </label>
        <label>
          Sifre Tekrar
          <input type="password" name="confirmPassword" value={form.confirmPassword} onChange={onChange} required />
        </label>
        {error && <div className="error">{error}</div>}
        <button className="btn primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Kayit Olusturuluyor...' : 'Kayit Ol'}
        </button>
        <div className="meta">Zaten hesabiniz var mi? <Link to="/login" className="link-inline">Giris Yap</Link></div>
      </form>
    </div>
  );
}
