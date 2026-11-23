import { useId, type ReactElement } from "react";
import { cloneElement } from "react";
import { Label } from "@/componentes/ui/label";

/**
 * Rótulo + controle + ajuda + erro, ligados por id e aria (o leitor de tela anuncia o erro do campo).
 *   <Campo rotulo="Número da cota" erro={erros.numeroCota}><Input name="numeroCota" ... /></Campo>
 */
export function Campo({ rotulo, ajuda, erro, children }: {
  rotulo: string; ajuda?: string; erro?: string | undefined; children: ReactElement<Record<string, unknown>>;
}) {
  const id = useId();
  const idDaMensagem = `${id}-mensagem`;
  return (
    <div className="space-y-1.5">
      <Label htmlFor={id}>{rotulo}</Label>
      {cloneElement(children, { id, "aria-invalid": erro ? true : undefined, "aria-describedby": erro || ajuda ? idDaMensagem : undefined })}
      {erro ? <p id={idDaMensagem} className="text-xs text-destructive">{erro}</p>
        : ajuda ? <p id={idDaMensagem} className="text-xs text-muted-foreground">{ajuda}</p> : null}
    </div>
  );
}
