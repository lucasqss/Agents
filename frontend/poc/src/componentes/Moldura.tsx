import { FlaskConical } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";
import { MODO } from "@/nucleo/http";
import { PorTrasDaTela } from "./PorTrasDaTela";

export interface ItemDeMenu { texto: string; href: string; ativo: boolean; icone?: ReactNode }

/**
 * O esqueleto de todas as telas: barra lateral (marca, selo "Protótipo", menu, rodapé com a persona) e conteúdo
 * (trilha, título, a tela, "Por trás da tela"). No celular, a barra lateral vira barra de topo.
 */
export function Moldura({ sigla, nome, menu, rodape, trilha, titulo, children }: {
  sigla: string; nome: string; menu: ItemDeMenu[]; rodape?: ReactNode;
  trilha: { texto: string; href?: string }[]; titulo: string; children: ReactNode;
}) {
  return (
    <div className="min-h-dvh md:grid md:grid-cols-[260px_1fr]">
      <aside className="flex flex-col gap-5 border-b bg-card/60 p-4 md:sticky md:top-0 md:h-dvh md:border-r md:border-b-0 md:p-5">
        <div className="flex items-center justify-between gap-3 md:flex-col md:items-start">
          <a href="#/" className="flex items-center gap-2.5 font-semibold">
            <span aria-hidden className="flex size-8 items-center justify-center rounded-lg bg-primary text-xs font-bold text-primary-foreground">{sigla}</span>
            {nome}
          </a>
          <span title="Telas para conversar sobre as jornadas e testar a API. Não é o produto final."
            className="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full border border-espera/30 bg-espera/10 px-2.5 py-0.5 text-xs font-medium text-espera">
            <FlaskConical className="size-3.5" aria-hidden />
            Protótipo{MODO === "falso" ? " · simulado" : ""}
          </span>
        </div>
        <nav aria-label="Principal" className="flex gap-1 overflow-x-auto md:flex-col">
          {menu.map((m) => (
            <a key={m.href} href={m.href} aria-current={m.ativo ? "page" : undefined}
              className={cn("flex shrink-0 items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors",
                m.ativo ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent/50 hover:text-foreground")}>
              {m.icone}
              {m.texto}
            </a>
          ))}
        </nav>
        {rodape && <div className="md:mt-auto">{rodape}</div>}
      </aside>
      <div className="mx-auto w-full max-w-5xl space-y-5 p-4 md:p-8">
        <header className="space-y-1">
          <nav aria-label="Você está em" className="flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
            {trilha.map((t, i) => (
              <span key={t.texto} className="flex items-center gap-1.5">
                {i > 0 && <span aria-hidden>›</span>}
                {t.href ? <a href={t.href} className="hover:text-foreground">{t.texto}</a> : <span>{t.texto}</span>}
              </span>
            ))}
          </nav>
          <h1 className="text-2xl font-semibold tracking-tight">{titulo}</h1>
        </header>
        <main aria-live="polite" className="space-y-5">{children}</main>
        <PorTrasDaTela />
      </div>
    </div>
  );
}
