import type { ReactNode } from "react";
import { Badge } from "@/componentes/ui/badge";
import { CORES_DO_TOM, type Tom } from "./tons";

/** A situação de algo, colorida pelo tom: <Etiqueta tom="espera">Aguardando vistoria</Etiqueta>. */
export function Etiqueta({ tom = "neutro", children }: { tom?: Tom; children: ReactNode }) {
  return <Badge variant="outline" className={CORES_DO_TOM[tom]}>{children}</Badge>;
}
