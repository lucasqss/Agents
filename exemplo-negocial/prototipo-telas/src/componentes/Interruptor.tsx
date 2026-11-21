/** Chave liga/desliga, com rótulo. */
export function Interruptor({ rotulo, ligado, aoMudar }: { rotulo: string; ligado: boolean; aoMudar: (v: boolean) => void }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2.5 text-sm text-neutral-300">
      <button type="button" role="switch" aria-checked={ligado} aria-label={rotulo} onClick={() => aoMudar(!ligado)}
        className={`relative h-5 w-9 shrink-0 rounded-full transition-colors ${ligado ? "bg-blue-600" : "bg-neutral-700"}`}>
        <span className={`absolute top-0.5 left-0 size-4 rounded-full bg-white transition-transform ${ligado ? "translate-x-4.5" : "translate-x-0.5"}`} />
      </button>
      <span aria-hidden>{rotulo}</span>
    </label>
  );
}
