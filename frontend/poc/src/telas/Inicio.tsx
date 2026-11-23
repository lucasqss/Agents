import { Plus, Search } from "lucide-react";
import { useState } from "react";
import { processos, type Processo } from "@/api/processos";
import { Cartao, Etiqueta, Tabela, Vazio } from "@/componentes";
import { Button } from "@/componentes/ui/button";
import { Input } from "@/componentes/ui/input";
import { situacao, tipo } from "@/jornadas/processo-pagamento";
import { reais } from "@/nucleo/formato";
import { useRecurso } from "@/nucleo/useRecurso";
import { ir } from "@/nucleo/rotas";
import { recentes } from "./contexto";

type Linha = Processo | { id: number; naoEncontrado: true };

/** Os processos que este navegador abriu ou consultou (a API não tem listagem), e a consulta por número. */
export function Inicio() {
  const ids = recentes.useValor();
  const [numero, setNumero] = useState("");
  const lista = useRecurso(`recentes:${ids.join(",")}`, async () => {
    const linhas = await Promise.all(ids.map(async (id): Promise<Linha> => {
      const r = await processos.consultar(id);
      return r.ok ? r.dados : { id, naoEncontrado: true };
    }));
    return { ok: true as const, status: 200, dados: linhas };
  });

  return (
    <Cartao titulo="Recentes" acoes={<>
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); if (numero.trim()) ir(`/processo/${numero.trim()}`); }}>
        <Input inputMode="numeric" placeholder="Número do processo" aria-label="Número do processo" className="w-44"
          value={numero} onChange={(e) => setNumero(e.target.value)} />
        <Button type="submit" variant="secondary"><Search aria-hidden />Consultar</Button>
      </form>
      <Button asChild><a href="#/abrir"><Plus aria-hidden />Abrir processo</a></Button>
    </>}>
      {ids.length === 0 ? <Vazio>Nenhum processo ainda. Abra o primeiro, ou consulte um pelo número.</Vazio>
        : lista.situacao !== "ok" ? <Vazio>Carregando...</Vazio>
        : <Tabela<Linha> linhas={lista.dados} chave={(l) => l.id} aoEscolher={(l) => ir(`/processo/${l.id}`)} colunas={[
            { titulo: "Número", valor: (l) => l.id, numero: true },
            { titulo: "Cota", valor: (l) => ("naoEncontrado" in l ? <span className="text-muted-foreground">não encontrado</span> : l.numeroCota) },
            { titulo: "Tipo", valor: (l) => ("naoEncontrado" in l ? "" : tipo(l.tipo).texto) },
            { titulo: "Valor", valor: (l) => ("naoEncontrado" in l ? "" : reais(l.valor)), numero: true },
            { titulo: "Situação", valor: (l) => ("naoEncontrado" in l ? "" : <Etiqueta tom={situacao(l.estado).tom}>{situacao(l.estado).texto}</Etiqueta>) },
          ]} />}
      <p className="mt-3 text-xs text-muted-foreground">Lista deste navegador: os processos que você abriu ou consultou aqui.</p>
    </Cartao>
  );
}
