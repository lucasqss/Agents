import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variante = "normal" | "principal" | "perigo";

const ESTILO: Record<Variante, string> = {
  normal: "bg-neutral-800 text-neutral-100 hover:bg-neutral-700 ring-1 ring-inset ring-neutral-700",
  principal: "bg-blue-600 text-white hover:bg-blue-500",
  perigo: "bg-red-500/10 text-red-300 hover:bg-red-500/20 ring-1 ring-inset ring-red-500/40",
};

const BASE =
  "inline-flex items-center justify-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors " +
  "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-500 disabled:cursor-not-allowed disabled:opacity-50";

export function Botao({ variante = "normal", icone, children, ...resto }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variante?: Variante; icone?: ReactNode }) {
  return (
    <button {...resto} className={`${BASE} ${ESTILO[variante]} ${resto.className ?? ""}`}>
      {icone}
      {children}
    </button>
  );
}

/** Mesmo visual do botão, para navegar (é um link, não dispara nada). */
export function BotaoLink({ variante = "normal", icone, href, children }:
  { variante?: Variante; icone?: ReactNode; href: string; children: ReactNode }) {
  return (
    <a href={href} className={`${BASE} ${ESTILO[variante]}`}>
      {icone}
      {children}
    </a>
  );
}
