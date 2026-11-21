// Vocabulário das jornadas: termo do negócio <-> código da API.
// Fonte: o caso de uso confirmado (src/test/resources/aceitacao/pagamento-processo/caso-de-uso.md) e as decisões das
// telas (bens, partes, documentos, tipos auto e eletro), que ainda vão para o caso de uso na fase 2. Ver contrato-proposto.md.
import type { Tom } from "../componentes";

export type Situacao =
  | "AGUARDANDO_DOCUMENTOS" | "ABERTO" | "EM_ANALISE" | "PENDENTE_DOCUMENTO" | "AGUARDANDO_VISTORIA" | "AGUARDANDO_ALCADA"
  | "AGUARDANDO_CONCORDANCIA" | "APROVADO" | "PAGAMENTO_SOLICITADO" | "PAGO" | "REPROVADO" | "CANCELADO";

export const SITUACOES: Record<Situacao, { texto: string; tom: Tom; final?: boolean }> = {
  AGUARDANDO_DOCUMENTOS: { texto: "Aguardando documentos", tom: "espera" }, // PROPOSTA (fase 2)
  ABERTO: { texto: "Pronto para análise", tom: "neutro" },
  EM_ANALISE: { texto: "Em análise", tom: "andamento" },
  PENDENTE_DOCUMENTO: { texto: "Pendente de documento", tom: "espera" },
  AGUARDANDO_VISTORIA: { texto: "Aguardando vistoria", tom: "espera" },
  AGUARDANDO_ALCADA: { texto: "Aguardando alçada", tom: "espera" },
  AGUARDANDO_CONCORDANCIA: { texto: "Aguardando concordância", tom: "espera" },
  APROVADO: { texto: "Aprovado", tom: "andamento" },
  PAGAMENTO_SOLICITADO: { texto: "Pagamento solicitado", tom: "andamento" },
  PAGO: { texto: "Pago", tom: "sucesso", final: true },
  REPROVADO: { texto: "Reprovado", tom: "erro", final: true },
  CANCELADO: { texto: "Cancelado", tom: "erro", final: true },
};

/** Ações no formato do caminho do endpoint (POST /processos/{id}/<ação>), como GET /processos/{id}/acoes devolve. */
export type Acao =
  | "iniciar-analise" | "solicitar-documento" | "receber-documento" | "concluir-analise" | "concluir-vistoria"
  | "aprovar-na-alcada" | "devolver-para-analise" | "solicitar-pagamento" | "reprovar" | "cancelar"
  | "alterar" | "propor-alteracao" | "concordar" | "discordar";

/**
 * Texto, se exige motivo e se é ação de risco. `naAnalise`: aparece como botão na jornada de análise.
 * Solicitar e receber documento saem da tela: com o envio pelas partes, o documento é recusado um a um e a parte
 * reenvia (decisão das telas, a confirmar no caso de uso). Alterar, concordar e discordar são do cliente.
 */
export const ACOES: Record<Acao, { texto: string; motivo?: boolean; perigo?: boolean; naAnalise?: boolean }> = {
  "iniciar-analise": { texto: "Iniciar a análise", naAnalise: true },
  "concluir-analise": { texto: "Concluir a análise", naAnalise: true },
  "concluir-vistoria": { texto: "Concluir a vistoria", naAnalise: true },
  "aprovar-na-alcada": { texto: "Aprovar na alçada", naAnalise: true },
  "devolver-para-analise": { texto: "Devolver para análise", motivo: true, naAnalise: true },
  "solicitar-pagamento": { texto: "Solicitar o pagamento", naAnalise: true },
  "reprovar": { texto: "Reprovar", motivo: true, perigo: true, naAnalise: true },
  "cancelar": { texto: "Cancelar", motivo: true, perigo: true, naAnalise: true },
  "solicitar-documento": { texto: "Solicitar documento", motivo: true },
  "receber-documento": { texto: "Receber o documento" },
  "propor-alteracao": { texto: "Propor alteração", motivo: true },
  "alterar": { texto: "Alterar" },
  "concordar": { texto: "Concordar com a alteração" },
  "discordar": { texto: "Discordar da alteração", motivo: true },
};

/** Textos do histórico para entradas que não são ações de botão (rotina, documentos). */
export const OUTRAS_ENTRADAS: Record<string, string> = {
  "CRIAR": "Processo criado",
  "DOCUMENTOS_COMPLETOS": "Todos os documentos enviados",
  "ACEITAR_DOCUMENTO": "Documento aceito",
  "RECUSAR_DOCUMENTO": "Documento recusado",
  "REENVIAR_DOCUMENTO": "Documento reenviado",
  "CONFIRMAR_PAGAMENTO": "Pagamento confirmado pela tesouraria",
  "RECUSAR_PAGAMENTO": "Pagamento recusado pela tesouraria",
  "TENTAR_PAGAMENTO": "Tentativa de pagamento",
};

export function textoDaEntrada(acao: string) {
  const comoCaminho = acao.toLowerCase().replace(/_/g, "-") as Acao;
  return ACOES[comoCaminho]?.texto ?? OUTRAS_ENTRADAS[acao] ?? acao;
}

// ---------- tipos de processo e seus bens ----------

export type Tipo = "IMOBILIARIO" | "AUTO" | "ELETRO" | "SERVICO";

export interface CampoDoBem { chave: string; rotulo: string; exemplo: string }

export const TIPOS: Record<Tipo, {
  texto: string; bem: string; bens: string; explicacao: string; vistoria: boolean;
  campos: CampoDoBem[]; documentosDoBem: { codigo: string; texto: string }[];
}> = {
  IMOBILIARIO: {
    texto: "Imobiliário", bem: "imóvel", bens: "imóveis", vistoria: true,
    explicacao: "Compra de imóveis. Depois da análise, passa por vistoria.",
    campos: [
      { chave: "endereco", rotulo: "Endereço", exemplo: "Rua das Flores, 120, apto 31, São Paulo/SP" },
      { chave: "matricula", rotulo: "Matrícula", exemplo: "45.871" },
      { chave: "cartorio", rotulo: "Cartório de registro", exemplo: "3º Registro de Imóveis de São Paulo" },
    ],
    documentosDoBem: [{ codigo: "MATRICULA", texto: "Matrícula atualizada" }, { codigo: "IPTU", texto: "Carnê do IPTU" }],
  },
  AUTO: {
    texto: "Auto", bem: "veículo", bens: "veículos", vistoria: false,
    explicacao: "Compra de carros, motos e utilitários.",
    campos: [
      { chave: "modelo", rotulo: "Marca e modelo", exemplo: "Toyota Corolla XEi" },
      { chave: "ano", rotulo: "Ano", exemplo: "2022" },
      { chave: "placa", rotulo: "Placa", exemplo: "ABC1D23" },
      { chave: "renavam", rotulo: "RENAVAM", exemplo: "01234567890" },
    ],
    documentosDoBem: [{ codigo: "CRLV", texto: "CRLV do veículo" }, { codigo: "NOTA_OU_RECIBO", texto: "Nota fiscal ou recibo de compra" }],
  },
  ELETRO: {
    texto: "Eletro", bem: "eletrodoméstico", bens: "eletrodomésticos", vistoria: false,
    explicacao: "Compra de eletrodomésticos e eletrônicos.",
    campos: [
      { chave: "descricao", rotulo: "Descrição", exemplo: "Geladeira frost free 480 L" },
      { chave: "marca", rotulo: "Marca", exemplo: "Brastemp" },
      { chave: "modelo", rotulo: "Modelo", exemplo: "BRM59" },
    ],
    documentosDoBem: [{ codigo: "NOTA_FISCAL", texto: "Nota fiscal" }],
  },
  SERVICO: {
    texto: "Serviço", bem: "serviço", bens: "serviços", vistoria: false,
    explicacao: "Contratação de serviços (reforma, cirurgia, festa, viagem...).",
    campos: [
      { chave: "descricao", rotulo: "Descrição do serviço", exemplo: "Reforma do banheiro" },
      { chave: "prestador", rotulo: "Prestador", exemplo: "Reformas Silva Ltda." },
    ],
    documentosDoBem: [{ codigo: "ORCAMENTO", texto: "Orçamento ou contrato do serviço" }],
  },
};

/** O caminho principal de cada tipo. `talvez`: só acima da alçada. */
export function caminhoDoTipo(tipo: Tipo): { situacao: Situacao; talvez?: boolean }[] {
  return [
    { situacao: "AGUARDANDO_DOCUMENTOS" }, { situacao: "ABERTO" }, { situacao: "EM_ANALISE" },
    ...(TIPOS[tipo].vistoria ? [{ situacao: "AGUARDANDO_VISTORIA" as Situacao }] : []),
    { situacao: "AGUARDANDO_ALCADA", talvez: true }, { situacao: "APROVADO" }, { situacao: "PAGAMENTO_SOLICITADO" }, { situacao: "PAGO" },
  ];
}

// ---------- partes e documentos ----------

export const PAPEIS_DA_PARTE = ["comprador", "vendedor", "cônjuge", "procurador", "cartório", "imobiliária", "prestador"] as const;
export type PapelDaParte = (typeof PAPEIS_DA_PARTE)[number];

/** Documentos pessoais de cada parte: pessoa física (CPF, 11 dígitos) ou jurídica (CNPJ, 14 dígitos). */
export const DOCUMENTOS_PESSOAIS = {
  fisica: [
    { codigo: "IDENTIDADE", texto: "Documento de identidade (RG ou CNH)" },
    { codigo: "COMPROVANTE_RESIDENCIA", texto: "Comprovante de residência" },
    { codigo: "CERTIDAO_ESTADO_CIVIL", texto: "Certidão de estado civil" },
  ],
  juridica: [
    { codigo: "CONTRATO_SOCIAL", texto: "Contrato social" },
    { codigo: "DOCUMENTO_REPRESENTANTE", texto: "Documento do representante legal" },
  ],
};

export const SITUACOES_DO_DOCUMENTO = {
  PENDENTE: { texto: "Pendente", tom: "neutro" },
  ENVIADO: { texto: "Enviado", tom: "andamento" },
  ACEITO: { texto: "Aceito", tom: "sucesso" },
  RECUSADO: { texto: "Recusado", tom: "erro" },
} as const satisfies Record<string, { texto: string; tom: Tom }>;
export type SituacaoDoDocumento = keyof typeof SITUACOES_DO_DOCUMENTO;

// ---------- quem usa ----------

export type Papel = "cliente" | "backoffice" | "aprovador" | "tesouraria";

/** Pessoas do protótipo. O token de cada uma leva usuário (sub), papéis (groups) e, para a cliente, o clienteId. */
export const PERSONAS = [
  { usuario: "maria", nome: "Maria Souza", papel: "cliente" as Papel, descricao: "compradora, cliente da cota" },
  { usuario: "ana", nome: "Ana", papel: "backoffice" as Papel, descricao: "backoffice" },
  { usuario: "carlos", nome: "Carlos", papel: "backoffice" as Papel, descricao: "backoffice" },
  { usuario: "bruno", nome: "Bruno", papel: "aprovador" as Papel, descricao: "aprovador (alçada)" },
  { usuario: "tesouraria", nome: "Tesouraria", papel: "tesouraria" as Papel, descricao: "pagamento" },
];
export type Persona = (typeof PERSONAS)[number];

export type Jornada = "criacao" | "documentos" | "analise";

/** Quem atua em cada jornada (o seletor de persona só mostra estas). Documentos: quem tem o link, sem persona. */
export const PERSONAS_DA_JORNADA: Record<Jornada, string[]> = {
  criacao: ["maria"],
  documentos: [],
  analise: ["ana", "carlos", "bruno", "tesouraria"],
};

/** R16: até este valor, inclusive, aprova direto; acima, precisa do aprovador. Igual a processo.alcada do serviço. */
export const ALCADA = 50_000;
