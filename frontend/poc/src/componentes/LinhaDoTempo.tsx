import type { ReactNode } from "react";

export interface ItemDoTempo {
  chave: string;
  titulo: ReactNode;
  /** Ao lado do título, mais apagado (ex.: "por ana"). */
  complemento?: string;
  quando?: string;
  detalhe?: ReactNode;
  /** Texto livre num quadro (ex.: o motivo). */
  nota?: string;
}

/** Acontecimentos em ordem (o mais recente em cima, se a tela mandar assim). */
export function LinhaDoTempo({ itens }: { itens: ItemDoTempo[] }) {
  return (
    <ol className="relative space-y-5 border-l pl-6">
      {itens.map((i) => (
        <li key={i.chave} className="relative">
          <span aria-hidden className="absolute -left-[29px] top-1.5 size-2.5 rounded-full bg-primary ring-4 ring-card" />
          <div className="flex flex-wrap items-baseline gap-x-2 text-sm">
            <span className="font-medium">{i.titulo}</span>
            {i.complemento && <span className="text-muted-foreground">{i.complemento}</span>}
            {i.quando && <span className="text-xs text-muted-foreground">{i.quando}</span>}
          </div>
          {i.detalhe && <div className="mt-1.5 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">{i.detalhe}</div>}
          {i.nota && <p className="mt-2 rounded-md border bg-muted/40 px-3 py-2 text-sm">{i.nota}</p>}
        </li>
      ))}
    </ol>
  );
}
