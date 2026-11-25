// TELA DE REFERÊNCIA (detalhe com ações). Tela nova de detalhe: copie esta estrutura.
//  1. Uma busca só: useRecurso + todas (recurso, ações disponíveis, histórico).
//  2. Um aviso por vez: começa com o de entrada ("Processo aberto.") e o resultado de cada ação o substitui.
//  3. Botões só das ações que a API lista, na ordem e com o rótulo da jornada; motivo num diálogo quando a jornada pede.
//  4. Depois de agir: recarregar(). A situação nova é a que a API devolve, nunca calculada aqui. Enquanto age e
//     recarrega, os botões ficam desabilitados: a lista antiga pode ter ação que já não vale.
//  5. Etapa opcional do caminho ("talvez"): feita se aparece no histórico, nunca decidida pelo valor.
import { useState } from "react";
import { processos, type Acao, type Documento } from "@/api/processos";
import { Aviso, Caminho, Carregando, Cartao, DialogoTexto, Etiqueta, Indicadores, LinhaDoTempo, SeloProposta, Tabela, Vazio, type Passo, type TipoDeAviso } from "@/componentes";
import { Button } from "@/componentes/ui/button";
import { ACOES, TEXTOS, acao, caminho, situacao, tipo, tipoDeDocumento } from "@/jornadas/processo-pagamento";
import { dataHora, reais, tamanhoDeArquivo } from "@/nucleo/formato";
import { podeTentarDeNovo, todas } from "@/nucleo/http";
import { useRecurso } from "@/nucleo/useRecurso";
import { lembrar, useEu } from "./contexto";

function EtiquetaDaSituacao({ codigo }: { codigo: string }) {
  const s = situacao(codigo);
  return <Etiqueta tom={s.tom}>{s.texto}</Etiqueta>;
}

/** `avisoDeEntrada`: o aviso de quem chegou de outra tela (ex.: "Processo aberto."), que o resultado das ações substitui. */
export function Processo({ id, avisoDeEntrada }: { id: number; avisoDeEntrada?: string }) {
  const eu = useEu();
  const recurso = useRecurso(`processo:${id}`, async () => {
    const r = await todas({ processo: processos.consultar(id), acoes: processos.acoes(id), historico: processos.historico(id) });
    if (r.ok) lembrar(id);
    return r;
  });
  const [aviso, setAviso] = useState<{ tipo: TipoDeAviso; texto: string } | null>(
    avisoDeEntrada ? { tipo: "sucesso", texto: avisoDeEntrada } : null);
  const [pedindoMotivo, setPedindoMotivo] = useState<Acao | null>(null);
  const [agindo, setAgindo] = useState(false);

  async function executar(a: Acao, motivo?: string) {
    setPedindoMotivo(null);
    setAgindo(true);
    const r = await processos.executar(id, a, eu.usuario, motivo);
    setAgindo(false);
    setAviso(r.ok
      ? { tipo: "sucesso", texto: `${ACOES[a].texto}: feito. O processo agora está ${situacao(r.dados.estado).texto}.` }
      : { tipo: "erro", texto: `${ACOES[a].texto}: não foi possível. ${r.erro.mensagem}` });
    recurso.recarregar();
  }

  if (recurso.situacao === "carregando") return <Cartao><Carregando linhas={4} texto={`Carregando o processo ${id}...`} /></Cartao>;
  if (recurso.situacao === "erro") {
    return (
      <>
        <Aviso tipo="erro" {...(podeTentarDeNovo(recurso.status) ? { acao: { texto: "Tentar de novo", aoClicar: recurso.recarregar } } : {})}>{recurso.erro.mensagem}</Aviso>
        <Button asChild variant="outline"><a href="#/">Voltar aos processos</a></Button>
      </>
    );
  }

  const { processo: p, acoes, historico } = recurso.dados;
  const disponiveis = (Object.keys(ACOES) as Acao[]).filter((a) => acoes.includes(a));
  const percorridas = new Set(historico.flatMap((t) => [t.de, t.para]));
  const passos: Passo[] = caminho(p.tipo).map((etapa) => {
    const s = etapa.situacao === p.estado ? "atual" : percorridas.has(etapa.situacao) ? "feito" : etapa.talvez ? "talvez" : "futuro";
    return { nome: situacao(etapa.situacao).texto + (s === "talvez" ? " (acima da alçada)" : ""), situacao: s };
  });

  return (
    <>
      {aviso && <Aviso tipo={aviso.tipo}>{aviso.texto}</Aviso>}
      <Indicadores itens={[
        { rotulo: "Situação", valor: <EtiquetaDaSituacao codigo={p.estado} /> },
        { rotulo: "Cota", valor: p.numeroCota },
        { rotulo: "Tipo", valor: tipo(p.tipo).texto },
        { rotulo: "Valor", valor: reais(p.valor) },
      ]} />

      <Cartao titulo={`Caminho do processo ${tipo(p.tipo).texto.toLowerCase()}`}>
        <Caminho passos={passos} />
      </Cartao>

      <Cartao titulo="Ações disponíveis" acoes={<span className="text-xs text-muted-foreground">como {eu.nome} · {eu.papel}</span>}>
        {disponiveis.length ? (
          <>
            <div className="flex flex-wrap gap-2">
              {disponiveis.map((a) => (
                <Button key={a} variant={ACOES[a].perigo ? "destructive" : "secondary"} disabled={agindo || recurso.atualizando}
                  onClick={() => (ACOES[a].motivo ? setPedindoMotivo(a) : void executar(a))}>
                  {ACOES[a].texto}
                </Button>
              ))}
            </div>
            <p className="mt-3 text-xs text-muted-foreground">{TEXTOS.perfil}</p>
          </>
        ) : <p className="text-sm text-muted-foreground">{TEXTOS.semAcoes}</p>}
      </Cartao>

      <Cartao titulo="Histórico">
        {historico.length ? (
          <LinhaDoTempo itens={historico.slice().reverse().map((t, i) => ({
            chave: String(historico.length - i),
            titulo: acao(t.acao).texto,
            complemento: `por ${t.autor}`,
            quando: dataHora(t.quando),
            detalhe: <><EtiquetaDaSituacao codigo={t.de} /> → <EtiquetaDaSituacao codigo={t.para} /></>,
            ...(t.observacao ? { nota: t.observacao } : {}),
          }))} />
        ) : <p className="text-sm text-muted-foreground">Nenhuma ação ainda.</p>}
      </Cartao>

      <DocumentosDoProcesso id={id} />

      <DialogoTexto titulo={pedindoMotivo && ACOES[pedindoMotivo].texto} explicacao={TEXTOS.motivo}
        aoCancelar={() => setPedindoMotivo(null)} aoConfirmar={(m) => pedindoMotivo && void executar(pedindoMotivo, m)} />
    </>
  );
}

/**
 * Os documentos do processo (contrato PROPOSTO), com a sua própria busca: se o serviço ainda não tiver a operação (501),
 * só este bloco mostra isso; o resto da tela segue.
 */
function DocumentosDoProcesso({ id }: { id: number }) {
  const docs = useRecurso(`documentos:${id}`, () => processos.documentos(id));
  return (
    <Cartao titulo="Documentos" acoes={<>
      <SeloProposta />
      <Button asChild variant="secondary" size="sm"><a href={`#/processo/${id}/anexar`}>Anexar documento</a></Button>
    </>}>
      {docs.situacao === "carregando" ? <Carregando linhas={2} />
        : docs.situacao === "erro" ? (docs.status === 501 ? <Vazio>{docs.erro.mensagem}</Vazio> : <Aviso tipo="erro">{docs.erro.mensagem}</Aviso>)
        : docs.dados.length === 0 ? <p className="text-sm text-muted-foreground">{TEXTOS.semDocumentos}</p>
        : <Tabela<Documento> linhas={docs.dados} chave={(d) => d.id} colunas={[
            { titulo: "Documento", valor: (d) => tipoDeDocumento(d.tipo).texto },
            { titulo: "Arquivo", valor: (d) => d.nome },
            { titulo: "Tamanho", valor: (d) => tamanhoDeArquivo(d.tamanho), numero: true },
            { titulo: "Enviado", valor: (d) => `${dataHora(d.quando)} por ${d.autor}` },
          ]} />}
    </Cartao>
  );
}
