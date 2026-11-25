/**
 * O serviço de processos simulado no navegador (modo falso: `npm run dev:falso` e os testes e2e).
 * Imita o contrato do serviço: mesmas rotas, status, mensagens e formato de erro, e a mesma tabela de fluxo
 * (FluxoProcesso.comum() + FluxoImobiliario). É uma CÓPIA para a tela; a regra que vale é a do domínio.
 * Se o fluxo mudar no serviço, mude aqui também (a TabelaDosFluxosTest diz o que é o certo).
 */
import type { Acao, Situacao, Tipo, TipoDocumento } from "@/api/processos";
import { guardado } from "@/nucleo/guardado";
import { erroSimulado as erro, type RotaSimulada as RotaFalsa } from "@/nucleo/simulado";
/** R4: até este valor, inclusive, aprova direto; acima, precisa de quem tem alçada. Igual a processo.alcada do serviço. */
const ALCADA = 50_000;
const FINAIS: Situacao[] = ["PAGO", "REPROVADO", "CANCELADO"];

/** Texto da ação nas mensagens do serviço: "Não é possível [descrição] um processo...". (AcaoProcesso.descricao) */
const DESCRICAO: Record<Acao, string> = {
  INICIAR_ANALISE: "iniciar a análise de",
  SOLICITAR_DOCUMENTO: "solicitar documento para",
  RECEBER_DOCUMENTO: "receber documento de",
  CONCLUIR_ANALISE: "concluir a análise de",
  CONCLUIR_VISTORIA: "concluir a vistoria de",
  APROVAR_NA_ALCADA: "aprovar na alçada",
  DEVOLVER_PARA_ANALISE: "devolver para análise",
  REPROVAR: "reprovar",
  PAGAR: "pagar",
  CANCELAR: "cancelar",
};
const EXIGE_MOTIVO: Acao[] = ["SOLICITAR_DOCUMENTO", "DEVOLVER_PARA_ANALISE", "REPROVAR", "CANCELAR"];

type Destino = (p: Processo) => Situacao;
type Tabela = Partial<Record<Situacao, Partial<Record<Acao, Destino>>>>;
const fixo = (s: Situacao): Destino => () => s;
const pelaAlcada = (acima: Situacao, ate: Situacao): Destino => (p) => (p.valor > ALCADA ? acima : ate);

const COMUM: Tabela = {
  ABERTO: { INICIAR_ANALISE: fixo("EM_ANALISE") },
  EM_ANALISE: {
    SOLICITAR_DOCUMENTO: fixo("PENDENTE_DOCUMENTO"),
    REPROVAR: fixo("REPROVADO"),
    CONCLUIR_ANALISE: pelaAlcada("AGUARDANDO_ALCADA", "APROVADO"),
  },
  PENDENTE_DOCUMENTO: { RECEBER_DOCUMENTO: fixo("EM_ANALISE") },
  AGUARDANDO_ALCADA: {
    APROVAR_NA_ALCADA: fixo("APROVADO"),
    REPROVAR: fixo("REPROVADO"),
    DEVOLVER_PARA_ANALISE: fixo("EM_ANALISE"),
  },
  APROVADO: { DEVOLVER_PARA_ANALISE: fixo("EM_ANALISE"), PAGAR: fixo("PAGO") },
};

function comCancelar(t: Tabela): Tabela {
  // R7: todo estado não final que o tipo usa pode ser cancelado.
  return Object.fromEntries(Object.entries(t).map(([s, saidas]) =>
    [s, FINAIS.includes(s as Situacao) ? saidas : { ...saidas, CANCELAR: fixo("CANCELADO") }]));
}

const FLUXOS: Record<Tipo, Tabela> = {
  SERVICO: comCancelar(COMUM),
  IMOBILIARIO: comCancelar({
    ...COMUM,
    EM_ANALISE: { ...COMUM.EM_ANALISE, CONCLUIR_ANALISE: fixo("AGUARDANDO_VISTORIA") },
    AGUARDANDO_VISTORIA: {
      CONCLUIR_VISTORIA: pelaAlcada("AGUARDANDO_ALCADA", "APROVADO"),
      DEVOLVER_PARA_ANALISE: fixo("EM_ANALISE"),
    },
  }),
};

/** O processo como o serviço guarda: os códigos já com o tipo do enum (no contrato, alguns chegam como string). */
interface Processo { id: number; tipo: Tipo; numeroCota: string; valor: number; estado: Situacao }
interface Transicao { de: Situacao; para: Situacao; acao: Acao; quando: string; autor: string; observacao: string | null }

/** PROPOSTA: documento anexado (contrato/proposto.yaml). Guarda só os dados do arquivo, não o conteúdo. */
interface Documento { id: number; tipo: TipoDocumento; nome: string; tamanho: number; quando: string; autor: string }
const TIPOS_DE_DOCUMENTO: TipoDocumento[] = ["IDENTIDADE", "COMPROVANTE_RESIDENCIA", "MATRICULA_DO_IMOVEL", "OUTRO"];
const FORMATOS_ACEITOS = [".pdf", ".jpg", ".jpeg", ".png"];
const LIMITE_DO_ARQUIVO = 10 * 1024 * 1024;

// Os dados simulados ficam no navegador, para sobreviver a uma recarga. "Limpar" = apagar a chave no localStorage.
const banco = guardado<{ processos: (Processo & { historico: Transicao[]; documentos?: Documento[] })[] }>("simulado:processos", { processos: [] });

function achar(id: string) {
  return banco.ler().processos.find((p) => String(p.id) === id);
}

const naoEncontrado = (id: string) => erro(404, `Processo ${id} não encontrado(a)`);

export const rotasDoServicoFalso: RotaFalsa[] = [
  {
    metodo: "POST", caminho: "/processos",
    tratar({ corpo }) {
      const c = (corpo ?? {}) as Partial<{ tipo: Tipo; numeroCota: string; valor: number }>;
      const campos: { campo: string; erro: string }[] = [];
      if (!c.numeroCota?.trim()) campos.push({ campo: "numeroCota", erro: "não deve estar em branco" });
      else if (c.numeroCota.length > 30) campos.push({ campo: "numeroCota", erro: "tamanho deve ser entre 0 e 30" });
      if (!c.tipo) campos.push({ campo: "tipo", erro: "não deve ser nulo" });
      else if (!(c.tipo in FLUXOS)) campos.push({ campo: "tipo", erro: "valor não aceito" });
      if (c.valor == null) campos.push({ campo: "valor", erro: "não deve ser nulo" });
      else if (!(c.valor > 0)) campos.push({ campo: "valor", erro: "deve ser maior que 0" });
      if (campos.length) return erro(400, "Dados inválidos", campos.sort((a, b) => a.campo.localeCompare(b.campo)));

      const { processos } = banco.ler();
      const id = processos.reduce((m, p) => Math.max(m, p.id), 0) + 1;
      banco.gravar({ processos: [...processos, { id, tipo: c.tipo!, numeroCota: c.numeroCota!.trim(), valor: c.valor!, estado: "ABERTO", historico: [] }] });
      return { status: 201, resposta: { id } };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id",
    tratar({ parametros: { id = "" } }) {
      const p = achar(id);
      if (!p) return naoEncontrado(id);
      const { historico: _, documentos: __, ...resumo } = p;
      return { status: 200, resposta: resumo };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/acoes",
    tratar({ parametros: { id = "" } }) {
      const p = achar(id);
      if (!p) return naoEncontrado(id);
      return { status: 200, resposta: Object.keys(FLUXOS[p.tipo][p.estado] ?? {}).sort() };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/historico",
    tratar({ parametros: { id = "" } }) {
      const p = achar(id);
      return p ? { status: 200, resposta: p.historico } : naoEncontrado(id);
    },
  },
  {
    metodo: "POST", caminho: "/processos/:id/acoes/:acao",
    tratar({ parametros: { id = "", acao = "" }, corpo, cabecalhos }) {
      const usuario = cabecalhos["x-usuario"]?.trim();
      if (!usuario) return erro(400, "Dados inválidos", [{ campo: "usuario", erro: "não deve estar em branco" }]);
      if (!(acao in DESCRICAO)) return erro(404, "Recurso não encontrado");
      const p = achar(id);
      if (!p) return naoEncontrado(id);
      const a = acao as Acao;
      const destino = FLUXOS[p.tipo][p.estado]?.[a];
      if (!destino) return erro(409, `Não é possível ${DESCRICAO[a]} um processo no estado ${p.estado}`);
      const motivo = ((corpo ?? {}) as { motivo?: string }).motivo ?? null;
      if (EXIGE_MOTIVO.includes(a) && !motivo?.trim()) return erro(400, `Motivo é obrigatório para ${DESCRICAO[a]} um processo`);

      const para = destino(p);
      const transicao: Transicao = { de: p.estado, para, acao: a, quando: new Date().toISOString(), autor: usuario, observacao: motivo };
      banco.gravar({
        processos: banco.ler().processos.map((x) => (x.id === p.id ? { ...x, estado: para, historico: [...x.historico, transicao] } : x)),
      });
      return { status: 200, resposta: { estado: para } };
    },
  },

  // ---------------- PROPOSTA (contrato/proposto.yaml): o serviço de verdade ainda não tem ----------------
  {
    metodo: "GET", caminho: "/processos",
    tratar({ busca }) {
      const situacao = busca.get("situacao");
      const pagina = Number(busca.get("pagina") ?? 0);
      const tamanho = Number(busca.get("tamanho") ?? 10);
      if (!Number.isInteger(pagina) || pagina < 0) return erro(400, "Dados inválidos", [{ campo: "pagina", erro: "deve ser maior que ou igual a 0" }]);
      if (!Number.isInteger(tamanho) || tamanho < 1 || tamanho > 50) return erro(400, "Dados inválidos", [{ campo: "tamanho", erro: "deve estar entre 1 e 50" }]);
      const todos = banco.ler().processos
        .filter((p) => !situacao || p.estado === situacao)
        .sort((a, b) => b.id - a.id)
        .map(({ historico: _, documentos: __, ...resumo }) => resumo);
      return { status: 200, resposta: { itens: todos.slice(pagina * tamanho, (pagina + 1) * tamanho), pagina, tamanho, total: todos.length } };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/documentos",
    tratar({ parametros: { id = "" } }) {
      const p = achar(id);
      return p ? { status: 200, resposta: (p.documentos ?? []).slice().reverse() } : naoEncontrado(id);
    },
  },
  {
    metodo: "POST", caminho: "/processos/:id/documentos",
    tratar({ parametros: { id = "" }, corpo, cabecalhos }) {
      const usuario = cabecalhos["x-usuario"]?.trim();
      if (!usuario) return erro(400, "Dados inválidos", [{ campo: "usuario", erro: "não deve estar em branco" }]);
      const p = achar(id);
      if (!p) return naoEncontrado(id);
      const { tipo, arquivo } = (corpo ?? {}) as { tipo?: string; arquivo?: unknown };
      const campos: { campo: string; erro: string }[] = [];
      if (!tipo || !TIPOS_DE_DOCUMENTO.includes(tipo as TipoDocumento)) campos.push({ campo: "tipo", erro: "valor não aceito" });
      if (!(arquivo instanceof File) || arquivo.size === 0) campos.push({ campo: "arquivo", erro: "envie um arquivo" });
      else if (!FORMATOS_ACEITOS.some((f) => arquivo.name.toLowerCase().endsWith(f))) campos.push({ campo: "arquivo", erro: "formato não aceito: envie PDF, JPG ou PNG" });
      if (campos.length) return erro(400, "Dados inválidos", campos);
      const f = arquivo as File;
      if (f.size > LIMITE_DO_ARQUIVO) return erro(413, "O arquivo passa do limite de 10 MB");
      const documentos = p.documentos ?? [];
      const novo: Documento = { id: documentos.length + 1, tipo: tipo as TipoDocumento, nome: f.name, tamanho: f.size, quando: new Date().toISOString(), autor: usuario };
      banco.gravar({ processos: banco.ler().processos.map((x) => (x.id === p.id ? { ...x, documentos: [...documentos, novo] } : x)) });
      return { status: 201, resposta: novo };
    },
  },
];
