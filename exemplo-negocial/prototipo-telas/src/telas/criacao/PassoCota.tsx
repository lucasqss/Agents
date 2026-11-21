import { CreditCard } from "lucide-react";
import { useEffect, useState } from "react";
import { reais } from "../../apoio/formato";
import { Aviso, Cartao, CartaoDeEscolha, Vazio } from "../../componentes";
import { cotas, type Cota } from "../../jornada/api";
import { demonstracao, rascunho } from "../../jornada/estado";
import { SITUACOES } from "../../jornada/vocabulario";
import type { PropsDoPasso } from "./Criacao";
import { Navegacao } from "./Navegacao";

export function PassoCota({ avancar }: PropsDoPasso) {
  const r = rascunho.usar();
  const demo = demonstracao.usar();
  const [lista, setLista] = useState<Cota[] | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  useEffect(() => {
    if (!demo) return;
    cotas.doCliente(demo.clienteId).then((x) => (x.ok ? setLista(x.dados) : setFalha(x.erro.mensagem)));
  }, [demo]);

  return (
    <Cartao titulo="Para qual cota é o pagamento?">
      {falha && <Aviso tipo="erro">{falha}</Aviso>}
      {!falha && lista === null && <Vazio>Carregando as suas cotas...</Vazio>}
      {lista && (
        <div className="mb-5 grid gap-3 sm:grid-cols-2">
          {lista.map((c) => {
            const emUso = c.processoAtualId !== null;
            return (
              <CartaoDeEscolha key={c.id} selecionado={r.cotaId === c.id} desabilitado={emUso}
                aoEscolher={() => rascunho.gravar({ ...r, cotaId: c.id })}
                icone={<CreditCard className="size-5" aria-hidden />}
                titulo={`Cota ${c.numero}`}
                descricao={<>Crédito disponível: <strong className="text-neutral-200">{reais(c.creditoDisponivel)}</strong></>}
                rodape={emUso
                  ? `Indisponível: o processo ${c.processoAtualId} está ${SITUACOES[c.processoAtualEstado!]?.texto.toLowerCase() ?? "em andamento"} (uma cota só tem um processo em andamento)`
                  : `Crédito da cota: ${reais(c.valorCredito)}`} />
            );
          })}
        </div>
      )}
      <Navegacao avancar={avancar} podeAvancar={!!r.cotaId} />
    </Cartao>
  );
}
