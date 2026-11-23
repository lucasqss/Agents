// Os endpoints do serviço de processos, um por função. É o único lugar com caminho de endpoint. Os tipos vêm do
// contrato (esquema.d.ts, gerado por `npm run gerar:api`): se o backend mudar, o typecheck aponta o que quebrou aqui.
import { criarCliente, resposta } from "@/nucleo/http";
import type { components, paths } from "./esquema";

type Esquema = components["schemas"];
export type Processo = Esquema["ProcessoResumo"];
export type Transicao = Esquema["ProcessoTransicaoResumo"];
export type NovoProcesso = Esquema["NovoProcesso"];
export type Situacao = Esquema["EstadoProcesso"];
export type Acao = Esquema["AcaoProcesso"];
export type Tipo = Esquema["TipoProcesso"];

const api = criarCliente<paths>();

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
};
