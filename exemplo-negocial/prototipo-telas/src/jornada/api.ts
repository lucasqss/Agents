// As chamadas das jornadas, tipadas. As telas usam só estas funções.
// Endpoints marcados PROPOSTA ainda não existem no serviço: só o serviço simulado os atende (ver contrato-proposto.md).
import { chamar, type Pedido } from "../api/api";
import { demonstracao, personaAtual } from "./estado";
import { tokenDe } from "./token";
import type { Acao, PapelDaParte, Situacao, SituacaoDoDocumento, Tipo } from "./vocabulario";

// ---------- formatos ----------

export interface Cota {
  id: number; numero: string; valorCredito: number; creditoDisponivel: number; clienteId: number; clienteNome: string;
  processoAtualId: number | null; processoAtualTipo: string | null; processoAtualEstado: Situacao | null;
}
export interface ProcessoResumo {
  id: number; cotaId: number; cotaNumero: string; tipo: Tipo; valor: number; estado: Situacao; protocoloPagamento: string | null;
}
export interface Bem { dados: Record<string, string>; valor: number }
export interface Parte { id: number; nome: string; documento: string; papel: PapelDaParte; email: string; valor: number | null }
export interface ProcessoDetalhe extends ProcessoResumo { bens: Bem[]; partes: Parte[] }
export interface Favorecido { nome: string; documento: string; papel: string; valor: number }
export interface Transicao {
  de: Situacao; para: Situacao; acao: string; quando: string; autor: string; observacao: string | null;
  valorAnterior: number | null; valorNovo: number | null;
}
export interface Pagina<T> { itens: T[]; pagina: number; tamanho: number; total: number }
export interface DocumentoExigido {
  id: number; parteId: number; codigo: string; texto: string; bem: string | null;
  situacao: SituacaoDoDocumento; arquivo: string | null; enviadoEm: string | null; motivoRecusa: string | null;
}
export interface Envio {
  processoId: number; tipo: Tipo; situacaoDoProcesso: Situacao;
  parte: { nome: string; papel: PapelDaParte }; documentos: DocumentoExigido[];
}
export interface Email {
  id: number; para: string; nome: string; assunto: string; texto: string; quando: string; link: string;
  tipo: "CONVITE" | "REENVIO"; processoId: number;
}
export interface NovoProcesso {
  cotaId: number; tipo: Tipo; valor: number;
  bens: Bem[];
  partes: Omit<Parte, "id">[];
  favorecidos: Favorecido[];
}

// ---------- quem chama ----------

async function comoPersona(): Promise<Record<string, string>> {
  const p = personaAtual();
  const clienteId = p.papel === "cliente" ? demonstracao.ler()?.clienteId : undefined;
  return { Authorization: `Bearer ${await tokenDe(p.usuario, [p.papel], clienteId)}` };
}

/** Como Ana (backoffice), para preparar a demonstração sem trocar a persona da tela. */
async function comoBackoffice(): Promise<Record<string, string>> {
  return { Authorization: `Bearer ${await tokenDe("ana", ["backoffice"])}` };
}

async function pedir<T>(pedido: Omit<Pedido, "cabecalhos">, cabecalhos?: Record<string, string>) {
  return chamar<T>({ ...pedido, cabecalhos: cabecalhos ?? (await comoPersona()) });
}

const id = (v: number | string) => encodeURIComponent(v);

// ---------- cadastro (demonstração) ----------

export const cadastro = {
  criarCliente: async (nome: string, cpf: string) =>
    pedir<{ id: number }>({ metodo: "POST", caminho: "/clientes", corpo: { nome, cpf } }, await comoBackoffice()),
  criarCota: async (numero: string, valorCredito: number, clienteId: number) =>
    pedir<{ id: number }>({ metodo: "POST", caminho: "/cotas", corpo: { numero, valorCredito, clienteId } }, await comoBackoffice()),
};

// ---------- cotas ----------

export const cotas = {
  detalhar: (cotaId: number) => pedir<Cota>({ metodo: "GET", caminho: `/cotas/${id(cotaId)}` }),
  /** PROPOSTA (fase 2) */
  doCliente: (clienteId: number) => pedir<Cota[]>({ metodo: "GET", caminho: `/clientes/${id(clienteId)}/cotas` }),
};

// ---------- processos ----------

export const processos = {
  criar: (novo: NovoProcesso) => pedir<{ id: number }>({ metodo: "POST", caminho: "/processos", corpo: novo }),
  listar: (estado?: Situacao) =>
    pedir<Pagina<ProcessoResumo>>({ metodo: "GET", caminho: `/processos?tamanho=100${estado ? `&estado=${estado}` : ""}` }),
  /** PROPOSTA (fase 2): o serviço só tem a lista, sem o detalhe de um processo. */
  detalhar: (processoId: number | string) => pedir<ProcessoDetalhe>({ metodo: "GET", caminho: `/processos/${id(processoId)}` }),
  acoes: (processoId: number | string) => pedir<{ acoes: Acao[] }>({ metodo: "GET", caminho: `/processos/${id(processoId)}/acoes` }),
  historico: (processoId: number | string) => pedir<Transicao[]>({ metodo: "GET", caminho: `/processos/${id(processoId)}/historico` }),
  favorecidos: (processoId: number | string) => pedir<Favorecido[]>({ metodo: "GET", caminho: `/processos/${id(processoId)}/favorecidos` }),
  /** Ações que só mudam a situação. Com motivo, vai { motivo }; sem, nada (a observação é opcional). */
  executar: (processoId: number, acao: Acao, motivo?: string) =>
    pedir<{ estado: Situacao }>({ metodo: "POST", caminho: `/processos/${processoId}/${acao}`, corpo: motivo === undefined ? undefined : { motivo } }),
};

// ---------- documentos (PROPOSTA, fase 2) ----------

export const documentos = {
  doProcesso: (processoId: number | string) =>
    pedir<DocumentoExigido[]>({ metodo: "GET", caminho: `/processos/${id(processoId)}/documentos` }),
  arquivo: (processoId: number, documentoId: number) =>
    pedir<Blob>({ metodo: "GET", caminho: `/processos/${processoId}/documentos/${documentoId}/arquivo`, resposta: "arquivo" }),
  aceitar: (processoId: number, documentoId: number) =>
    pedir<DocumentoExigido>({ metodo: "POST", caminho: `/processos/${processoId}/documentos/${documentoId}/aceitar` }),
  recusar: (processoId: number, documentoId: number, motivo: string) =>
    pedir<DocumentoExigido>({ metodo: "POST", caminho: `/processos/${processoId}/documentos/${documentoId}/recusar`, corpo: { motivo } }),
};

// ---------- envio pela parte, pelo link do e-mail (PROPOSTA, fase 2): sem login ----------

export const envios = {
  ver: (link: string) => pedir<Envio>({ metodo: "GET", caminho: `/envios/${id(link)}` }, {}),
  enviar: (link: string, documentoId: number, arquivo: File) => {
    const corpo = new FormData();
    corpo.append("arquivo", arquivo);
    return pedir<DocumentoExigido>({ metodo: "POST", caminho: `/envios/${id(link)}/documentos/${documentoId}`, corpo }, {});
  },
};

// ---------- só em desenvolvimento (PROPOSTA, fase 2) ----------

export const desenvolvimento = {
  /** A caixa de e-mail simulada: o que o serviço "enviou" às partes. */
  emails: () => pedir<Email[]>({ metodo: "GET", caminho: "/dev/emails" }, {}),
  /** Roda a rotina de pagamento agora (no serviço real ela roda sozinha a cada 5 minutos). */
  rodarRotina: () => pedir<{ processados: number }>({ metodo: "POST", caminho: "/dev/rotina-de-pagamento" }, {}),
};
