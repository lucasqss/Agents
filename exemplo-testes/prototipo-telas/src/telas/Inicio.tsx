import { Plus, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { ir } from "../apoio/rotas";
import { reais } from "../apoio/formato";
import { Botao, BotaoLink, Cartao, Entrada, Etiqueta, Tabela, Vazio } from "../componentes";
import { processos, type Processo } from "../jornada/api";
import { recentes } from "../jornada/estado";
import { SITUACOES, TIPOS } from "../jornada/vocabulario";

type Linha = Processo | { id: number; naoEncontrado: true };

export function Inicio() {
  const ids = recentes.usar();
  const [linhas, setLinhas] = useState<Linha[] | null>(null);
  const [numero, setNumero] = useState("");

  useEffect(() => {
    let vivo = true;
    Promise.all(ids.map(async (id): Promise<Linha> => {
      const r = await processos.consultar(id);
      return r.ok ? r.dados : { id, naoEncontrado: true };
    })).then((l) => vivo && setLinhas(l));
    return () => { vivo = false; };
  }, [ids]);

  return (
    <Cartao titulo="Recentes" acoes={<>
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); if (numero.trim()) ir(`/processo/${numero.trim()}`); }}>
        <Entrada inputMode="numeric" placeholder="Número do processo" aria-label="Número do processo"
          value={numero} onChange={(e) => setNumero(e.target.value)} className="w-44" />
        <Botao type="submit" icone={<Search className="size-4" aria-hidden />}>Consultar</Botao>
      </form>
      <BotaoLink href="#/abrir" variante="principal" icone={<Plus className="size-4" aria-hidden />}>Abrir processo</BotaoLink>
    </>}>
      {ids.length === 0 ? <Vazio>Nenhum processo ainda. Abra o primeiro, ou consulte um pelo número.</Vazio>
        : linhas === null ? <Vazio>Carregando...</Vazio>
        : <Tabela<Linha> linhas={linhas} chave={(l) => l.id} aoClicar={(l) => ir(`/processo/${l.id}`)} colunas={[
            { titulo: "Número", valor: (l) => l.id, numero: true },
            { titulo: "Cota", valor: (l) => ("naoEncontrado" in l ? <span className="text-neutral-500">não encontrado</span> : l.numeroCota) },
            { titulo: "Tipo", valor: (l) => ("naoEncontrado" in l ? "" : TIPOS[l.tipo]?.texto ?? l.tipo) },
            { titulo: "Valor", valor: (l) => ("naoEncontrado" in l ? "" : reais(l.valor)), numero: true },
            { titulo: "Situação", valor: (l) => ("naoEncontrado" in l ? "" : <Etiqueta tom={SITUACOES[l.estado]?.tom}>{SITUACOES[l.estado]?.texto ?? l.estado}</Etiqueta>) },
          ]} />}
      <p className="mt-3 text-xs text-neutral-500">Lista deste navegador: os processos que você abriu ou consultou aqui.</p>
    </Cartao>
  );
}
