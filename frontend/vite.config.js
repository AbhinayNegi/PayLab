import { defineConfig } from 'vite';

export default defineConfig({
  server: { proxy: { '/api': process.env.PAYLAB_PROXY_TARGET || 'http://localhost:8080' } }
});
