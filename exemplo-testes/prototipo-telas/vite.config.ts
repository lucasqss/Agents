import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Porta fixa: o CORS do serviço em %dev (application.properties) libera exatamente esta origem.
// base relativa: o build abre em qualquer subcaminho (GitHub Pages, por exemplo) sem configurar nada.
export default defineConfig({
  base: "./",
  plugins: [react(), tailwindcss()],
  server: { port: 5173, strictPort: true },
  preview: { port: 4173, strictPort: true, host: "127.0.0.1" },
});
