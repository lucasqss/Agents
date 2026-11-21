import { ClipboardCheck, FilePlus2, FolderUp, Sparkles } from "lucide-react";
import { useState } from "react";
import { MODO } from "../api/api";
import { Aviso, Botao, Cartao, CartaoDeEscolha } from "../componentes";
import { prepararDemonstracao } from "../jornada/demonstracao";
import { demonstracao } from "../jornada/estado";

export function EscolherJornada() {
  const demo = demonstracao.usar();
  const [preparando, setPreparando] = useState(false);
  const [falha, setFalha] = useState<string | null>(null);

  async function preparar() {
    setPreparando(true);
    setFalha(await prepararDemonstracao());
    setPreparando(false);
  }

  return (
    <>
      {!demo ? (
        <Cartao titulo="Antes de começar">
          <p className="mb-4 text-sm text-neutral-400">
            A demonstração precisa de uma cliente com cotas. O botão abaixo cadastra a cliente Maria Souza e duas cotas dela
            (crédito de R$ 150.000,00 e de R$ 40.000,00){MODO === "falso" ? " no serviço simulado" : ", como Ana (backoffice), no serviço"}.
          </p>
          {falha && <div className="mb-4"><Aviso tipo="erro">{falha}</Aviso></div>}
          <Botao variante="principal" onClick={preparar} disabled={preparando} icone={<Sparkles className="size-4" aria-hidden />}>
            Preparar demonstração
          </Botao>
        </Cartao>
      ) : (
        <Aviso tipo="sucesso">Demonstração pronta: cliente Maria Souza, com {demo.cotas.length} cotas.</Aviso>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        <CartaoDeEscolha href="#/criacao" desabilitado={!demo} icone={<FilePlus2 className="size-5" aria-hidden />}
          titulo="1. Criar o processo"
          descricao="O comprador escolhe a cota e o tipo, informa os bens e as partes. Cada parte recebe um e-mail para enviar os documentos."
          rodape="Quem atua: Maria (cliente da cota)" />
        <CartaoDeEscolha href="#/documentos" desabilitado={!demo} icone={<FolderUp className="size-5" aria-hidden />}
          titulo="2. Enviar os documentos"
          descricao="Cada parte abre o link do e-mail e envia os próprios documentos. Com tudo enviado, o processo fica pronto para análise."
          rodape="Quem atua: cada parte, pelo link (vê só o que é dela)" />
        <CartaoDeEscolha href="#/analise" desabilitado={!demo} icone={<ClipboardCheck className="size-5" aria-hidden />}
          titulo="3. Analisar"
          descricao="O backoffice confere documento a documento, conclui a análise e a vistoria; o aprovador decide acima da alçada; a tesouraria solicita o pagamento."
          rodape="Quem atua: Ana e Carlos (backoffice), Bruno (aprovador), Tesouraria" />
      </div>
    </>
  );
}
