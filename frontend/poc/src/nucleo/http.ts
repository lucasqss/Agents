/**
 * O único lugar que fala HTTP. As telas não sabem se o serviço é o de verdade ou o simulado no navegador:
 *   - real: VITE_API (padrão http://localhost:8080);
 *   - simulado: VITE_MODO=falso, com as rotas registradas por registrarSimulado() (ver src/simulado/).
 * Toda chamada fica registrada para o painel "Por trás da tela". Esta pasta é genérica: em outro projeto, fica igual.
 */
import createClient, { type Client, type Middleware } from "openapi-fetch";
import { fetchSimulado, type RotaSimulada } from "./simulado";

export const MODO: "real" | "falso" = import.meta.env.VITE_MODO === "falso" ? "falso" : "real";
const URL_DA_API: string = MODO === "falso" ? "http://simulado" : (import.meta.env.VITE_API ?? "http://localhost:8080");

/** Formato de erro da API (ErroResponse): `campos` só vem preenchido em erro de validação de entrada. */
export interface ErroApi {
  mensagem: string;
  campos: { campo: string | null; erro: string }[];
}

/** O resultado de uma chamada: os dados (2xx) ou o erro da API, sempre com o status. Nunca lança. */
export type Resposta<T> = { ok: true; status: number; dados: T } | { ok: false; status: number; erro: ErroApi };

// ---------- registro das chamadas (painel "Por trás da tela") ----------

export interface Chamada {
  metodo: string;
  url: string;
  cabecalhos: Record<string, string>;
  corpo?: unknown;
  status: number;
  resposta: unknown;
}

/** As últimas chamadas, a mais recente primeiro (uma ação e as recargas que vêm depois dela). */
let recentes: readonly Chamada[] = [];
const ouvintes = new Set<() => void>();
export const chamadasRecentes = () => recentes;
export function ouvirChamadas(ouvinte: () => void) {
  ouvintes.add(ouvinte);
  return () => void ouvintes.delete(ouvinte);
}

async function lerCorpo(r: Request | Response): Promise<unknown> {
  const texto = await r.clone().text();
  if (!texto) return undefined;
  try {
    return JSON.parse(texto);
  } catch {
    return texto;
  }
}

// O corpo do pedido é lido antes do envio (depois, o transporte já o consumiu); a resposta o encontra pelo id.
const corpos = new Map<string, unknown>();

const registrar: Middleware = {
  async onRequest({ request, id }) {
    corpos.set(id, await lerCorpo(request));
  },
  async onResponse({ request, response, id }) {
    const corpo = corpos.get(id);
    corpos.delete(id);
    const cabecalhos: Record<string, string> = {};
    request.headers.forEach((v, k) => (cabecalhos[k.replace(/(^|-)[a-z]/g, (c) => c.toUpperCase())] = v));
    const chamada: Chamada = {
      metodo: request.method,
      url: request.url.replace(URL_DA_API, MODO === "falso" ? "(simulado) " : URL_DA_API),
      cabecalhos,
      corpo,
      status: response.status,
      resposta: await lerCorpo(response),
    };
    recentes = [chamada, ...recentes].slice(0, 5);
    ouvintes.forEach((o) => o());
  },
};

// ---------- o cliente ----------

let rotasSimuladas: RotaSimulada[] = [];

/** O projeto entrega as rotas do serviço simulado (src/simulado/servicoFalso.ts). */
export function registrarSimulado(rotas: RotaSimulada[]) {
  rotasSimuladas = rotas;
}

/** Cliente tipado pelo contrato (`paths` gerado do OpenAPI). Só a camada api o cria e o usa. */
export function criarCliente<Paths extends object>(): Client<Paths> {
  const cliente = createClient<Paths>({
    baseUrl: URL_DA_API,
    fetch: MODO === "falso" ? (pedido: Request) => fetchSimulado(pedido, rotasSimuladas) : undefined,
  });
  cliente.use(registrar);
  return cliente;
}

/** Converte o retorno do openapi-fetch em Resposta. Falha de rede vira status 0 com mensagem, não exceção. */
export async function resposta<D>(chamada: Promise<{ data?: D; error?: unknown; response: Response }>): Promise<Resposta<D>> {
  try {
    const { data, error, response } = await chamada;
    if (response.ok) return { ok: true, status: response.status, dados: data as D };
    const e = (error ?? {}) as Partial<{ mensagem: string | null; campos: ErroApi["campos"] }>;
    return { ok: false, status: response.status, erro: { mensagem: e.mensagem ?? `Erro ${response.status}`, campos: e.campos ?? [] } };
  } catch (e) {
    return { ok: false, status: 0, erro: { mensagem: `Sem resposta de ${URL_DA_API}. O serviço está no ar? (${(e as Error).message})`, campos: [] } };
  }
}

/**
 * Operação do contrato PROPOSTO (telas primeiro). No modo real, se o serviço ainda não a publicou (`pendente`), não
 * chama: devolve 501 com uma mensagem clara, e a tela mostra "ainda não disponível". No modo simulado, chama normalmente.
 */
export function proposta<T>(pendente: boolean, chamar: () => Promise<Resposta<T>>): Promise<Resposta<T>> {
  if (MODO === "real" && pendente) {
    return Promise.resolve({ ok: false, status: 501, erro: { mensagem: "Ainda não disponível no serviço: esta parte é uma proposta das telas.", campos: [] } });
  }
  return chamar();
}

/** Corpo multipart (envio de arquivo): { tipo: "X", arquivo: File } vira FormData. Use em bodySerializer. */
export function comoFormulario(corpo: unknown): FormData {
  const formulario = new FormData();
  for (const [campo, valor] of Object.entries(corpo as Record<string, unknown>)) {
    if (valor === undefined || valor === null) continue;
    if (valor instanceof Blob) formulario.append(campo, valor);
    else if (typeof valor === "string" || typeof valor === "number" || typeof valor === "boolean") formulario.append(campo, String(valor));
    else formulario.append(campo, JSON.stringify(valor));
  }
  return formulario;
}

/** Várias chamadas de uma vez, numa Resposta só: falha se qualquer uma falhar (com o erro da primeira que falhou). */
export async function todas<T extends Record<string, Promise<Resposta<unknown>>>>(
  chamadas: T,
): Promise<Resposta<{ [K in keyof T]: T[K] extends Promise<Resposta<infer D>> ? D : never }>> {
  const chaves = Object.keys(chamadas) as (keyof T)[];
  const resultados = await Promise.all(chaves.map((k) => chamadas[k]!));
  const falha = resultados.find((r) => !r.ok);
  if (falha && !falha.ok) return falha;
  const dados = Object.fromEntries(chaves.map((k, i) => [k, (resultados[i] as { dados: unknown }).dados]));
  return { ok: true, status: 200, dados: dados as { [K in keyof T]: T[K] extends Promise<Resposta<infer D>> ? D : never } };
}
