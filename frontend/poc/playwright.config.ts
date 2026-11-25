import { defineConfig, devices } from "@playwright/test";

// Os percursos das jornadas, num navegador de verdade.
//   npm run test:e2e        contra o build em modo SIMULADO (sem backend), porta 4174
//   npm run test:e2e:real   contra o SERVIÇO REAL em dev (VITE_API), porta 5173 (a origem que o CORS de dev libera);
//                           pula os testes @proposta (o serviço ainda não tem) e @simulado (dependem do simulado)
// PW_CHROMIUM: caminho de um Chromium já instalado (opcional; sem ele, o do Playwright).
// PW_PORTA: porta do servidor de teste; mude para rodar dois projetos ao mesmo tempo.
const REAL = process.env.PW_MODO === "real";
const PORTA = process.env.PW_PORTA ?? (REAL ? "5173" : "4174");

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
    command: `npm run ${REAL ? "build" : "build:falso"} && npx vite preview --host 127.0.0.1 --port ${PORTA} --strictPort`,
    url: `http://127.0.0.1:${PORTA}`,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
