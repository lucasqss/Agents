/** Qual serviço as telas usam: "real" (VITE_API, padrão http://localhost:8080) ou "falso" (VITE_MODO=falso: o simulado). */
export const MODO: "real" | "falso" = import.meta.env.VITE_MODO === "falso" ? "falso" : "real";
