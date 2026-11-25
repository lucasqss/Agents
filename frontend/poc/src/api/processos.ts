// Os endpoints do serviço de processos, um por função. É o único lugar com caminho de endpoint. Os tipos vêm do
// contrato (esquema.d.ts, gerado por `npm run gerar:api` do serviço + do proposto): se o contrato mudar, o typecheck
// aponta o que quebrou aqui. As operações do contrato PROPOSTO passam por proposta(): no modo real, enquanto o serviço
// não as publicar, devolvem 501 sem chamar.
import { comoFormulario, criarCliente, proposta, resposta } from "@/nucleo/http";
import type { components, paths } from "./esquema";
import { PROPOSTAS_PENDENTES } from "./propostas.gerado";

type Esquema = components["schemas"];
export type Processo = Esquema["ProcessoResumo"];
export type Transicao = Esquema["ProcessoTransicaoResumo"];
export type NovoProcesso = Esquema["NovoProcesso"];
export type Situacao = Esquema["EstadoProcesso"];
export type Acao = Esquema["AcaoProcesso"];
export type Tipo = Esquema["TipoProcesso"];
export type PaginaDeProcessos = Esquema["PaginaDeProcessos"];
export type Documento = Esquema["DocumentoDoProcesso"];
export type TipoDocumento = Esquema["TipoDocumento"];

const api = criarCliente<paths>();
const pendente = (operacao: string) => PROPOSTAS_PENDENTES.has(operacao);

export const processos = {
  abrir: (novo: NovoProcesso) => resposta(api.POST("/processos", { body: novo })),

  consultar: (id: number) => resposta(api.GET("/processos/{id}", { params: { path: { id } } })),

  acoes: (id: number) => resposta(api.GET("/processos/{id}/acoes", { params: { path: { id } } })),

  historico: (id: number) => resposta(api.GET("/processos/{id}/historico", { params: { path: { id } } })),

  /** `usuario`: quem age (cabeçalho X-Usuario, obrigatório no contrato). `motivo`: só nas ações que o exigem. */
  executar: (id: number, acao: Acao, usuario: string, motivo?: string) =>
    resposta(api.POST("/processos/{id}/acoes/{acao}", {
      params: { path: { id, acao }, header: { "X-Usuario": usuario } },
      body: motivo === undefined ? undefined : { motivo },
    })),

  // ---- contrato proposto ----

  /** Proposta: lista paginada, com filtro opcional por situação. */
  listar: (filtro: { situacao?: Situacao; pagina: number; tamanho: number }) =>
    proposta(pendente("GET /processos"), () => resposta(api.GET("/processos", { params: { query: filtro } }))),

  /** Proposta: os documentos anexados ao processo. */
  documentos: (id: number) =>
    proposta(pendente("GET /processos/{id}/documentos"), () => resposta(api.GET("/processos/{id}/documentos", { params: { path: { id } } }))),

  /** Proposta: anexa um arquivo ao processo (multipart). */
  anexarDocumento: (id: number, tipo: TipoDocumento, arquivo: File, usuario: string) =>
    proposta(pendente("POST /processos/{id}/documentos"), () => resposta(api.POST("/processos/{id}/documentos", {
      params: { path: { id }, header: { "X-Usuario": usuario } },
      body: { tipo, arquivo },
      bodySerializer: comoFormulario,
    }))),
};
