/** Abas (filtros de uma lista), com contador opcional. */
export function Abas<T extends string>({ abas, atual, aoMudar }:
  { abas: { valor: T; texto: string; contador?: number }[]; atual: T; aoMudar: (v: T) => void }) {
  return (
    <div role="tablist" className="-mx-1 flex gap-1 overflow-x-auto border-b border-neutral-800 px-1">
      {abas.map((a) => (
        <button key={a.valor} role="tab" type="button" aria-selected={a.valor === atual} onClick={() => aoMudar(a.valor)}
          className={`-mb-px flex shrink-0 items-center gap-2 border-b-2 px-3 py-2.5 text-sm transition-colors ${a.valor === atual
            ? "border-blue-500 text-neutral-100" : "border-transparent text-neutral-400 hover:text-neutral-200"}`}>
          {a.texto}
          {a.contador !== undefined && (
            <span className={`rounded-full px-1.5 text-xs tabular-nums ${a.valor === atual ? "bg-blue-500/20 text-blue-200" : "bg-neutral-800 text-neutral-400"}`}>{a.contador}</span>
          )}
        </button>
      ))}
    </div>
  );
}
