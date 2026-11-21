import { PlayCircle } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { MODO } from "../../api/api";
import { dataHora, reais } from "../../apoio/formato";
import {
  Aviso, Botao, BotaoLink, Caminho, Cartao, DialogoTexto, Etiqueta, Indicadores, LinhaDoTempo, Tabela, Vazio,
  type SituacaoDoPasso, type TipoDeAviso,
} from "../../componentes";
import {
  desenvolvimento, documentos as apiDocumentos, processos,
  type DocumentoExigido, type Parte, type ProcessoDetalhe, type Transicao, type Bem,
} from "../../jornada/api";
import { persona, personaAtual } from "../../jornada/estado";
import { ACOES, SITUACOES, TIPOS, caminhoDoTipo, textoDaEntrada, type Acao, type Situacao } from "../../jornada/vocabulario";
import { DocumentosDoProcesso } from "./DocumentosDoProcesso";

const Situ = ({ s }: { s: Situacao }) => <Etiqueta tom={SITUACOES[s]?.tom}>{SITUACOES[s]?.texto ?? s}</Etiqueta>;

interface Carregado { processo: ProcessoDetalhe; acoes: Acao[]; historico: Transicao[]; documentos: DocumentoExigido[] }
type PedidoDeMotivo = { titulo: string; executar: (motivo: string) => Promise<void> } | null;

export function ProcessoEmAnalise({ id }: { id: string }) {
  persona.usar(); // trocar a persona recarrega: as recusas de papel e o R17 dependem de quem age
  const [dados, setDados] = useState<Carregado | null>(null);
  const [falha, setFalha] = useState<string | null>(null);
  const [aviso, setAviso] = useState<{ tipo: TipoDeAviso; texto: string } | null>(null);
  const [pedindo, setPedindo] = useState<PedidoDeMotivo>(null);

  const carregar = useCallback(async () => {
    const [p, a, h, d] = await Promise.all([processos.detalhar(id), processos.acoes(id), processos.historico(id), apiDocumentos.doProcesso(id)]);
    if (!p.ok) return setFalha(p.erro.mensagem);
    setFalha(null);
    setDados({ processo: p.dados, acoes: a.ok ? a.dados.acoes : [], historico: h.ok ? h.dados : [], documentos: d.ok ? d.dados : [] });
  }, [id]);
  useEffect(() => { carregar(); }, [carregar]);

  async function depois(ok: boolean, sucesso: string, erro: string) {
    setAviso({ tipo: ok ? "sucesso" : "erro", texto: ok ? sucesso : erro });
    await carregar();
  }

  async function executar(acao: Acao, motivo?: string) {
    setPedindo(null);
    const r = await processos.executar(Number(id), acao, motivo);
    await depois(r.ok, r.ok ? `${ACOES[acao].texto}: feito. O processo agora está ${SITUACOES[r.dados.estado]?.texto ?? r.dados.estado}.` : "",
      r.ok ? "" : `${ACOES[acao].texto}: não foi possível. ${r.erro.mensagem}`);
  }

  async function ver(d: DocumentoExigido) {
    const r = await apiDocumentos.arquivo(Number(id), d.id);
    if (!r.ok) return setAviso({ tipo: "erro", texto: `${d.texto}: ${r.erro.mensagem}` });
    window.open(URL.createObjectURL(r.dados), "_blank", "noopener");
  }

  async function aceitar(d: DocumentoExigido) {
    const r = await apiDocumentos.aceitar(Number(id), d.id);
    await depois(r.ok, `${d.texto}: aceito.`, r.ok ? "" : `${d.texto}: não foi possível aceitar. ${r.erro.mensagem}`);
  }

  function recusar(d: DocumentoExigido) {
    setPedindo({
      titulo: `Recusar: ${d.texto}`,
      executar: async (motivo) => {
        setPedindo(null);
        const r = await apiDocumentos.recusar(Number(id), d.id, motivo);
        await depois(r.ok, `${d.texto}: recusado. A parte recebeu um e-mail para reenviar.`, r.ok ? "" : `${d.texto}: não foi possível recusar. ${r.erro.mensagem}`);
      },
    });
  }

  if (falha) return <><Aviso tipo="erro">{falha}</Aviso><BotaoLink href="#/analise">Voltar à fila</BotaoLink></>;
  if (!dados) return <Cartao><Vazio>Carregando o processo {id}...</Vazio></Cartao>;

  const { processo: p, acoes, historico, documentos } = dados;
  const tipo = TIPOS[p.tipo];
  const eu = personaAtual();
  const botoes = (Object.keys(ACOES) as Acao[]).filter((a) => ACOES[a].naAnalise && acoes.includes(a));
  const percorridos = new Set(historico.flatMap((t) => [t.de, t.para]));
  const passos = caminhoDoTipo(p.tipo).map(({ situacao, talvez }) => {
    const s: SituacaoDoPasso = situacao === p.estado ? "atual" : percorridos.has(situacao) ? "feito" : talvez ? "talvez" : "futuro";
    return { nome: SITUACOES[situacao].texto + (s === "talvez" ? " (acima da alçada)" : ""), situacao: s };
  });
  const conferindo = p.estado === "EM_ANALISE" || p.estado === "PENDENTE_DOCUMENTO";

  return (
    <>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <Indicadores itens={[
        { rotulo: "Situação", valor: <Situ s={p.estado} /> },
        { rotulo: "Cota", valor: p.cotaNumero },
        { rotulo: "Tipo", valor: tipo.texto },
        { rotulo: "Valor", valor: reais(p.valor) },
      ]} />

      <Cartao titulo={`Caminho do processo ${tipo.texto.toLowerCase()}`}>
        <Caminho passos={passos} />
        {!passos.some((x) => x.situacao === "atual") && (
          <p className="mt-4 flex items-center gap-2 text-sm text-neutral-400">Situação atual fora do caminho principal: <Situ s={p.estado} /></p>
        )}
      </Cartao>

      <Cartao titulo="Ações disponíveis" acoes={<span className="text-xs text-neutral-500">como {eu.nome} · {eu.descricao}</span>}>
        {botoes.length ? (
          <div className="flex flex-wrap gap-2">
            {botoes.map((a) => (
              <Botao key={a} variante={ACOES[a].perigo ? "perigo" : "normal"}
                onClick={() => (ACOES[a].motivo ? setPedindo({ titulo: ACOES[a].texto, executar: (m) => executar(a, m) }) : executar(a))}>
                {ACOES[a].texto}
              </Botao>
            ))}
          </div>
        ) : (
          <p className="text-sm text-neutral-400">
            {SITUACOES[p.estado].final ? "Nenhuma ação: o processo está numa situação final."
              : p.estado === "AGUARDANDO_DOCUMENTOS" ? "Aguardando as partes enviarem os documentos."
              : p.estado === "PAGAMENTO_SOLICITADO" ? "Aguardando a rotina de pagamento, que pede o pagamento à tesouraria a cada 5 minutos."
              : "Nenhuma ação nesta situação."}
          </p>
        )}
        <p className="mt-3 text-xs text-neutral-500">
          Os botões são as ações que a situação aceita. Quem pode cada uma é conferido pelo serviço: com a persona errada, aparece a recusa.
        </p>
        {MODO === "falso" && p.estado === "PAGAMENTO_SOLICITADO" && (
          <div className="mt-3">
            <Botao onClick={async () => { const r = await desenvolvimento.rodarRotina(); await depois(r.ok, "Rotina de pagamento rodou: a tesouraria simulada confirmou.", "A rotina falhou."); }}
              icone={<PlayCircle className="size-4" aria-hidden />}>Rodar a rotina de pagamento agora (simulação)</Botao>
          </div>
        )}
      </Cartao>

      <DocumentosDoProcesso documentos={documentos} partes={p.partes} conferindo={conferindo} aoVer={ver} aoAceitar={aceitar} aoRecusar={recusar} />

      <Cartao titulo={`${tipo.bens[0].toUpperCase()}${tipo.bens.slice(1)}`}>
        <Tabela<Bem> linhas={p.bens} chave={(b) => JSON.stringify(b)} colunas={[
          ...tipo.campos.map((c) => ({ titulo: c.rotulo, valor: (b: Bem) => b.dados[c.chave] })),
          { titulo: "Valor", valor: (b) => reais(b.valor), numero: true },
        ]} />
      </Cartao>

      <Cartao titulo="Partes">
        <Tabela<Parte> linhas={p.partes} chave={(x) => x.id} colunas={[
          { titulo: "Nome", valor: (x) => x.nome },
          { titulo: "Papel", valor: (x) => x.papel },
          { titulo: "CPF/CNPJ", valor: (x) => x.documento },
          { titulo: "E-mail", valor: (x) => x.email },
          { titulo: "Recebe", valor: (x) => (x.valor ? reais(x.valor) : "—"), numero: true },
        ]} />
      </Cartao>

      <Cartao titulo="Histórico">
        {historico.length ? (
          <LinhaDoTempo itens={historico.slice().reverse().map((t, i) => ({
            chave: String(historico.length - i),
            titulo: <>{textoDaEntrada(t.acao)} <span className="font-normal text-neutral-400">por {t.autor}</span></>,
            quando: dataHora(t.quando),
            detalhe: t.de !== t.para ? <><Situ s={t.de} /> → <Situ s={t.para} /></> : undefined,
            nota: t.observacao ?? undefined,
          }))} />
        ) : <p className="text-sm text-neutral-400">Nenhuma ação ainda.</p>}
      </Cartao>

      <DialogoTexto titulo={pedindo?.titulo ?? null}
        explicacao="Informe o motivo. Ele fica registrado no histórico do processo."
        aoCancelar={() => setPedindo(null)} aoConfirmar={(m) => pedindo?.executar(m)} />
    </>
  );
}
