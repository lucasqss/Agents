import type { ReactNode } from "react";

/** Faixa de fatos-chave no topo de uma tela de detalhe. */
export function Indicadores({ itens }: { itens: { rotulo: string; valor: ReactNode }[] }) {
  return (
    <dl className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {itens.map((i) => (
        <div key={i.rotulo} className="rounded-xl border bg-card px-4 py-3">
          <dt className="text-xs uppercase tracking-wide text-muted-foreground">{i.rotulo}</dt>
          <dd className="mt-1 text-base font-semibold tabular-nums">{i.valor}</dd>
        </div>
      ))}
    </dl>
  );
}
