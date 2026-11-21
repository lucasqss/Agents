import { Code2 } from "lucide-react";
import { useSyncExternalStore } from "react";
import { MODO, descricaoDoTransporte, ouvirChamadas, ultimaChamada } from "../api/api";

/** A última chamada à API, crua: para quem testa ver exatamente o que foi e o que voltou. */
export function PorTrasDaTela() {
  const c = useSyncExternalStore(ouvirChamadas, ultimaChamada);
  const texto = c
    ? `${c.metodo} ${c.url}\n` +
      Object.entries(c.cabecalhos).map(([k, v]) => `${k}: ${v}`).join("\n") +
      (c.corpo !== undefined ? `\n\n${JSON.stringify(c.corpo, null, 2)}` : "") +
      `\n\n→ ${c.status || "sem resposta"}\n${JSON.stringify(c.resposta, null, 2)}`
    : "Nenhuma chamada ainda.";
  return (
    <details className="group rounded-xl border border-neutral-800 bg-neutral-900/40">
      <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-3 text-sm text-neutral-400 hover:text-neutral-200">
        <Code2 className="size-4" aria-hidden />
        Por trás da tela: a última chamada à API
        <span className="ml-auto text-xs text-neutral-600">{MODO === "falso" ? "modo simulado" : descricaoDoTransporte()}</span>
      </summary>
      <pre data-testid="ultima-chamada" className="max-h-80 overflow-auto border-t border-neutral-800 px-4 py-3 font-mono text-xs leading-relaxed text-neutral-300">{texto}</pre>
    </details>
  );
}
