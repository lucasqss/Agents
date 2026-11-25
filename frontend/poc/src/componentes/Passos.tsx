import { Check } from "lucide-react";
import { cn } from "cn";

/**
 * Os passos de um assistente: os feitos com ✓ (clicáveis, para voltar), o atual em destaque, os próximos apagados.
 *   <Passos passos={["Tipo", "Arquivo", "Conferir"]} atual={passo} aoVoltar={setPasso} />
 */
export function Passos({ passos, atual, aoVoltar }: { passos: string[]; atual: number; aoVoltar?: (indice: number) => void }) {
  return (
    <nav aria-label="Passos">
      <ol className="flex flex-wrap items-center gap-2 text-sm">
        {passos.map((nome, i) => {
          const feito = i < atual;
          const conteudo = (
            <>
              <span className={cn("flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                feito ? "bg-primary text-primary-foreground" : i === atual ? "text-andamento ring-1 ring-andamento" : "bg-muted text-muted-foreground")}>
                {feito ? <Check className="size-3.5" aria-hidden /> : i + 1}
              </span>
              <span className={i === atual ? "font-medium" : "text-muted-foreground"}>{nome}</span>
            </>
          );
          return (
            <li key={nome} className="flex items-center gap-2" aria-current={i === atual ? "step" : undefined}>
              {i > 0 && <span aria-hidden className="h-px w-4 bg-border sm:w-8" />}
              {feito && aoVoltar
                ? <button type="button" onClick={() => aoVoltar(i)} className="flex items-center gap-2 rounded-md hover:opacity-80">{conteudo}</button>
                : <span className="flex items-center gap-2">{conteudo}</span>}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
