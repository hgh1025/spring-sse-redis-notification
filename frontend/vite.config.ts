import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// /api/* → 백엔드 프록시 (CORS 없이 동일 출처)
export default defineConfig(({ mode }) => {
  const target = loadEnv(mode, process.cwd(), '').VITE_API_TARGET || 'http://localhost:8080';
  return {
    plugins: [react()],
    server: {
      proxy: { '/api': { target, changeOrigin: true, rewrite: (p) => p.replace(/^\/api/, '') } },
    },
  };
});
