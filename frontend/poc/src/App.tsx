import { FilePlus2, ListChecks } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { Aviso, Moldura, SeletorNativo } from "@/componentes";
import { PERSONAS } from "@/jornadas/processo-pagamento";
import { useRota } from "@/nucleo/rotas";
import { AbrirProcesso } from "@/telas/AbrirProcesso";
import { persona, useEu } from "@/telas/contexto";
import { Inicio } from "@/telas/Inicio";
import { Processo } from "@/telas/Processo";

/** Rotas e moldura. Tela nova: um caso aqui e, se for de primeiro nível, um item no menu. */
export function App() {
  const { partes, busca } = useRota();
  const eu = useEu();

  let tela: { titulo: string; menu: "inicio" | "abrir"; trilha: { texto: string; href?: string }[]; corpo: ReactNode };
  if (partes[0] === "abrir") {
    tela = { titulo: "Abrir processo", menu: "abrir", trilha: [{ texto: "Processos", href: "#/" }, { texto: "Abrir processo" }], corpo: <AbrirProcesso /> };
  } else if (partes[0] === "processo" && partes[1]) {
    const id = Number(partes[1]);
    tela = {
      titulo: `Processo ${partes[1]}`, menu: "inicio", trilha: [{ texto: "Processos", href: "#/" }, { texto: `Processo ${partes[1]}` }],
      corpo: Number.isInteger(id) && id > 0
        ? <Processo key={id} id={id} recemAberto={busca.has("aberto")} />
        : <Aviso tipo="erro">Número de processo inválido: {partes[1]}</Aviso>,
    };
  } else {
    tela = { titulo: "Processos", menu: "inicio", trilha: [{ texto: "Processos" }], corpo: <Inicio /> };
  }
  const titulo = tela.titulo;
  useEffect(() => {
    document.title = `${titulo} · Processos de pagamento`;
  }, [titulo]);

  return (
    <Moldura sigla="PP" nome="Processos de pagamento" titulo={tela.titulo} trilha={tela.trilha}
      menu={[
        { texto: "Processos", href: "#/", ativo: tela.menu === "inicio", icone: <ListChecks className="size-4" aria-hidden /> },
        { texto: "Abrir processo", href: "#/abrir", ativo: tela.menu === "abrir", icone: <FilePlus2 className="size-4" aria-hidden /> },
      ]}
      rodape={
        <label className="flex items-center gap-3 rounded-lg border bg-background p-2.5">
          <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold">
            {eu.nome.slice(0, 2).toUpperCase()}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-xs text-muted-foreground">Quem sou eu</span>
            <SeletorNativo aria-label="Quem sou eu" value={eu.usuario} onChange={(e) => persona.gravar(e.target.value)} className="border-0 px-0">
              {PERSONAS.map((p) => <option key={p.usuario} value={p.usuario} className="bg-popover">{p.nome} · {p.papel}</option>)}
            </SeletorNativo>
          </span>
        </label>
      }>
      {tela.corpo}
    </Moldura>
  );
}
