import { useSyncExternalStore } from "react";

/**
 * Um valor guardado no navegador (localStorage), que as telas leem com o hook `useValor()` e que se atualiza em todas quando
 * muda. Para o que o protótipo lembra entre recargas: persona, recentes, dados simulados.
 */
export function guardado<T>(chave: string, padrao: T) {
  const ouvintes = new Set<() => void>();
  let valor: T = ler();

  function ler(): T {
    try {
      const texto = localStorage.getItem(chave);
      return texto === null ? padrao : (JSON.parse(texto) as T);
    } catch {
      return padrao;
    }
  }

  function gravar(novo: T) {
    valor = novo;
    try {
      localStorage.setItem(chave, JSON.stringify(novo));
    } catch {
      /* navegador sem armazenamento: segue só em memória */
    }
    ouvintes.forEach((o) => o());
  }

  function assinar(o: () => void) {
    ouvintes.add(o);
    return () => void ouvintes.delete(o);
  }

  return { ler: () => valor, gravar, useValor: () => useSyncExternalStore(assinar, () => valor) };
}
