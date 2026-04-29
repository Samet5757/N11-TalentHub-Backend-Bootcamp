import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/products': 'http://localhost:8080',
      '/carts': 'http://localhost:8080',
      '/orders': 'http://localhost:8080',
      '/payments': 'http://localhost:8080'
    }
  }
});
