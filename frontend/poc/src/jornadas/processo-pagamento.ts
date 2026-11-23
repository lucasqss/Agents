// O desenho da jornada do processo de pagamento: termos do negócio para cada código da API. É DADO: sem condição,
// sem valor de regra (a alçada é do backend). As chaves são tipadas pelo contrato: se o serviço criar uma situação
// ou ação nova, o typecheck exige o rótulo aqui. O vocabulário é o mesmo das especificações de aceitação.
import type { Acao, Situacao, Tipo } from "@/api/processos";
import type { Tom } from "@/componentes";

export const SITUACOES: Record<Situacao, { texto: string; tom: Tom }> = {
  ABERTO: { texto: "Aberto", tom: "neutro" },
  EM_ANALISE: { texto: "Em análise", tom: "andamento" },
  PENDENTE_DOCUMENTO: { texto: "Pendente de documento", tom: "espera" },
  AGUARDANDO_VISTORIA: { texto: "Aguardando vistoria", tom: "espera" },
  AGUARDANDO_ALCADA: { texto: "Aguardando alçada", tom: "espera" },
  APROVADO: { texto: "Aprovado", tom: "andamento" },
  PAGO: { texto: "Pago", tom: "sucesso" },
  REPROVADO: { texto: "Reprovado", tom: "erro" },
  CANCELADO: { texto: "Cancelado", tom: "erro" },
};

/** A ordem aqui é a ordem dos botões. `motivo`: a tela pede o motivo num diálogo (quem exige de fato é o backend). */
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

/** O caminho principal de cada tipo, para o negócio ver onde o processo está. `talvez`: etapa que só acontece em alguns casos. */
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

/** Textos de ajuda da jornada (sem número de regra: o valor da alçada é do backend). */
export const TEXTOS = {
  ajudaDoValor: "Acima da alçada, o processo precisa da aprovação de quem tem alçada.",
  motivo: "Esta ação exige um motivo. Ele fica registrado no histórico do processo.",
  semAcoes: "Nenhuma ação: o processo está numa situação final.",
  perfil: "Nesta versão todos veem as ações da situação; o perfil de cada pessoa ainda vai ser definido.",
};

// Tradução de código para o desenho. Alguns campos do contrato chegam como string (ex.: ProcessoResumo.estado):
// estes tradutores aceitam string e devolvem o próprio código se ele for desconhecido, em vez de quebrar a tela.
export const situacao = (codigo: string) => SITUACOES[codigo as Situacao] ?? { texto: codigo, tom: "neutro" };
export const acao = (codigo: string) => ACOES[codigo as Acao] ?? { texto: codigo };
export const tipo = (codigo: string) => TIPOS[codigo as Tipo] ?? { texto: codigo, explicacao: "" };
export const caminho = (codigo: string) => CAMINHOS[codigo as Tipo] ?? [];
