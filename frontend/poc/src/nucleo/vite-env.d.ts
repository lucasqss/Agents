/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** "falso": o serviço simulado no navegador (npm run dev:falso). */
  readonly VITE_MODO?: string;
  /** URL do serviço de verdade (padrão http://localhost:8080). */
  readonly VITE_API?: string;
}
