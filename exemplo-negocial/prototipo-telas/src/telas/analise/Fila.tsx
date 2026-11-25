import { useEffect, useState } from "react";
import { reais } from "../../apoio/formato";
import { ir } from "../../apoio/rotas";
import { Abas, Aviso, Cartao, Etiqueta, Tabela, Vazio } from "../../componentes";
import { processos, type ProcessoResumo } from "../../jornada/api";
import { persona } from "../../jornada/estado";
import {
  FILTRO_INICIAL_DA_PERSONA, FILTRO_PADRAO, FILTROS_DA_FILA, SITUACOES, TIPOS, type FiltroDaFila,
} from "../../jornada/vocabulario";

const filtroInicial = (usuario: string) => FILTRO_INICIAL_DA_PERSONA[usuario] ?? FILTRO_PADRAO;

/** Os processos que a aba mostra: os das situações dela, ou todos se ela não lista situações. */
function naAba(valor: FiltroDaFila, lista: ProcessoResumo[]) {
  const situacoes = FILTROS_DA_FILA.find((f) => f.valor === valor)?.situacoes;
  return situacoes ? lista.filter((p) => situacoes.includes(p.estado)) : lista;
}

export function Fila() {
  const usuario = persona.usar();
  const [aba, setAba] = useState<FiltroDaFila>(filtroInicial(usuario));
  const [lista, setLista] = useState<ProcessoResumo[] | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  useEffect(() => { setAba(filtroInicial(usuario)); }, [usuario]);
  useEffect(() => {
    processos.listar().then((r) => (r.ok ? setLista(r.dados.itens) : setFalha(r.erro.mensagem)));
  }, [usuario]);

  if (falha) return <Aviso tipo="erro">{falha}</Aviso>;
  const visiveis = naAba(aba, lista ?? []);

  return (
    <Cartao>
      <Abas abas={FILTROS_DA_FILA.map((f) => ({ valor: f.valor, texto: f.texto, contador: lista ? naAba(f.valor, lista).length : undefined }))}
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
