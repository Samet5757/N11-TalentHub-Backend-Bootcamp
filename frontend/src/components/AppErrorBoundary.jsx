import React from 'react';

export default class AppErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: '' };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error: error?.message || 'Beklenmeyen bir hata olustu.' };
  }

  componentDidCatch(error) {
    // Keep console trace for debugging while preventing blank screen for users.
    // eslint-disable-next-line no-console
    console.error('UI crash captured:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="card">
          <h2 className="section-title">Ekran Yuklenemedi</h2>
          <p className="meta">Beklenmeyen bir sorun yakalandi. Sayfayi yenileyip tekrar deneyin.</p>
          <div className="error">{this.state.error}</div>
        </div>
      );
    }

    return this.props.children;
  }
}
