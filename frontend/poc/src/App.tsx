import { FilePlus2, ListChecks, Rows3 } from "lucide-react";
import { useEffect, type ReactNode } from "react";
import { Aviso, Cartao, Moldura, SeletorNativo } from "@/componentes";
import { PERSONAS } from "@/jornadas/processo-pagamento";
import { useRota } from "@/nucleo/rotas";
import { AbrirProcesso } from "@/telas/AbrirProcesso";
import { AnexarDocumento } from "@/telas/AnexarDocumento";
import { persona, useEu } from "@/telas/contexto";
import { Inicio } from "@/telas/Inicio";
import { ListaProcessos } from "@/telas/ListaProcessos";
import { Processo } from "@/telas/Processo";

/** Rotas e moldura. Tela nova: um caso aqui e, se for de primeiro nível, um item no menu. */
export function App() {
  const { partes, busca } = useRota();
  const eu = useEu();

  let tela: { titulo: string; menu: "inicio" | "abrir" | "todos"; trilha: { texto: string; href?: string }[]; corpo: ReactNode };
  if (partes[0] === "abrir") {
    tela = { titulo: "Abrir processo", menu: "abrir", trilha: [{ texto: "Processos", href: "#/" }, { texto: "Abrir processo" }], corpo: <AbrirProcesso /> };
  } else if (partes[0] === "processos") {
    tela = { titulo: "Todos os processos", menu: "todos", trilha: [{ texto: "Processos", href: "#/" }, { texto: "Todos os processos" }], corpo: <ListaProcessos /> };
  } else if (partes[0] === "processo" && partes[1] && partes[2] === "anexar") {
    const id = Number(partes[1]);
    tela = {
      titulo: "Anexar documento", menu: "inicio",
      trilha: [{ texto: "Processos", href: "#/" }, { texto: `Processo ${partes[1]}`, href: `#/processo/${partes[1]}` }, { texto: "Anexar documento" }],
      corpo: Number.isInteger(id) && id > 0 ? <AnexarDocumento key={id} id={id} /> : <Aviso tipo="erro">Número de processo inválido: {partes[1]}</Aviso>,
    };
  } else if (partes[0] === "processo" && partes[1]) {
    const id = Number(partes[1]);
    const avisoDeEntrada = busca.has("aberto") ? "Processo aberto." : busca.has("anexado") ? "Documento anexado." : undefined;
    tela = {
      titulo: `Processo ${partes[1]}`, menu: "inicio", trilha: [{ texto: "Processos", href: "#/" }, { texto: `Processo ${partes[1]}` }],
      corpo: Number.isInteger(id) && id > 0
        ? <Processo key={`${id}:${avisoDeEntrada ?? ""}`} id={id} {...(avisoDeEntrada ? { avisoDeEntrada } : {})} />
        : <Aviso tipo="erro">Número de processo inválido: {partes[1]}</Aviso>,
    };
  } else if (partes.length === 0) {
    tela = { titulo: "Processos", menu: "inicio", trilha: [{ texto: "Processos" }], corpo: <Inicio /> };
  } else {
    tela = { titulo: "Página não encontrada", menu: "inicio", trilha: [{ texto: "Processos", href: "#/" }, { texto: "Página não encontrada" }],
      corpo: <NaoEncontrada /> };
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
        { texto: "Todos os processos", href: "#/processos", ativo: tela.menu === "todos", icone: <Rows3 className="size-4" aria-hidden /> },
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

function NaoEncontrada() {
  return (
    <Cartao>
      <p className="text-sm text-muted-foreground">Esta página não existe. O endereço pode estar errado ou ter mudado.</p>
      <a href="#/" className="mt-3 inline-block text-sm font-medium text-andamento underline-offset-4 hover:underline">Voltar aos processos</a>
    </Cartao>
  );
}
