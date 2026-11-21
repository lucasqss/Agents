import type { Transporte } from "./api";

/** O serviço de verdade. Ele precisa liberar CORS para a origem do protótipo (só no perfil de dev). */
export function transporteHttp(base: string): Transporte {
  return {
    descricao: base,
    async chamar({ metodo, caminho, corpo, cabecalhos, resposta: formato }) {
      const url = base + caminho;
      const headers: Record<string, string> = { ...cabecalhos };
      let body: BodyInit | undefined;
      if (corpo instanceof FormData) {
        body = corpo; // o navegador põe o Content-Type multipart, com o separador
      } else if (corpo !== undefined) {
        headers["Content-Type"] = "application/json";
        body = JSON.stringify(corpo);
      }
      const r = await fetch(url, { method: metodo, headers, body });
      if (formato === "arquivo" && r.ok) return { status: r.status, resposta: await r.blob(), url };
      const texto = await r.text();
      let resposta: unknown = null;
      try {
        resposta = texto ? JSON.parse(texto) : null;
      } catch {
        resposta = { mensagem: texto };
      }
      return { status: r.status, resposta, url };
    },
  };
}
