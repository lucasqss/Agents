import { Check } from "lucide-react";

export type SituacaoDoPasso = "feito" | "atual" | "futuro" | "talvez";

/** Os passos de uma jornada: feitos com ✓, o atual em destaque, os futuros apagados. No celular, vira lista. */
export function Caminho({ passos }: { passos: { nome: string; situacao: SituacaoDoPasso }[] }) {
  return (
    <ol className="flex flex-col gap-3 md:flex-row md:gap-0">
      {passos.map((p, i) => (
        <li key={p.nome} aria-current={p.situacao === "atual" ? "step" : undefined}
          className="relative flex items-center gap-3 md:flex-1 md:flex-col md:gap-2 md:text-center">
          {i > 0 && (
            <span aria-hidden className={`absolute hidden h-0.5 md:block md:top-4 md:right-1/2 md:w-full ${p.situacao === "feito" || p.situacao === "atual" ? "bg-blue-500" : "bg-neutral-800"}`} />
          )}
          <span className={`relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full border-2 text-xs font-semibold ${
            p.situacao === "feito" ? "border-blue-500 bg-blue-500 text-white"
            : p.situacao === "atual" ? "border-blue-500 bg-neutral-950 text-blue-300 ring-4 ring-blue-500/20"
            : p.situacao === "talvez" ? "border-dashed border-neutral-700 bg-neutral-950 text-neutral-500"
            : "border-neutral-700 bg-neutral-950 text-neutral-500"}`}>
            {p.situacao === "feito" ? <Check className="size-4" aria-hidden /> : i + 1}
          </span>
          <span className={`text-sm md:text-xs ${p.situacao === "atual" ? "font-semibold text-neutral-100" : p.situacao === "feito" ? "text-neutral-300" : "text-neutral-500"}`}>
            {p.nome}
          </span>
        </li>
      ))}
    </ol>
  );
}
