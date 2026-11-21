import { Check } from "lucide-react";

/** Os passos de um assistente: os já feitos são clicáveis (voltar), o atual em destaque, os próximos apagados. */
export function Passos({ passos, atual, aoIr }: { passos: string[]; atual: number; aoIr?: (indice: number) => void }) {
  return (
    <nav aria-label="Passos">
      <ol className="flex flex-wrap items-center gap-x-2 gap-y-2 text-sm">
        {passos.map((nome, i) => {
          const feito = i < atual;
          const conteudo = (
            <>
              <span className={`flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                feito ? "bg-blue-600 text-white" : i === atual ? "bg-blue-500/15 text-blue-300 ring-1 ring-blue-500" : "bg-neutral-800 text-neutral-500"}`}>
                {feito ? <Check className="size-3.5" aria-hidden /> : i + 1}
              </span>
              <span className={i === atual ? "font-medium text-neutral-100" : feito ? "text-neutral-300" : "text-neutral-500"}>{nome}</span>
            </>
          );
          return (
            <li key={nome} className="flex items-center gap-2" aria-current={i === atual ? "step" : undefined}>
              {i > 0 && <span aria-hidden className="h-px w-4 bg-neutral-700 sm:w-8" />}
              {feito && aoIr
                ? <button type="button" onClick={() => aoIr(i)} className="flex items-center gap-2 rounded-md hover:opacity-80">{conteudo}</button>
                : <span className="flex items-center gap-2">{conteudo}</span>}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
