import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

declare const process: { cwd: () => string };
const root = process.cwd();

export default defineConfig({
  root,
  plugins: [react()],
  publicDir: `${root}/../front/assets`,
  server: {
    port: 8093,
    strictPort: true,
    fs: { allow: [root, `${root}/../front`] },
    proxy: {
      // En Docker el backend no publica 8080 al host; el frontend legado en
      // 8092 ya expone el reverse proxy exacto que React debe reutilizar en dev.
      // El origen del navegador es 8093, pero el backend local autoriza 8092.
      // Vite mantiene el origen del navegador por defecto, por eso lo fijamos
      // solo en este proxy interno; no se desactiva CORS en la aplicación.
      '/api': { target: 'http://localhost:8092', headers: { origin: 'http://localhost:8092' } },
      '/uploads': { target: 'http://localhost:8092', headers: { origin: 'http://localhost:8092' } },
    },
  },
  build: {
    outDir: `${root}/dist`,
    emptyOutDir: true,
  },
});
