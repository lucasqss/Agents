import type { InputHTMLAttributes, ReactNode } from "react";

/** Rótulo + controle + ajuda + erro. O controle vem como filho (Entrada, select, Opcoes...). */
export function Campo({ rotulo, ajuda, erro, children }: { rotulo: string; ajuda?: ReactNode; erro?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-neutral-300">{rotulo}</span>
      {children}
      {ajuda && !erro && <p className="text-xs text-neutral-500">{ajuda}</p>}
      {erro && <p className="text-xs text-red-400">{erro}</p>}
    </label>
  );
}

export function Entrada(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={
        "w-full rounded-lg border border-neutral-700 bg-neutral-950 px-3 py-2 text-sm text-neutral-100 placeholder:text-neutral-600 " +
        "focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30 " + (props.className ?? "")
      }
    />
  );
}

/** Escolha única em cartões (rádio), com título e explicação de cada opção. */
export function Opcoes<T extends string>({ nome, valor, opcoes, aoMudar }:
  { nome: string; valor: T; opcoes: { valor: T; titulo: string; explicacao?: string }[]; aoMudar: (v: T) => void }) {
  return (
    <div role="radiogroup" className="grid gap-3 sm:grid-cols-2">
      {opcoes.map((o) => (
        <label key={o.valor}
          className={`cursor-pointer rounded-lg border p-3.5 transition-colors ${valor === o.valor
            ? "border-blue-500 bg-blue-500/10" : "border-neutral-700 bg-neutral-950 hover:border-neutral-600"}`}>
          <input type="radio" name={nome} value={o.valor} checked={valor === o.valor} onChange={() => aoMudar(o.valor)} className="sr-only" />
          <span className="block text-sm font-medium text-neutral-100">{o.titulo}</span>
          {o.explicacao && <span className="mt-1 block text-xs text-neutral-400">{o.explicacao}</span>}
        </label>
      ))}
    </div>
  );
}
