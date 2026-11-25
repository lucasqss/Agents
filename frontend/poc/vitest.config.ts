import { mergeConfig } from "vite";
import { defineConfig } from "vitest/config";
import vite from "./vite.config";

// Testes de unidade do núcleo e dos componentes (jsdom). As jornadas são provadas no navegador, pelo Playwright (e2e/).
export default mergeConfig(vite, defineConfig({
  test: { environment: "jsdom", include: ["src/**/*.test.{ts,tsx}"], restoreMocks: true, globals: true },
}));
