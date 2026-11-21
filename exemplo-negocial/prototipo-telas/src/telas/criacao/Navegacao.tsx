import { ArrowLeft, ArrowRight } from "lucide-react";
import { Botao } from "../../componentes";

/** Voltar e continuar, no pé de cada passo. */
export function Navegacao({ voltar, avancar, podeAvancar = true, textoAvancar = "Continuar" }:
  { voltar?: () => void; avancar: () => void; podeAvancar?: boolean; textoAvancar?: string }) {
  return (
    <div className="flex justify-between gap-2">
      {voltar ? <Botao onClick={voltar} icone={<ArrowLeft className="size-4" aria-hidden />}>Voltar</Botao> : <span />}
      <Botao variante="principal" onClick={avancar} disabled={!podeAvancar}>
        {textoAvancar}<ArrowRight className="size-4" aria-hidden />
      </Botao>
    </div>
  );
}
