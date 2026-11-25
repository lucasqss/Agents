// TELA DE REFERÊNCIA (lista vinda da API, com filtro e páginas). Endpoint do contrato PROPOSTO: no modo real, enquanto
// o serviço não o tiver, a tela mostra que é proposta (a api devolve 501 sem chamar).
import { useState } from "react";
import { processos, type Processo, type Situacao } from "@/api/processos";
import { Aviso, Carregando, Cartao, Etiqueta, Paginacao, SeloProposta, SeletorNativo, Tabela, Vazio } from "@/componentes";
import { Label } from "@/componentes/ui/label";
import { SITUACOES, situacao, tipo } from "@/jornadas/processo-pagamento";
import { reais } from "@/nucleo/formato";
import { podeTentarDeNovo } from "@/nucleo/http";
import { ir } from "@/nucleo/rotas";
import { useRecurso } from "@/nucleo/useRecurso";

const TAMANHO_DA_PAGINA = 5;

export function ListaProcessos() {
  const [filtro, setFiltro] = useState<Situacao | "">("");
  const [pagina, setPagina] = useState(0);
  const lista = useRecurso(`lista:${filtro}:${pagina}`, () =>
    processos.listar({ ...(filtro ? { situacao: filtro } : {}), pagina, tamanho: TAMANHO_DA_PAGINA }));

  function filtrar(valor: string) {
    setFiltro(valor as Situacao | "");
    setPagina(0);
  }

  return (
    <Cartao titulo="Todos os processos" acoes={<SeloProposta />}>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <Label htmlFor="filtro-situacao">Situação</Label>
        <SeletorNativo id="filtro-situacao" className="w-56" value={filtro} onChange={(e) => filtrar(e.target.value)}>
          <option value="" className="bg-popover">Todas</option>
          {(Object.keys(SITUACOES) as Situacao[]).map((s) => <option key={s} value={s} className="bg-popover">{SITUACOES[s].texto}</option>)}
        </SeletorNativo>
      </div>
      {lista.situacao === "carregando" ? <Carregando linhas={5} />
        : lista.situacao === "erro" ? (lista.status === 501 ? <Vazio>{lista.erro.mensagem}</Vazio>
          : <Aviso tipo="erro" {...(podeTentarDeNovo(lista.status) ? { acao: { texto: "Tentar de novo", aoClicar: lista.recarregar } } : {})}>{lista.erro.mensagem}</Aviso>)
        : lista.dados.total === 0 ? <Vazio>{filtro ? "Nenhum processo nesta situação." : "Nenhum processo ainda."}</Vazio>
        : <>
            <Tabela<Processo> linhas={lista.dados.itens} chave={(p) => p.id} aoEscolher={(p) => ir(`/processo/${p.id}`)} colunas={[
              { titulo: "Número", valor: (p) => p.id, numero: true },
              { titulo: "Cota", valor: (p) => p.numeroCota },
              { titulo: "Tipo", valor: (p) => tipo(p.tipo).texto },
              { titulo: "Valor", valor: (p) => reais(p.valor), numero: true },
              { titulo: "Situação", valor: (p) => <Etiqueta tom={situacao(p.estado).tom}>{situacao(p.estado).texto}</Etiqueta> },
            ]} />
            <Paginacao pagina={pagina} total={lista.dados.total} tamanho={TAMANHO_DA_PAGINA} aoMudar={setPagina} />
          </>}
    </Cartao>
  );
}
