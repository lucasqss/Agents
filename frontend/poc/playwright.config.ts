import { defineConfig, devices } from "@playwright/test";

const PORTA = process.env.PW_PORTA ?? "4174";

// Os percursos das jornadas, num navegador de verdade, contra o build em modo simulado (sem backend).
// PW_CHROMIUM: caminho de um Chromium já instalado (opcional; sem ele, o do Playwright).
// PW_PORTA: porta do servidor de teste (padrão 4174); mude para rodar dois projetos ao mesmo tempo.
export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: `http://127.0.0.1:${PORTA}`,
    ...(process.env.PW_CHROMIUM ? { launchOptions: { executablePath: process.env.PW_CHROMIUM } } : {}),
  },
  projects: [
    { name: "computador", use: { ...devices["Desktop Chrome"] } },
    { name: "celular", use: { ...devices["Pixel 7"] } },
  ],
  webServer: {
    command: `npm run build:falso && npx vite preview --port ${PORTA} --strictPort`,
    url: `http://127.0.0.1:${PORTA}`,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
