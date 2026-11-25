import { CircleAlert, CircleCheck, Info } from "lucide-react";
import type { ReactNode } from "react";
import { Alert, AlertDescription } from "@/componentes/ui/alert";
import { Button } from "@/componentes/ui/button";

export type TipoDeAviso = "sucesso" | "erro" | "info";

/**
 * O resultado do que a pessoa fez. "erro" é anunciado como alerta; os outros, como status.
 * Na tela, um aviso por vez: o resultado novo substitui o anterior.
 * `acao`: um botão no aviso (ex.: "Tentar de novo" numa falha de rede, chamando o recarregar do recurso).
 */
export function Aviso({ tipo = "info", acao, children }: { tipo?: TipoDeAviso; acao?: { texto: string; aoClicar: () => void }; children: ReactNode }) {
  const Icone = tipo === "sucesso" ? CircleCheck : tipo === "erro" ? CircleAlert : Info;
  return (
    <Alert role={tipo === "erro" ? "alert" : "status"} variant={tipo === "erro" ? "destructive" : "default"}
      className={tipo === "sucesso" ? "border-sucesso/40 text-sucesso" : undefined}>
      <Icone aria-hidden />
      <AlertDescription className={tipo === "sucesso" ? "text-sucesso" : undefined}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span>{children}</span>
          {acao && <Button size="sm" variant="outline" onClick={acao.aoClicar}>{acao.texto}</Button>}
        </div>
      </AlertDescription>
    </Alert>
  );
}
