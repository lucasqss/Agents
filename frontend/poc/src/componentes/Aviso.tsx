import { CircleAlert, CircleCheck, Info } from "lucide-react";
import type { ReactNode } from "react";
import { Alert, AlertDescription } from "@/componentes/ui/alert";

export type TipoDeAviso = "sucesso" | "erro" | "info";

/**
 * O resultado do que a pessoa fez. "erro" é anunciado como alerta; os outros, como status.
 * Na tela, um aviso por vez: o resultado novo substitui o anterior.
 */
export function Aviso({ tipo = "info", children }: { tipo?: TipoDeAviso; children: ReactNode }) {
  const Icone = tipo === "sucesso" ? CircleCheck : tipo === "erro" ? CircleAlert : Info;
  return (
    <Alert role={tipo === "erro" ? "alert" : "status"} variant={tipo === "erro" ? "destructive" : "default"}
      className={tipo === "sucesso" ? "border-sucesso/40 text-sucesso" : undefined}>
      <Icone aria-hidden />
      <AlertDescription className={tipo === "sucesso" ? "text-sucesso" : undefined}>{children}</AlertDescription>
    </Alert>
  );
}
