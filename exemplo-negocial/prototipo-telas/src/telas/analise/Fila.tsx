import { useEffect, useState } from "react";
import { reais } from "../../apoio/formato";
import { ir } from "../../apoio/rotas";
import { Abas, Aviso, Cartao, Etiqueta, Tabela, Vazio } from "../../componentes";
import { processos, type ProcessoResumo } from "../../jornada/api";
import { persona } from "../../jornada/estado";
import { SITUACOES, TIPOS, type Situacao } from "../../jornada/vocabulario";

type Aba = Situacao | "TODOS";
const ABAS: { valor: Aba; texto: string }[] = [
  { valor: "ABERTO", texto: "Prontos para análise" },
  { valor: "EM_ANALISE", texto: "Em análise" },
  { valor: "PENDENTE_DOCUMENTO", texto: "Pendente de documento" },
  { valor: "AGUARDANDO_VISTORIA", texto: "Aguardando vistoria" },
  { valor: "AGUARDANDO_ALCADA", texto: "Aguardando alçada" },
  { valor: "APROVADO", texto: "Aprovados" },
  { valor: "AGUARDANDO_DOCUMENTOS", texto: "Aguardando documentos" },
  { valor: "TODOS", texto: "Todos" },
];

/** A aba com que cada papel começa: o que é dele fazer. */
const ABA_DO_PAPEL: Record<string, Aba> = { ana: "ABERTO", carlos: "ABERTO", bruno: "AGUARDANDO_ALCADA", tesouraria: "APROVADO" };

export function Fila() {
  const usuario = persona.usar();
  const [aba, setAba] = useState<Aba>(ABA_DO_PAPEL[usuario] ?? "ABERTO");
  const [lista, setLista] = useState<ProcessoResumo[] | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  useEffect(() => { setAba(ABA_DO_PAPEL[usuario] ?? "ABERTO"); }, [usuario]);
  useEffect(() => {
    processos.listar().then((r) => (r.ok ? setLista(r.dados.itens) : setFalha(r.erro.mensagem)));
  }, [usuario]);

  if (falha) return <Aviso tipo="erro">{falha}</Aviso>;
  const visiveis = (lista ?? []).filter((p) => aba === "TODOS" || p.estado === aba);

  return (
    <Cartao>
      <Abas abas={ABAS.map((a) => ({ ...a, contador: a.valor === "TODOS" ? lista?.length : lista?.filter((p) => p.estado === a.valor).length }))}
        atual={aba} aoMudar={setAba} />
      <div className="mt-4">
        {lista === null ? <Vazio>Carregando...</Vazio>
          : !visiveis.length ? <Vazio>Nenhum processo nesta situação.</Vazio>
          : <Tabela<ProcessoResumo> linhas={visiveis} chave={(p) => p.id} aoClicar={(p) => ir(`/analise/${p.id}`)} colunas={[
              { titulo: "Nº", valor: (p) => p.id, numero: true },
              { titulo: "Cota", valor: (p) => p.cotaNumero },
              { titulo: "Tipo", valor: (p) => TIPOS[p.tipo]?.texto ?? p.tipo },
              { titulo: "Valor", valor: (p) => reais(p.valor), numero: true },
              { titulo: "Situação", valor: (p) => <Etiqueta tom={SITUACOES[p.estado]?.tom}>{SITUACOES[p.estado]?.texto ?? p.estado}</Etiqueta> },
            ]} />}
      </div>
    </Cartao>
  );
}
