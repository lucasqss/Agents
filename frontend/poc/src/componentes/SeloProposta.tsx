import { Lightbulb } from "lucide-react";

/** Marca o que as telas propõem e o serviço ainda não tem (contrato proposto): o negócio vê o que é rascunho. */
export function SeloProposta() {
  return (
    <span title="O serviço ainda não tem esta parte: é uma proposta das telas (contrato/proposto.yaml)."
      className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border border-espera/30 bg-espera/10 px-2.5 py-0.5 text-xs font-medium text-espera">
      <Lightbulb className="size-3.5" aria-hidden />
      Proposta
    </span>
  );
}
