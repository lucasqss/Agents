import { useState } from "react";
import { Button } from "@/componentes/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/componentes/ui/dialog";
import { Textarea } from "@/componentes/ui/textarea";

/**
 * Pede um texto (ex.: o motivo de uma ação) num diálogo. Aberto quando `titulo` não é nulo.
 *   <DialogoTexto titulo={pedindo && ACOES[pedindo].texto} explicacao="..." aoConfirmar={(t) => ...} aoCancelar={() => ...} />
 */
export function DialogoTexto({ titulo, explicacao, obrigatorio = true, aoConfirmar, aoCancelar }: {
  titulo: string | null; explicacao?: string; obrigatorio?: boolean; aoConfirmar: (texto: string) => void; aoCancelar: () => void;
}) {
  return (
    <Dialog open={titulo !== null} onOpenChange={(aberto) => !aberto && aoCancelar()}>
      <DialogContent>
        {titulo !== null && <Formulario key={titulo} titulo={titulo} explicacao={explicacao} obrigatorio={obrigatorio} aoConfirmar={aoConfirmar} aoCancelar={aoCancelar} />}
      </DialogContent>
    </Dialog>
  );
}

function Formulario({ titulo, explicacao, obrigatorio, aoConfirmar, aoCancelar }: {
  titulo: string; explicacao: string | undefined; obrigatorio: boolean; aoConfirmar: (texto: string) => void; aoCancelar: () => void;
}) {
  const [texto, setTexto] = useState("");
  return (
    <form className="grid gap-4" onSubmit={(e) => { e.preventDefault(); aoConfirmar(texto.trim()); }}>
      <DialogHeader>
        <DialogTitle>{titulo}</DialogTitle>
        {explicacao && <DialogDescription>{explicacao}</DialogDescription>}
      </DialogHeader>
      <Textarea aria-label={titulo} rows={4} maxLength={500} placeholder="Escreva aqui" value={texto} onChange={(e) => setTexto(e.target.value)} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={aoCancelar}>Voltar</Button>
        <Button type="submit" disabled={obrigatorio && !texto.trim()}>Confirmar</Button>
      </DialogFooter>
    </form>
  );
}
