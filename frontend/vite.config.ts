import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'

/**
 * Vite configuration for CodeSage AI frontend.
 *
 * Key features:
 * - Tailwind CSS v4 via Vite plugin (replaces PostCSS config)
 * - API proxy to Spring Boot backend (/api → localhost:8080)
 * - Path alias @/ → src/ for clean imports
 */
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],

  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
    },
  },

  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Proxy all /api requests to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      },
      // Proxy Swagger UI
      '/swagger-ui': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/v3/api-docs': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: false,       // Enable in prod debugging if needed
    rollupOptions: {
      output: {
        // Split vendor chunks for better caching
        manualChunks: {
          'react-vendor': ['react', 'react-dom'],
          'router': ['react-router-dom'],
          'axios': ['axios'],
        },
      },
    },
  },
})
