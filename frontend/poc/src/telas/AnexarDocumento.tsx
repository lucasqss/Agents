// TELA DE REFERÊNCIA (assistente em passos, com envio de arquivo). Endpoint do contrato PROPOSTO.
//  - Cada passo confere só o que a pessoa precisa para seguir (escolheu o tipo? escolheu o arquivo?).
//  - O envio é no último passo, depois de conferir. Erro da API: o aviso com a mensagem e os campos, no próprio passo.
//  - Deu certo: volta para o processo com o aviso de entrada "Documento anexado.".
import { useState } from "react";
import { processos, type TipoDocumento } from "@/api/processos";
import { Aviso, Cartao, EnvioDeArquivo, Indicadores, Opcoes, Passos, SeloProposta } from "@/componentes";
import { Button } from "@/componentes/ui/button";
import { TEXTOS, TIPOS_DE_DOCUMENTO } from "@/jornadas/processo-pagamento";
import { tamanhoDeArquivo } from "@/nucleo/formato";
import { ir } from "@/nucleo/rotas";
import { useEu } from "./contexto";

const PASSOS = ["Tipo do documento", "Arquivo", "Conferir e enviar"];

export function AnexarDocumento({ id }: { id: number }) {
  const eu = useEu();
  const [passo, setPasso] = useState(0);
  const [tipoDoDocumento, setTipo] = useState<TipoDocumento | null>(null);
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar() {
    if (!tipoDoDocumento || !arquivo) return;
    setErro(null);
    setEnviando(true);
    const r = await processos.anexarDocumento(id, tipoDoDocumento, arquivo, eu.usuario);
    setEnviando(false);
    if (r.ok) {
      ir(`/processo/${id}?anexado=1`);
      return;
    }
    setErro([r.erro.mensagem, ...r.erro.campos.map((c) => `${c.campo ?? ""}: ${c.erro}`)].join(" · "));
  }

  return (
    <Cartao titulo={`Anexar documento ao processo ${id}`} acoes={<SeloProposta />}>
      <div className="space-y-5">
        <Passos passos={PASSOS} atual={passo} aoVoltar={setPasso} />

        {passo === 0 && (
          <Opcoes rotulo="Tipo do documento" valor={tipoDoDocumento ?? ("" as TipoDocumento)} aoMudar={setTipo}
            opcoes={(Object.keys(TIPOS_DE_DOCUMENTO) as TipoDocumento[]).map((t) => ({ valor: t, titulo: TIPOS_DE_DOCUMENTO[t].texto, explicacao: TIPOS_DE_DOCUMENTO[t].explicacao }))} />
        )}
        {passo === 1 && (
          <EnvioDeArquivo rotulo="Arquivo do documento" ajuda={TEXTOS.arquivosAceitos} aceitos={[".pdf", ".jpg", ".jpeg", ".png"]} tamanhoMaximoMb={10}
            arquivo={arquivo} aoEscolher={setArquivo} />
        )}
        {passo === 2 && tipoDoDocumento && arquivo && (
          <Indicadores itens={[
            { rotulo: "Processo", valor: id },
            { rotulo: "Tipo", valor: TIPOS_DE_DOCUMENTO[tipoDoDocumento].texto },
            { rotulo: "Arquivo", valor: arquivo.name },
            { rotulo: "Tamanho", valor: tamanhoDeArquivo(arquivo.size) },
          ]} />
        )}

        {erro && <Aviso tipo="erro">{erro}</Aviso>}

        <div className="flex flex-wrap gap-2">
          {passo > 0 && <Button variant="outline" onClick={() => setPasso(passo - 1)}>Voltar</Button>}
          {passo < 2 && (
            <Button disabled={(passo === 0 && !tipoDoDocumento) || (passo === 1 && !arquivo)} onClick={() => setPasso(passo + 1)}>Continuar</Button>
          )}
          {passo === 2 && <Button disabled={enviando} onClick={() => void enviar()}>Enviar documento</Button>}
          <Button asChild variant="ghost"><a href={`#/processo/${id}`}>Cancelar</a></Button>
        </div>
      </div>
    </Cartao>
  );
}
