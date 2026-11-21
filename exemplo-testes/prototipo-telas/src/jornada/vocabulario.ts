// Vocabulário da jornada: termo do negócio <-> código da API.
// É o mesmo das especificações de aceitação (src/test/java/cucumber/passos/Vocabulario.java).
import type { Tom } from "../componentes";

export type Situacao =
  | "ABERTO" | "EM_ANALISE" | "PENDENTE_DOCUMENTO" | "AGUARDANDO_VISTORIA" | "AGUARDANDO_ALCADA"
  | "APROVADO" | "PAGO" | "REPROVADO" | "CANCELADO";

export type Acao =
  | "INICIAR_ANALISE" | "SOLICITAR_DOCUMENTO" | "RECEBER_DOCUMENTO" | "CONCLUIR_ANALISE" | "CONCLUIR_VISTORIA"
  | "APROVAR_NA_ALCADA" | "DEVOLVER_PARA_ANALISE" | "PAGAR" | "REPROVAR" | "CANCELAR";

export type Tipo = "SERVICO" | "IMOBILIARIO";

export const SITUACOES: Record<Situacao, { texto: string; tom: Tom; final?: boolean }> = {
  ABERTO: { texto: "Aberto", tom: "neutro" },
  EM_ANALISE: { texto: "Em análise", tom: "andamento" },
  PENDENTE_DOCUMENTO: { texto: "Pendente de documento", tom: "espera" },
  AGUARDANDO_VISTORIA: { texto: "Aguardando vistoria", tom: "espera" },
  AGUARDANDO_ALCADA: { texto: "Aguardando alçada", tom: "espera" },
  APROVADO: { texto: "Aprovado", tom: "andamento" },
  PAGO: { texto: "Pago", tom: "sucesso", final: true },
  REPROVADO: { texto: "Reprovado", tom: "erro", final: true },
  CANCELADO: { texto: "Cancelado", tom: "erro", final: true },
};

/** A ordem aqui é a ordem dos botões. `motivo`: a ação exige motivo (regra R5). `perigo`: muda o estilo. */
export const ACOES: Record<Acao, { texto: string; motivo?: boolean; perigo?: boolean }> = {
  INICIAR_ANALISE: { texto: "Iniciar a análise" },
  SOLICITAR_DOCUMENTO: { texto: "Solicitar documento", motivo: true },
  RECEBER_DOCUMENTO: { texto: "Receber o documento" },
  CONCLUIR_ANALISE: { texto: "Concluir a análise" },
  CONCLUIR_VISTORIA: { texto: "Concluir a vistoria" },
  APROVAR_NA_ALCADA: { texto: "Aprovar na alçada" },
  DEVOLVER_PARA_ANALISE: { texto: "Devolver para análise", motivo: true },
  PAGAR: { texto: "Pagar" },
  REPROVAR: { texto: "Reprovar", motivo: true, perigo: true },
  CANCELAR: { texto: "Cancelar", motivo: true, perigo: true },
};

export const TIPOS: Record<Tipo, { texto: string; explicacao: string }> = {
  SERVICO: { texto: "Serviço", explicacao: "Análise e, acima da alçada, aprovação." },
  IMOBILIARIO: { texto: "Imobiliário", explicacao: "Análise, vistoria do imóvel e, acima da alçada, aprovação." },
};

/** O caminho principal de cada tipo, para o negócio ver onde o processo está e o que falta. `talvez`: só acima da alçada. */
export const CAMINHOS: Record<Tipo, { situacao: Situacao; talvez?: boolean }[]> = {
  SERVICO: [{ situacao: "ABERTO" }, { situacao: "EM_ANALISE" }, { situacao: "AGUARDANDO_ALCADA", talvez: true }, { situacao: "APROVADO" }, { situacao: "PAGO" }],
  IMOBILIARIO: [{ situacao: "ABERTO" }, { situacao: "EM_ANALISE" }, { situacao: "AGUARDANDO_VISTORIA" }, { situacao: "AGUARDANDO_ALCADA", talvez: true }, { situacao: "APROVADO" }, { situacao: "PAGO" }],
};

/** Quem usa o protótipo. O `usuario` vai no cabeçalho X-Usuario e aparece no histórico. */
export const PERSONAS = [
  { usuario: "ana", nome: "Ana", papel: "analista" },
  { usuario: "vitor", nome: "Vitor", papel: "vistoriador" },
  { usuario: "carla", nome: "Carla", papel: "aprovadora (alçada)" },
  { usuario: "tesouraria", nome: "Tesouraria", papel: "pagamento" },
] as const;

/** R4: até este valor, inclusive, aprova direto; acima, precisa de quem tem alçada. Igual a processo.alcada do serviço. */
export const ALCADA = 50_000;
