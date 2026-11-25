import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./oraculo-e2e", workers: 1, retries: 1, reporter: [["line"]],
  use: { baseURL: "http://127.0.0.1:4176", launchOptions: { executablePath: process.env.PW_CHROMIUM } },
  projects: [{ name: "computador", use: { ...devices["Desktop Chrome"] } }],
  webServer: { command: "npm run build:falso && npx vite preview --port 4176 --strictPort", url: "http://127.0.0.1:4176", reuseExistingServer: false, timeout: 120_000 },
});
