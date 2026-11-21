import type { ReactNode } from "react";

/** Bloco da tela, com título e, opcionalmente, ações à direita do título. */
export function Cartao({ titulo, acoes, children }: { titulo?: ReactNode; acoes?: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-xl border border-neutral-800 bg-neutral-900/60 p-5 shadow-sm">
      {(titulo || acoes) && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          {titulo && <h2 className="text-base font-semibold text-neutral-100">{titulo}</h2>}
          {acoes && <div className="flex flex-wrap items-center gap-2">{acoes}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
