import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { viteSingleFile } from "vite-plugin-singlefile";

export default defineConfig({
  // Single-file build: all JS/CSS inlined into index.html, so the page
  // needs zero /assets/* sub-requests (some networks block those).
  plugins: [react(), viteSingleFile()],
  server: {
    host: "localhost",
    port: 5173,
    // Dev-only: forward /api/* to Spring Boot so `npm run dev`
    // works without CORS issues while backend runs on :8080.
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true }
    }
  },
  preview: { host: "localhost", port: 4173 },
  build: {
    // Merged deployment: React build output goes straight into
    // Spring Boot's static resources, served on the same port (8080).
    outDir: "../src/main/resources/static",
    emptyOutDir: true
  }
});