import type { ReactNode } from "react";

export interface Coluna<T> {
  titulo: string;
  valor: (linha: T) => ReactNode;
  /** Números alinhados à direita, com algarismos de mesma largura. */
  numero?: boolean;
}

/** Tabela simples; com `aoClicar`, a linha inteira vira link. No celular, rola na horizontal. */
export function Tabela<T>({ colunas, linhas, chave, aoClicar }:
  { colunas: Coluna<T>[]; linhas: T[]; chave: (l: T) => string | number; aoClicar?: (l: T) => void }) {
  return (
    <div className="-mx-5 overflow-x-auto">
      <table className="w-full min-w-[560px] text-sm">
        <thead>
          <tr className="border-b border-neutral-800 text-left text-xs uppercase tracking-wide text-neutral-500">
            {colunas.map((c) => (
              <th key={c.titulo} className={`px-5 py-2.5 font-medium ${c.numero ? "text-right" : ""}`}>{c.titulo}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {linhas.map((l) => (
            <tr key={chave(l)} onClick={aoClicar && (() => aoClicar(l))}
              className={`border-b border-neutral-800/60 last:border-0 ${aoClicar ? "cursor-pointer hover:bg-neutral-800/40" : ""}`}>
              {colunas.map((c) => (
                <td key={c.titulo} className={`px-5 py-3 ${c.numero ? "text-right tabular-nums" : ""}`}>{c.valor(l)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
