import { Code2 } from "lucide-react";
import { useSyncExternalStore } from "react";
import { MODO, chamadasRecentes, ouvirChamadas, type Chamada } from "@/nucleo/http";

function comoTexto(c: Chamada) {
  return `${c.metodo} ${c.url}\n` + Object.entries(c.cabecalhos).map(([k, v]) => `${k}: ${v}`).join("\n") +
    (c.corpo !== undefined ? `\n\n${JSON.stringify(c.corpo, null, 2)}` : "") +
    `\n\n→ ${c.status}\n${JSON.stringify(c.resposta, null, 2)}`;
}

/** As últimas chamadas à API, cruas (a mais recente primeiro): quem testa vê exatamente o que foi e o que voltou. */
export function PorTrasDaTela() {
  const chamadas = useSyncExternalStore(ouvirChamadas, chamadasRecentes);
  return (
    <details className="rounded-xl border bg-card/50">
      <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-3 text-sm text-muted-foreground hover:text-foreground">
        <Code2 className="size-4" aria-hidden />
        Por trás da tela: as últimas chamadas à API
        <span className="ml-auto text-xs">{MODO === "falso" ? "modo simulado" : "serviço real"}</span>
      </summary>
      <pre data-testid="chamadas" className="max-h-96 overflow-auto border-t px-4 py-3 font-mono text-xs leading-relaxed">
        {chamadas.length ? chamadas.map(comoTexto).join("\n\n────────\n\n") : "Nenhuma chamada ainda."}
      </pre>
    </details>
  );
}
