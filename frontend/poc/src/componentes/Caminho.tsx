import { Check } from "lucide-react";
import { cn } from "cn";

export type SituacaoDoPasso = "feito" | "atual" | "futuro" | "talvez";
export interface Passo { nome: string; situacao: SituacaoDoPasso }

/** As etapas de uma jornada: feitas com ✓, a atual em destaque, as futuras apagadas, as "talvez" tracejadas. */
export function Caminho({ passos }: { passos: Passo[] }) {
  return (
    <ol className="flex flex-col gap-3 md:flex-row md:gap-0">
      {passos.map((p, i) => (
        <li key={p.nome} aria-current={p.situacao === "atual" ? "step" : undefined}
          className="relative flex items-center gap-3 md:flex-1 md:flex-col md:gap-2 md:text-center">
          {i > 0 && (
            <span aria-hidden className={cn("absolute hidden h-0.5 md:top-4 md:right-1/2 md:block md:w-full",
              p.situacao === "feito" || p.situacao === "atual" ? "bg-primary" : "bg-border")} />
          )}
          <span className={cn("relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full border-2 bg-background text-xs font-semibold",
            p.situacao === "feito" && "border-primary bg-primary text-primary-foreground",
            p.situacao === "atual" && "border-andamento text-andamento ring-4 ring-andamento/20",
            p.situacao === "talvez" && "border-dashed border-border text-muted-foreground",
            p.situacao === "futuro" && "border-border text-muted-foreground")}>
            {p.situacao === "feito" ? <Check className="size-4" aria-hidden /> : i + 1}
          </span>
          <span className={cn("text-sm md:text-xs", p.situacao === "atual" ? "font-semibold" : "text-muted-foreground")}>{p.nome}</span>
        </li>
      ))}
    </ol>
  );
}
