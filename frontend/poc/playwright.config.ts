import { defineConfig, devices } from "@playwright/test";

// Os percursos das jornadas, num navegador de verdade, contra o build em modo simulado (sem backend).
// PW_CHROMIUM: caminho de um Chromium já instalado (opcional; sem ele, o do Playwright).
export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: "http://127.0.0.1:4174",
    ...(process.env.PW_CHROMIUM ? { launchOptions: { executablePath: process.env.PW_CHROMIUM } } : {}),
  },
  projects: [
    { name: "computador", use: { ...devices["Desktop Chrome"] } },
    { name: "celular", use: { ...devices["Pixel 7"] } },
  ],
  webServer: {
    command: "npm run build:falso && npx vite preview --port 4174 --strictPort",
    url: "http://127.0.0.1:4174",
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
