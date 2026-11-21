import { ArrowLeftRight, ClipboardCheck, FilePlus2, FolderUp, Mail } from "lucide-react";
import { useEffect } from "react";
import { useRota } from "./apoio/rotas";
import { Layout, type ItemDeMenu } from "./componentes";
import { garantirPersonaDa, persona } from "./jornada/estado";
import { PERSONAS, PERSONAS_DA_JORNADA, type Jornada } from "./jornada/vocabulario";
import { Fila } from "./telas/analise/Fila";
import { ProcessoEmAnalise } from "./telas/analise/ProcessoEmAnalise";
import { Criacao, PASSOS_DA_CRIACAO, type PassoDaCriacao } from "./telas/criacao/Criacao";
import { EmailsEnviados } from "./telas/criacao/EmailsEnviados";
import { CaixaDeEmail } from "./telas/documentos/CaixaDeEmail";
import { EnvioDaParte } from "./telas/documentos/EnvioDaParte";
import { EscolherJornada } from "./telas/EscolherJornada";

const NOME_DA_JORNADA: Record<Jornada, string> = { criacao: "Criar o processo", documentos: "Enviar os documentos", analise: "Analisar" };

interface Tela { jornada: Jornada | null; titulo: string; trilha: { texto: string; href?: string }[]; corpo: React.ReactNode }

function escolherTela(partes: string[]): Tela {
  const [jornada, a, b] = partes;
  const inicio = { texto: "Jornadas", href: "#/" };
  if (jornada === "criacao") {
    const trilha = [inicio, { texto: NOME_DA_JORNADA.criacao, href: "#/criacao" }];
    if (a === "criado" && b) return { jornada: "criacao", titulo: `Processo ${b} criado`, trilha: [...trilha, { texto: "E-mails enviados" }], corpo: <EmailsEnviados processoId={b} /> };
    const passo = (PASSOS_DA_CRIACAO.some((p) => p.chave === a) ? a : "cota") as PassoDaCriacao;
    return { jornada: "criacao", titulo: "Novo processo de pagamento", trilha, corpo: <Criacao passo={passo} /> };
  }
  if (jornada === "documentos") {
    const trilha = [inicio, { texto: NOME_DA_JORNADA.documentos, href: "#/documentos" }];
    if (a) return { jornada: "documentos", titulo: "Envio de documentos", trilha: [...trilha, { texto: "Pelo link" }], corpo: <EnvioDaParte key={a} link={a} /> };
    return { jornada: "documentos", titulo: "Caixa de e-mail (simulada)", trilha, corpo: <CaixaDeEmail /> };
  }
  if (jornada === "analise") {
    const trilha = [inicio, { texto: NOME_DA_JORNADA.analise, href: "#/analise" }];
    if (a) return { jornada: "analise", titulo: `Processo ${a}`, trilha: [...trilha, { texto: `Processo ${a}` }], corpo: <ProcessoEmAnalise key={a} id={a} /> };
    return { jornada: "analise", titulo: "Fila de análise", trilha, corpo: <Fila /> };
  }
  return { jornada: null, titulo: "Escolha a jornada", trilha: [{ texto: "Jornadas" }], corpo: <EscolherJornada /> };
}

const ICONE = { size: "size-4" };
const MENU_INICIAL: ItemDeMenu[] = [
  { texto: "1. Criar o processo", href: "#/criacao", ativo: false, icone: <FilePlus2 className={ICONE.size} aria-hidden /> },
  { texto: "2. Enviar os documentos", href: "#/documentos", ativo: false, icone: <FolderUp className={ICONE.size} aria-hidden /> },
  { texto: "3. Analisar", href: "#/analise", ativo: false, icone: <ClipboardCheck className={ICONE.size} aria-hidden /> },
];

/** Rotas e moldura. Cada jornada tem as suas telas e as suas personas; a inicial escolhe a jornada. */
export function App() {
  const { partes } = useRota();
  const usuario = persona.usar();
  const tela = escolherTela(partes);

  useEffect(() => { if (tela.jornada) garantirPersonaDa(tela.jornada); }, [tela.jornada]);
  useEffect(() => { document.title = `${tela.titulo} · Processos de pagamento`; }, [tela.titulo]);

  const menu: ItemDeMenu[] = tela.jornada === null ? MENU_INICIAL : [
    ...({
      criacao: [{ texto: "Novo processo", href: "#/criacao", ativo: true, icone: <FilePlus2 className={ICONE.size} aria-hidden /> }],
      documentos: [{ texto: "Caixa de e-mail", href: "#/documentos", ativo: partes.length === 1, icone: <Mail className={ICONE.size} aria-hidden /> }],
      analise: [{ texto: "Fila de análise", href: "#/analise", ativo: partes.length === 1, icone: <ClipboardCheck className={ICONE.size} aria-hidden /> }],
    } as Record<Jornada, ItemDeMenu[]>)[tela.jornada],
    { texto: "Trocar de jornada", href: "#/", ativo: false, icone: <ArrowLeftRight className={ICONE.size} aria-hidden /> },
  ];

  const permitidas = tela.jornada ? PERSONAS_DA_JORNADA[tela.jornada] : [];
  const eu = PERSONAS.find((p) => p.usuario === usuario) ?? PERSONAS[0];
  const rodape = tela.jornada === "documentos" ? (
    <p className="rounded-lg border border-neutral-800 bg-neutral-950 p-3 text-xs text-neutral-400">
      Nesta jornada não há login: quem entra é quem tem o link do e-mail. Garantir que a parte é ela mesma fica fora do escopo.
    </p>
  ) : permitidas.length ? (
    <label className="flex items-center gap-3 rounded-lg border border-neutral-800 bg-neutral-950 p-2.5">
      <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-full bg-neutral-800 text-xs font-semibold text-neutral-200">
        {eu.nome.slice(0, 2).toUpperCase()}
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-xs text-neutral-500">Quem sou eu</span>
        <select value={usuario} onChange={(e) => persona.gravar(e.target.value)} aria-label="Quem sou eu"
          className="w-full truncate bg-transparent text-sm text-neutral-100 focus:outline-none">
          {PERSONAS.filter((p) => permitidas.includes(p.usuario)).map((p) => (
            <option key={p.usuario} value={p.usuario} className="bg-neutral-900">{p.nome} · {p.descricao}</option>
          ))}
        </select>
      </span>
    </label>
  ) : undefined;

  return (
    <Layout sigla="PC" nome="Pagamento de consórcio" titulo={tela.titulo} trilha={tela.trilha} menu={menu} rodape={rodape}>
      {tela.corpo}
    </Layout>
  );
}
