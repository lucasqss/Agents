import type { Metodo, Transporte } from "./api";

/**
 * O serviço simulado no navegador. O projeto declara as rotas (src/jornada/servicoFalso.ts) com o mesmo
 * contrato do serviço de verdade: caminhos, status e formato de erro. Assim a tela não muda ao trocar de modo.
 */
export interface PedidoFalso {
  parametros: Record<string, string>;
  /** O que veio depois do "?" no caminho. */
  busca: URLSearchParams;
  /** JSON já como objeto; FormData vira um objeto { campo: File | string }. */
  corpo: unknown;
  cabecalhos: Record<string, string>;
}

export interface RespostaFalsa {
  status: number;
  resposta?: unknown;
}

export interface RotaFalsa {
  metodo: Metodo;
  /** Ex.: "/processos/:id/acoes/:acao". */
  caminho: string;
  tratar(pedido: PedidoFalso): RespostaFalsa;
}

/** Atalhos para as respostas de erro no formato da API. */
export const erro = (status: number, mensagem: string, campos: { campo: string | null; erro: string }[] = []): RespostaFalsa =>
  ({ status, resposta: { mensagem, campos } });

function casar(padrao: string, caminho: string): Record<string, string> | null {
  const a = padrao.split("/").filter(Boolean);
  const b = caminho.split("?")[0].split("/").filter(Boolean);
  if (a.length !== b.length) return null;
  const parametros: Record<string, string> = {};
  for (let i = 0; i < a.length; i++) {
    if (a[i].startsWith(":")) parametros[a[i].slice(1)] = decodeURIComponent(b[i]);
    else if (a[i] !== b[i]) return null;
  }
  return parametros;
}

export function transporteFalso(rotas: RotaFalsa[]): Transporte {
  return {
    descricao: "serviço simulado no navegador",
    async chamar({ metodo, caminho, corpo, cabecalhos }) {
      await new Promise((r) => setTimeout(r, 120)); // um pouco de espera, para os estados de carregando aparecerem
      const url = `(simulado) ${caminho}`;
      for (const rota of rotas) {
        if (rota.metodo !== metodo) continue;
        const parametros = casar(rota.caminho, caminho);
        if (!parametros) continue;
        // Cópia: o serviço simulado não pode mexer no objeto que a tela mandou (File e Blob são clonáveis; FormData não).
        const copia = corpo instanceof FormData ? Object.fromEntries(corpo.entries()) : structuredClone(corpo ?? null);
        const busca = new URLSearchParams(caminho.split("?")[1] ?? "");
        const { status, resposta = null } = rota.tratar({ parametros, busca, corpo: copia, cabecalhos: cabecalhos ?? {} });
        return { status, resposta: structuredClone(resposta), url };
      }
      return { status: 404, resposta: { mensagem: `Rota simulada inexistente: ${metodo} ${caminho}`, campos: [] }, url };
    },
  };
}
