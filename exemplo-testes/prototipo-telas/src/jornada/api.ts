// As chamadas da jornada, tipadas. As telas usam só estas funções; o transporte (real ou simulado) fica em src/api.
import { chamar } from "../api/api";
import { personaAtual } from "./estado";
import type { Acao, Situacao, Tipo } from "./vocabulario";

export interface Processo {
  id: number;
  tipo: Tipo;
  numeroCota: string;
  valor: number;
  estado: Situacao;
}

export interface Transicao {
  de: Situacao;
  para: Situacao;
  acao: Acao;
  quando: string;
  autor: string;
  observacao: string | null;
}

const comoPersona = () => ({ "X-Usuario": personaAtual().usuario });

export const processos = {
  abrir: (novo: { tipo: Tipo; numeroCota: string; valor: number }) =>
    chamar<{ id: number }>({ metodo: "POST", caminho: "/processos", corpo: novo, cabecalhos: comoPersona() }),

  consultar: (id: number | string) =>
    chamar<Processo>({ metodo: "GET", caminho: `/processos/${encodeURIComponent(id)}`, cabecalhos: comoPersona() }),

  acoes: (id: number | string) =>
    chamar<Acao[]>({ metodo: "GET", caminho: `/processos/${encodeURIComponent(id)}/acoes`, cabecalhos: comoPersona() }),

  historico: (id: number | string) =>
    chamar<Transicao[]>({ metodo: "GET", caminho: `/processos/${encodeURIComponent(id)}/historico`, cabecalhos: comoPersona() }),

  executar: (id: number, acao: Acao, motivo?: string) =>
    chamar<{ estado: Situacao }>({
      metodo: "POST", caminho: `/processos/${id}/acoes/${acao}`,
      corpo: motivo === undefined ? undefined : { motivo }, cabecalhos: comoPersona(),
    }),
};
