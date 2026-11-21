import { AlertTriangle, CheckCircle2, Info, XCircle } from "lucide-react";
import type { ReactNode } from "react";
import { CORES_DO_TOM } from "./tons";

const ICONE = { sucesso: CheckCircle2, erro: XCircle, atencao: AlertTriangle, info: Info };
const TOM = { sucesso: "sucesso", erro: "erro", atencao: "espera", info: "andamento" } as const;

export type TipoDeAviso = keyof typeof ICONE;

export function Aviso({ tipo = "info", children }: { tipo?: TipoDeAviso; children: ReactNode }) {
  const Icone = ICONE[tipo];
  return (
    <div role={tipo === "erro" ? "alert" : "status"} className={`flex items-start gap-2.5 rounded-lg px-4 py-3 text-sm ring-1 ring-inset ${CORES_DO_TOM[TOM[tipo]]}`}>
      <Icone className="mt-0.5 size-4 shrink-0" aria-hidden />
      <div>{children}</div>
    </div>
  );
}
