import type { ReactNode } from "react";

/** Faixa de números/fatos-chave no topo de uma tela de detalhe. */
export function Indicadores({ itens }: { itens: { rotulo: string; valor: ReactNode }[] }) {
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {itens.map((i) => (
        <div key={i.rotulo} className="rounded-xl border border-neutral-800 bg-neutral-900/60 px-4 py-3">
          <div className="text-xs uppercase tracking-wide text-neutral-500">{i.rotulo}</div>
          <div className="mt-1 text-base font-semibold tabular-nums text-neutral-100">{i.valor}</div>
        </div>
      ))}
    </div>
  );
}
