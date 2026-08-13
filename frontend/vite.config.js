import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // El puerto está fijado a propósito: es el origen que los cinco servicios
    // declaran en `seguridad.origenes-permitidos`. Si Vite eligiera otro al
    // estar ocupado, el navegador bloquearía todas las llamadas por CORS.
    strictPort: true
  }
})
