import { useSyncExternalStore } from "react";

/**
 * Um valor guardado no navegador (localStorage), que as telas leem com um hook e que se atualiza em todas
 * quando muda. Serve para o que o protótipo precisa lembrar entre recargas: persona, recentes, dados simulados.
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
    return () => ouvintes.delete(o);
  }

  return {
    ler: () => valor,
    gravar,
    usar: () => useSyncExternalStore(assinar, () => valor),
  };
}
