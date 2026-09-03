import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

declare const process: { cwd: () => string; env: Record<string, string | undefined> };
const root = process.cwd();
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  root,
  plugins: [react()],
  // `public/` es la ubicación por defecto de Vite: su contenido se sirve desde la raíz
  // en desarrollo y se copia tal cual al build. Las fuentes viven en `src/assets` para
  // que Vite las empaquete con hash, que es lo que permite cachearlas de verdad.
  server: {
    port: 8093,
    strictPort: true,
    proxy: {
      // En Docker el backend no publica 8080 al host; se usa el mismo reverse proxy
      // que sirve la aplicación. El origen del navegador se mantiene, así que no se
      // desactiva CORS en la aplicación.
      '/api': { target: apiProxyTarget, changeOrigin: true },
      '/uploads': { target: apiProxyTarget, changeOrigin: true },
    },
  },
  build: {
    outDir: `${root}/dist`,
    emptyOutDir: true,
  },
});
