/**
 * O protótipo fala com o serviço só por aqui. Há dois transportes com a mesma cara:
 * - http: o serviço de verdade (VITE_API, padrão http://localhost:8080);
 * - falso: o serviço simulado no navegador (VITE_MODO=falso), para mostrar a jornada sem backend e para o e2e.
 * As telas não sabem qual está em uso. Esta pasta é genérica: em outro projeto, fica como está.
 */
import { transporteFalso } from "./apiFalsa";
import { transporteHttp } from "./apiHttp";
import type { RotaFalsa } from "./apiFalsa";

export type Metodo = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

/** Formato de erro da API: `campos` só vem em erro de validação de entrada. */
export interface ErroApi {
  mensagem: string;
  campos: { campo: string | null; erro: string }[];
}

export type Resposta<T> =
  | { ok: true; status: number; dados: T }
  | { ok: false; status: number; erro: ErroApi };

/** Uma chamada como ela aconteceu: o que o painel "Por trás da tela" mostra. */
export interface Chamada {
  metodo: Metodo;
  url: string;
  cabecalhos: Record<string, string>;
  corpo?: unknown;
  status: number;
  resposta: unknown;
}

export interface Pedido {
  metodo: Metodo;
  caminho: string;
  /** Objeto vira JSON; FormData vai como multipart (upload de arquivo). */
  corpo?: unknown;
  cabecalhos?: Record<string, string>;
  /** "arquivo": a resposta é o conteúdo de um arquivo (Blob), não JSON. */
  resposta?: "json" | "arquivo";
}

export interface Transporte {
  descricao: string;
  chamar(pedido: Pedido): Promise<{ status: number; resposta: unknown; url: string }>;
}

// ---------- escolha do transporte ----------

export const MODO = import.meta.env.VITE_MODO === "falso" ? "falso" : "http";
export const URL_DA_API: string = import.meta.env.VITE_API ?? "http://localhost:8080";

let transporte: Transporte | null = null;
let rotasFalsas: RotaFalsa[] = [];

/** O projeto entrega as rotas do serviço simulado (src/jornada/servicoFalso.ts). */
export function registrarServicoFalso(rotas: RotaFalsa[]) {
  rotasFalsas = rotas;
  transporte = null;
}

function atual(): Transporte {
  transporte ??= MODO === "falso" ? transporteFalso(rotasFalsas) : transporteHttp(URL_DA_API);
  return transporte;
}

export function descricaoDoTransporte() {
  return atual().descricao;
}

// ---------- registro das chamadas ----------

let ultima: Chamada | null = null;
const ouvintes = new Set<() => void>();

export function ultimaChamada() {
  return ultima;
}

export function ouvirChamadas(ouvinte: () => void) {
  ouvintes.add(ouvinte);
  return () => ouvintes.delete(ouvinte);
}

// ---------- a chamada ----------

export async function chamar<T>(pedido: Pedido): Promise<Resposta<T>> {
  const cabecalhos = { ...(pedido.cabecalhos ?? {}) };
  let status = 0;
  let resposta: unknown;
  let url = pedido.caminho;
  try {
    ({ status, resposta, url } = await atual().chamar({ ...pedido, cabecalhos }));
  } catch (e) {
    resposta = { mensagem: `Sem resposta de ${atual().descricao}. O serviço está no ar? (${(e as Error).message})`, campos: [] };
  }
  ultima = { metodo: pedido.metodo, url, cabecalhos, corpo: paraMostrar(pedido.corpo), status, resposta: paraMostrar(resposta) };
  ouvintes.forEach((o) => o());

  if (status >= 200 && status < 300) return { ok: true, status, dados: resposta as T };
  const r = (resposta ?? {}) as Partial<ErroApi>;
  return { ok: false, status, erro: { mensagem: r.mensagem ?? `Erro ${status || "de rede"}`, campos: r.campos ?? [] } };
}

/** Arquivo não cabe no painel: mostra nome, tipo e tamanho. */
function paraMostrar(valor: unknown): unknown {
  const arquivo = (b: Blob) => `[arquivo${b instanceof File ? ` ${b.name}` : ""}, ${b.type || "sem tipo"}, ${Math.ceil(b.size / 1024)} KB]`;
  if (valor instanceof FormData) {
    return Object.fromEntries([...valor.entries()].map(([k, v]) => [k, v instanceof Blob ? arquivo(v) : v]));
  }
  return valor instanceof Blob ? arquivo(valor) : valor;
}
