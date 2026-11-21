import { useEffect, useRef, useState } from "react";
import { Botao } from "./Botao";

/**
 * Pede um texto (ex.: o motivo de uma ação) num diálogo modal. Aberto quando `titulo` não é nulo;
 * `aoConfirmar` recebe o texto, `aoCancelar` fecha sem fazer nada.
 */
export function DialogoTexto({ titulo, explicacao, obrigatorio = true, aoConfirmar, aoCancelar }:
  { titulo: string | null; explicacao?: string; obrigatorio?: boolean; aoConfirmar: (texto: string) => void; aoCancelar: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  const [texto, setTexto] = useState("");

  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (titulo !== null && !d.open) {
      setTexto("");
      d.showModal();
    } else if (titulo === null && d.open) {
      d.close();
    }
  }, [titulo]);

  return (
    <dialog ref={ref} onCancel={(e) => { e.preventDefault(); aoCancelar(); }}
      className="m-auto w-[min(92vw,480px)] rounded-xl border border-neutral-700 bg-neutral-900 p-0 text-neutral-100 shadow-2xl backdrop:bg-black/60">
      <form className="space-y-4 p-6" onSubmit={(e) => { e.preventDefault(); aoConfirmar(texto.trim()); }}>
        <h2 className="text-lg font-semibold">{titulo}</h2>
        {explicacao && <p className="text-sm text-neutral-400">{explicacao}</p>}
        <textarea autoFocus rows={4} maxLength={500} value={texto} onChange={(e) => setTexto(e.target.value)}
          aria-label={titulo ?? "Texto"} placeholder="Escreva aqui"
          className="w-full rounded-lg border border-neutral-700 bg-neutral-950 px-3 py-2 text-sm placeholder:text-neutral-600 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/30" />
        <div className="flex justify-end gap-2">
          <Botao type="button" onClick={aoCancelar}>Voltar</Botao>
          <Botao type="submit" variante="principal" disabled={obrigatorio && !texto.trim()}>Confirmar</Botao>
        </div>
      </form>
    </dialog>
  );
}
