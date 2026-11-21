import { Car, Home, Refrigerator, Wrench } from "lucide-react";
import { Cartao, CartaoDeEscolha } from "../../componentes";
import { rascunho } from "../../jornada/estado";
import { TIPOS, type Tipo } from "../../jornada/vocabulario";
import type { PropsDoPasso } from "./Criacao";
import { Navegacao } from "./Navegacao";

const ICONES: Record<Tipo, React.ReactNode> = {
  IMOBILIARIO: <Home className="size-5" aria-hidden />,
  AUTO: <Car className="size-5" aria-hidden />,
  ELETRO: <Refrigerator className="size-5" aria-hidden />,
  SERVICO: <Wrench className="size-5" aria-hidden />,
};

export function PassoTipo({ avancar, voltar }: PropsDoPasso) {
  const r = rascunho.usar();
  return (
    <Cartao titulo="O que vai ser comprado?">
      <div className="mb-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {(Object.keys(TIPOS) as Tipo[]).map((t) => (
          <CartaoDeEscolha key={t} selecionado={r.tipo === t} icone={ICONES[t]} titulo={TIPOS[t].texto}
            descricao={TIPOS[t].explicacao} rodape={`Bens: ${TIPOS[t].bens}${TIPOS[t].vistoria ? " · com vistoria" : ""}`}
            // Trocar o tipo descarta os bens: um processo só tem bens do próprio tipo.
            aoEscolher={() => rascunho.gravar({ ...r, tipo: t, bens: r.tipo === t ? r.bens : [] })} />
        ))}
      </div>
      <Navegacao voltar={voltar} avancar={avancar} podeAvancar={!!r.tipo} />
    </Cartao>
  );
}
