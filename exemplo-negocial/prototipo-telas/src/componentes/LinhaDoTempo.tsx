import type { ReactNode } from "react";

/** Histórico em ordem (o mais recente em cima, se a tela mandar assim). */
export function LinhaDoTempo({ itens }: { itens: { chave: string; titulo: ReactNode; detalhe?: ReactNode; quando?: string; nota?: string }[] }) {
  return (
    <ol className="relative space-y-5 border-l border-neutral-800 pl-6">
      {itens.map((i) => (
        <li key={i.chave} className="relative">
          <span aria-hidden className="absolute -left-[29px] top-1.5 size-2.5 rounded-full bg-blue-500 ring-4 ring-neutral-900" />
          <div className="flex flex-wrap items-baseline gap-x-2 text-sm">
            <span className="font-medium text-neutral-100">{i.titulo}</span>
            {i.quando && <span className="text-xs text-neutral-500">{i.quando}</span>}
          </div>
          {i.detalhe && <div className="mt-1.5 flex flex-wrap items-center gap-2 text-sm text-neutral-400">{i.detalhe}</div>}
          {i.nota && <p className="mt-2 rounded-lg border border-neutral-800 bg-neutral-950 px-3 py-2 text-sm text-neutral-300">{i.nota}</p>}
        </li>
      ))}
    </ol>
  );
}
