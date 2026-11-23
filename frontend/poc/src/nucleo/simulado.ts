/**
 * Serviço simulado no navegador, com a cara de um `fetch`: o openapi-fetch chama esta função no lugar da rede quando
 * VITE_MODO=falso. As rotas (mesmos caminhos, status e formato de erro do serviço de verdade) vêm do projeto, em
 * src/simulado/. Assim a tela não muda ao trocar de modo.
 */
export interface PedidoSimulado {
  parametros: Record<string, string>;
  busca: URLSearchParams;
  corpo: unknown;
  cabecalhos: Record<string, string>;
}

export interface RespostaSimulada {
  status: number;
  resposta?: unknown;
}

export interface RotaSimulada {
  metodo: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Ex.: "/processos/:id/acoes/:acao". */
  caminho: string;
  tratar(pedido: PedidoSimulado): RespostaSimulada;
}

/** Resposta de erro no formato da API (ErroResponse). */
export const erroSimulado = (status: number, mensagem: string, campos: { campo: string | null; erro: string }[] = []): RespostaSimulada =>
  ({ status, resposta: { mensagem, campos } });

function casar(padrao: string, caminho: string): Record<string, string> | null {
  const a = padrao.split("/").filter(Boolean);
  const b = caminho.split("/").filter(Boolean);
  if (a.length !== b.length) return null;
  const parametros: Record<string, string> = {};
  for (let i = 0; i < a.length; i++) {
    const p = a[i]!;
    if (p.startsWith(":")) parametros[p.slice(1)] = decodeURIComponent(b[i]!);
    else if (p !== b[i]) return null;
  }
  return parametros;
}

export async function fetchSimulado(pedido: Request, rotas: RotaSimulada[]): Promise<Response> {
  await new Promise((r) => setTimeout(r, 120)); // um pouco de espera, para os estados de carregando aparecerem
  const url = new URL(pedido.url);
  const texto = await pedido.clone().text();
  let corpo: unknown = null;
  try {
    corpo = texto ? JSON.parse(texto) : null;
  } catch {
    corpo = texto;
  }
  const cabecalhos: Record<string, string> = {};
  pedido.headers.forEach((v, k) => (cabecalhos[k.toLowerCase()] = v));
  for (const rota of rotas) {
    if (rota.metodo !== pedido.method) continue;
    const parametros = casar(rota.caminho, url.pathname);
    if (!parametros) continue;
    const { status, resposta = null } = rota.tratar({ parametros, busca: url.searchParams, corpo, cabecalhos });
    return new Response(status === 204 ? null : JSON.stringify(resposta), { status, headers: { "Content-Type": "application/json" } });
  }
  return new Response(JSON.stringify({ mensagem: `Rota simulada inexistente: ${pedido.method} ${url.pathname}`, campos: [] }),
    { status: 404, headers: { "Content-Type": "application/json" } });
}
