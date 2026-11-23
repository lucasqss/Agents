// Análise estática da POC: TypeScript com tipos, regras de hooks do React (incluindo as do React Compiler) e
// acessibilidade de JSX. Regra que falha se corrige no código, não aqui.
import js from "@eslint/js";
import jsxA11y from "eslint-plugin-jsx-a11y";
import reactHooks from "eslint-plugin-react-hooks";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist", "node_modules", "playwright-report", "test-results", "src/api/esquema.d.ts"] },
  {
    files: ["**/*.{ts,tsx}"],
    extends: [js.configs.recommended, ...tseslint.configs.recommendedTypeChecked, reactHooks.configs.flat.recommended, jsxA11y.flatConfigs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
    rules: {
      "@typescript-eslint/no-unused-vars": ["error", { varsIgnorePattern: "^_", ignoreRestSiblings: true }],
      "@typescript-eslint/no-misused-promises": ["error", { checksVoidReturn: { attributes: false } }],
    },
  },
  {
    // Os componentes do shadcn/ui são gerados pelo CLI: valem as regras de tipo e de hooks, não reescrevemos o estilo deles.
    files: ["src/componentes/ui/**"],
    rules: { "jsx-a11y/heading-has-content": "off", "jsx-a11y/anchor-has-content": "off" },
  },
  { files: ["eslint.config.js"], extends: [tseslint.configs.disableTypeChecked] },
);
