import { FlaskConical } from "lucide-react";
import type { ReactNode } from "react";
import { MODO } from "../api/api";
import { PorTrasDaTela } from "./PorTrasDaTela";

export interface ItemDeMenu {
  texto: string;
  href: string;
  ativo: boolean;
  icone?: ReactNode;
}

/**
 * Esqueleto de todas as telas: barra lateral (marca, selo, menu, rodapé com a persona) e área de conteúdo
 * (trilha, título, a tela, "Por trás da tela"). No celular, a barra lateral vira barra de topo.
 */
export function Layout({ sigla, nome, menu, rodape, trilha, titulo, children }: {
  sigla: string;
  nome: string;
  menu: ItemDeMenu[];
  rodape?: ReactNode;
  trilha: { texto: string; href?: string }[];
  titulo: string;
  children: ReactNode;
}) {
  return (
    <div className="min-h-dvh md:grid md:grid-cols-[260px_1fr]">
      <aside className="flex flex-col gap-5 border-b border-neutral-800 bg-neutral-900/70 p-4 md:sticky md:top-0 md:h-dvh md:border-r md:border-b-0 md:p-5">
        <div className="flex items-center justify-between gap-3 md:flex-col md:items-start">
          <a href="#/" className="flex items-center gap-2.5 font-semibold text-neutral-100">
            <span aria-hidden className="flex size-8 items-center justify-center rounded-lg bg-blue-600 text-xs font-bold text-white">{sigla}</span>
            {nome}
          </a>
          <span title="Telas para conversar sobre as jornadas e testar a API. Não é o produto final."
            className="inline-flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-300 ring-1 ring-inset ring-amber-500/30">
            <FlaskConical className="size-3.5" aria-hidden />
            Protótipo{MODO === "falso" ? " · simulado" : ""}
          </span>
        </div>
        <nav aria-label="Principal" className="flex gap-1 overflow-x-auto md:flex-col">
          {menu.map((m) => (
            <a key={m.href} href={m.href} aria-current={m.ativo ? "page" : undefined}
              className={`flex shrink-0 items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors ${m.ativo
                ? "bg-neutral-800 text-neutral-100" : "text-neutral-400 hover:bg-neutral-800/50 hover:text-neutral-200"}`}>
              {m.icone}
              {m.texto}
            </a>
          ))}
        </nav>
        {rodape && <div className="md:mt-auto">{rodape}</div>}
      </aside>

      <div className="mx-auto w-full max-w-5xl space-y-5 p-4 md:p-8">
        <header className="space-y-1">
          <nav aria-label="Você está em" className="flex flex-wrap items-center gap-1.5 text-xs text-neutral-500">
            {trilha.map((t, i) => (
              <span key={t.texto} className="flex items-center gap-1.5">
                {i > 0 && <span aria-hidden>›</span>}
                {t.href ? <a href={t.href} className="hover:text-neutral-300">{t.texto}</a> : <span>{t.texto}</span>}
              </span>
            ))}
          </nav>
          <h1 className="text-2xl font-semibold tracking-tight text-neutral-100">{titulo}</h1>
        </header>
        <main aria-live="polite" className="space-y-5">{children}</main>
        <PorTrasDaTela />
      </div>
    </div>
  );
}
