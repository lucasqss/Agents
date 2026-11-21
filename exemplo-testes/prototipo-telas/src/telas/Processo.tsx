import { useCallback, useEffect, useState } from "react";
import { dataHora, reais } from "../apoio/formato";
import { Aviso, BotaoLink, Botao, Caminho, Cartao, DialogoTexto, Etiqueta, Indicadores, LinhaDoTempo, Vazio, type SituacaoDoPasso, type TipoDeAviso } from "../componentes";
import { processos, type Processo as DadosDoProcesso, type Transicao } from "../jornada/api";
import { lembrar, persona, personaAtual } from "../jornada/estado";
import { ACOES, CAMINHOS, SITUACOES, TIPOS, type Acao, type Situacao } from "../jornada/vocabulario";

function EtiquetaDaSituacao({ s }: { s: Situacao }) {
  return <Etiqueta tom={SITUACOES[s]?.tom}>{SITUACOES[s]?.texto ?? s}</Etiqueta>;
}

interface Carregado { processo: DadosDoProcesso; acoes: Acao[]; historico: Transicao[] }

export function Processo({ id, recemAberto }: { id: string; recemAberto: boolean }) {
  persona.usar(); // trocar a persona recarrega a tela (as ações e o histórico mostram quem age)
  const [dados, setDados] = useState<Carregado | null>(null);
  const [falha, setFalha] = useState<string | null>(null);
  const [aviso, setAviso] = useState<{ tipo: TipoDeAviso; texto: string } | null>(recemAberto ? { tipo: "sucesso", texto: "Processo aberto." } : null);
  const [pedindoMotivo, setPedindoMotivo] = useState<Acao | null>(null);

  const carregar = useCallback(async () => {
    const [p, a, h] = await Promise.all([processos.consultar(id), processos.acoes(id), processos.historico(id)]);
    if (!p.ok) { setFalha(p.erro.mensagem); return; }
    lembrar(p.dados.id);
    setFalha(null);
    setDados({ processo: p.dados, acoes: a.ok ? a.dados : [], historico: h.ok ? h.dados : [] });
  }, [id]);

  useEffect(() => { setDados(null); carregar(); }, [carregar]);

  async function executar(acao: Acao, motivo?: string) {
    setPedindoMotivo(null);
    const r = await processos.executar(Number(id), acao, motivo);
    setAviso(r.ok
      ? { tipo: "sucesso", texto: `${ACOES[acao].texto}: feito. O processo agora está ${SITUACOES[r.dados.estado]?.texto ?? r.dados.estado}.` }
      : { tipo: "erro", texto: `${ACOES[acao].texto}: não foi possível. ${r.erro.mensagem}` });
    await carregar();
  }

  if (falha) return <><Aviso tipo="erro">{falha}</Aviso><BotaoLink href="#/">Voltar aos processos</BotaoLink></>;
  if (!dados) return <Cartao><Vazio>Carregando o processo {id}...</Vazio></Cartao>;

  const { processo: p, acoes, historico } = dados;
  const eu = personaAtual();
  const disponiveis = (Object.keys(ACOES) as Acao[]).filter((a) => acoes.includes(a));
  const percorridos = new Set(historico.flatMap((t) => [t.de, t.para]));
  const passos = (CAMINHOS[p.tipo] ?? []).map(({ situacao, talvez }) => {
    const s: SituacaoDoPasso = situacao === p.estado ? "atual" : percorridos.has(situacao) ? "feito" : talvez ? "talvez" : "futuro";
    return { nome: SITUACOES[situacao].texto + (s === "talvez" ? " (acima da alçada)" : ""), situacao: s };
  });
  const foraDoCaminho = !passos.some((x) => x.situacao === "atual");

  return (
    <>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <Indicadores itens={[
        { rotulo: "Situação", valor: <EtiquetaDaSituacao s={p.estado} /> },
        { rotulo: "Cota", valor: p.numeroCota },
        { rotulo: "Tipo", valor: TIPOS[p.tipo]?.texto ?? p.tipo },
        { rotulo: "Valor", valor: reais(p.valor) },
      ]} />

      <Cartao titulo={`Caminho do processo ${(TIPOS[p.tipo]?.texto ?? "").toLowerCase()}`}>
        <Caminho passos={passos} />
        {foraDoCaminho && <p className="mt-4 flex items-center gap-2 text-sm text-neutral-400">Situação atual fora do caminho principal: <EtiquetaDaSituacao s={p.estado} /></p>}
      </Cartao>

      <Cartao titulo="Ações disponíveis" acoes={<span className="text-xs text-neutral-500">como {eu.nome} · {eu.papel}</span>}>
        {disponiveis.length ? <>
          <div className="flex flex-wrap gap-2">
            {disponiveis.map((a) => (
              <Botao key={a} variante={ACOES[a].perigo ? "perigo" : "normal"}
                onClick={() => (ACOES[a].motivo ? setPedindoMotivo(a) : executar(a))}>{ACOES[a].texto}</Botao>
            ))}
          </div>
          <p className="mt-3 text-xs text-neutral-500">Nesta versão todos veem as ações da situação; o perfil de cada pessoa ainda vai ser definido.</p>
        </> : <p className="text-sm text-neutral-400">Nenhuma ação: o processo está numa situação final.</p>}
      </Cartao>

      <Cartao titulo="Histórico">
        {historico.length ? (
          <LinhaDoTempo itens={historico.slice().reverse().map((t, i) => ({
            chave: `${historico.length - i}`,
            titulo: <>{ACOES[t.acao]?.texto ?? t.acao} <span className="font-normal text-neutral-400">por {t.autor}</span></>,
            quando: dataHora(t.quando),
            detalhe: <><EtiquetaDaSituacao s={t.de} /> → <EtiquetaDaSituacao s={t.para} /></>,
            nota: t.observacao ?? undefined,
          }))} />
        ) : <p className="text-sm text-neutral-400">Nenhuma ação ainda.</p>}
      </Cartao>

      <DialogoTexto titulo={pedindoMotivo && ACOES[pedindoMotivo].texto}
        explicacao="Esta ação exige um motivo. Ele fica registrado no histórico do processo."
        aoCancelar={() => setPedindoMotivo(null)} aoConfirmar={(m) => pedindoMotivo && executar(pedindoMotivo, m)} />
    </>
  );
}
