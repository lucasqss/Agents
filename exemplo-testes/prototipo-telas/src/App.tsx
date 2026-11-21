import { FilePlus2, ListChecks } from "lucide-react";
import { useRota } from "./apoio/rotas";
import { Layout } from "./componentes";
import { persona } from "./jornada/estado";
import { PERSONAS } from "./jornada/vocabulario";
import { AbrirProcesso } from "./telas/AbrirProcesso";
import { Inicio } from "./telas/Inicio";
import { Processo } from "./telas/Processo";

/** Rotas e moldura do protótipo. Tela nova: um caso aqui e um item no menu, se for de primeiro nível. */
export function App() {
  const { partes, busca } = useRota();
  const usuario = persona.usar();

  let tela: { titulo: string; menu: "inicio" | "abrir"; trilha: { texto: string; href?: string }[]; corpo: React.ReactNode };
  if (partes[0] === "abrir") {
    tela = { titulo: "Abrir processo", menu: "abrir", trilha: [{ texto: "Processos", href: "#/" }, { texto: "Abrir processo" }], corpo: <AbrirProcesso /> };
  } else if (partes[0] === "processo" && partes[1]) {
    const id = partes[1];
    tela = { titulo: `Processo ${id}`, menu: "inicio", trilha: [{ texto: "Processos", href: "#/" }, { texto: `Processo ${id}` }],
      corpo: <Processo key={id} id={id} recemAberto={busca.has("aberto")} /> };
  } else {
    tela = { titulo: "Processos", menu: "inicio", trilha: [{ texto: "Processos" }], corpo: <Inicio /> };
  }
  document.title = `${tela.titulo} · Processos de pagamento`;

  const eu = PERSONAS.find((p) => p.usuario === usuario) ?? PERSONAS[0];
  return (
    <Layout sigla="PP" nome="Processos de pagamento" titulo={tela.titulo} trilha={tela.trilha}
      menu={[
        { texto: "Processos", href: "#/", ativo: tela.menu === "inicio", icone: <ListChecks className="size-4" aria-hidden /> },
        { texto: "Abrir processo", href: "#/abrir", ativo: tela.menu === "abrir", icone: <FilePlus2 className="size-4" aria-hidden /> },
      ]}
      rodape={
        <label className="flex items-center gap-3 rounded-lg border border-neutral-800 bg-neutral-950 p-2.5">
          <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-full bg-neutral-800 text-xs font-semibold text-neutral-200">
            {eu.nome.slice(0, 2).toUpperCase()}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block text-xs text-neutral-500">Quem sou eu</span>
            <select value={usuario} onChange={(e) => persona.gravar(e.target.value)} aria-label="Quem sou eu"
              className="w-full truncate bg-transparent text-sm text-neutral-100 focus:outline-none">
              {PERSONAS.map((p) => <option key={p.usuario} value={p.usuario} className="bg-neutral-900">{p.nome} · {p.papel}</option>)}
            </select>
          </span>
        </label>
      }>
      {tela.corpo}
    </Layout>
  );
}
