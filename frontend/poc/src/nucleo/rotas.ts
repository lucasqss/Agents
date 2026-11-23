import { useSyncExternalStore } from "react";

/**
 * Rotas por #hash: funcionam abertas de qualquer lugar (inclusive num site estático), sem roteador.
 * "#/processo/12?aberto=1" -> { partes: ["processo", "12"], busca: URLSearchParams("aberto=1") }.
 */
function assinar(o: () => void) {
  window.addEventListener("hashchange", o);
  return () => window.removeEventListener("hashchange", o);
}

export function useRota() {
  const hash = useSyncExternalStore(assinar, () => location.hash);
  const [caminho = "", busca = ""] = hash.replace(/^#/, "").split("?");
  return {
    partes: (caminho || "/").split("/").filter(Boolean).map(decodeURIComponent),
    busca: new URLSearchParams(busca),
  };
}

/** Navega para outra tela: ir("/processo/12"). */
export function ir(destino: string) {
  location.hash = destino;
}
