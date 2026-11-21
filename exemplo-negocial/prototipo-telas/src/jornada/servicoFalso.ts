/**
 * O serviço simulado no navegador (modo falso: `npm run dev:falso` e os testes e2e).
 *
 * Duas partes:
 * 1. O que JÁ EXISTE no serviço: imita rotas, status, mensagens, a tabela do fluxo (FluxoProcesso.comum() e
 *    FluxoImobiliario), os papéis de cada linha, R6, R13, R15, R17 e a ordem das recusas da ADR 003
 *    (401, 403 de papel, 400 de formato, 404, 409 de situação, 403 da linha, 400 de domínio). É uma CÓPIA: a regra que
 *    vale é a do domínio; se divergir, o serviço está certo.
 * 2. O que é PROPOSTA (fase 2), marcado assim: bens, partes, tipos auto e eletro, documentos por parte, e-mails, a
 *    situação Aguardando documentos. Vira o caso de uso e os endpoints novos depois da validação das telas.
 */
import { erro, type RespostaFalsa, type RotaFalsa } from "../api/apiFalsa";
import { guardado } from "../apoio/guardado";
import type { Bem, DocumentoExigido, Email, Favorecido, Parte, Transicao } from "./api";
import { lerToken } from "./token";
import {
  ALCADA, DOCUMENTOS_PESSOAIS, SITUACOES, TIPOS, type Acao, type Papel, type Situacao, type Tipo,
} from "./vocabulario";

// ---------- dados ----------

interface ParteGravada extends Parte { link: string }
interface ProcessoGravado {
  id: number; cotaId: number; tipo: Tipo; valor: number; estado: Situacao; protocolo: string | null;
  bens: Bem[]; partes: ParteGravada[]; favorecidos: Favorecido[]; historico: Transicao[];
  autorUltimaConclusao: string | null;
}
interface DocumentoGravado extends DocumentoExigido { processoId: number }
interface Banco {
  clientes: { id: number; nome: string; cpf: string }[];
  cotas: { id: number; numero: string; valorCredito: number; clienteId: number }[];
  processos: ProcessoGravado[];
  documentos: DocumentoGravado[];
  emails: Email[];
}

const banco = guardado<Banco>("simulado:negocial", { clientes: [], cotas: [], processos: [], documentos: [], emails: [] });
/** Os arquivos ficam só na memória: somem ao recarregar a página (o localStorage não comporta arquivos). */
const arquivos = new Map<number, Blob>();

function mudar(f: (b: Banco) => void) {
  const copia = structuredClone(banco.ler());
  f(copia);
  banco.gravar(copia);
}
const proximoId = (lista: { id: number }[]) => lista.reduce((m, x) => Math.max(m, x.id), 0) + 1;
const agora = () => new Date().toISOString();
const dinheiro = (v: number) => v.toFixed(2);

// ---------- identidade (ADR 003) ----------

interface Autor { usuario: string; papeis: Papel[]; clienteId: number | null }

function autorDe(cabecalhos: Record<string, string>): Autor | null {
  const t = lerToken((cabecalhos.Authorization ?? "").replace(/^Bearer /, ""));
  if (!t?.sub) return null;
  return { usuario: t.sub, papeis: (t.groups ?? []) as Papel[], clienteId: t.clienteId ?? null };
}

const NAO_IDENTIFICADO = erro(401, "Identificação ausente ou inválida");
const SEM_PERMISSAO = erro(403, "Sem permissão para esta operação");

/** Autenticado e com um dos papéis (o @RolesAllowed do endpoint); sem papéis, qualquer autenticado (@Authenticated). */
function exigir(cabecalhos: Record<string, string>, ...papeis: Papel[]): Autor | RespostaFalsa {
  const autor = autorDe(cabecalhos);
  if (!autor) return NAO_IDENTIFICADO;
  if (papeis.length && !autor.papeis.some((p) => papeis.includes(p))) return SEM_PERMISSAO;
  return autor;
}
const recusou = (x: Autor | RespostaFalsa): x is RespostaFalsa => "status" in x;

// ---------- tabela do fluxo (cópia de FluxoProcesso.comum() + FluxoImobiliario) ----------

interface Linha { destino: (p: ProcessoGravado) => Situacao; papeis: Papel[]; segundaPessoa?: boolean }
type Tabela = Partial<Record<Situacao, Partial<Record<Acao, Linha>>>>;

const para = (s: Situacao, ...papeis: Papel[]): Linha => ({ destino: () => s, papeis });
const pelaAlcada = (...papeis: Papel[]): Linha => ({ destino: (p) => (p.valor > ALCADA ? "AGUARDANDO_ALCADA" : "APROVADO"), papeis });

const COMUM: Tabela = {
  // PROPOSTA (fase 2): o processo nasce aguardando os documentos; sai dali sozinho, quando a última parte envia.
  AGUARDANDO_DOCUMENTOS: {},
  ABERTO: { "iniciar-analise": para("EM_ANALISE", "backoffice"), "alterar": para("ABERTO", "cliente") },
  EM_ANALISE: {
    "solicitar-documento": para("PENDENTE_DOCUMENTO", "backoffice"),
    "reprovar": para("REPROVADO", "backoffice"),
    "concluir-analise": pelaAlcada("backoffice"),
    "propor-alteracao": para("AGUARDANDO_CONCORDANCIA", "backoffice"),
  },
  PENDENTE_DOCUMENTO: { "receber-documento": para("EM_ANALISE", "backoffice") },
  AGUARDANDO_ALCADA: {
    "aprovar-na-alcada": { ...para("APROVADO", "aprovador"), segundaPessoa: true }, // R17
    "reprovar": para("REPROVADO", "aprovador"),
    "devolver-para-analise": para("EM_ANALISE", "aprovador"),
  },
  APROVADO: { "devolver-para-analise": para("EM_ANALISE", "backoffice"), "solicitar-pagamento": para("PAGAMENTO_SOLICITADO", "tesouraria") },
  AGUARDANDO_CONCORDANCIA: { "concordar": para("EM_ANALISE", "cliente"), "discordar": para("EM_ANALISE", "cliente") },
};

function comCancelar(t: Tabela): Tabela {
  // R4: toda situação não final pode ser cancelada pelo backoffice ou pelo aprovador, menos pagamento solicitado.
  const r: Tabela = {};
  for (const s of Object.keys(SITUACOES) as Situacao[]) {
    if (SITUACOES[s].final) continue;
    r[s] = s === "PAGAMENTO_SOLICITADO" ? { ...t[s] } : { ...t[s], "cancelar": para("CANCELADO", "backoffice", "aprovador") };
  }
  return r;
}

const IMOBILIARIO = comCancelar({
  ...COMUM,
  EM_ANALISE: { ...COMUM.EM_ANALISE, "concluir-analise": para("AGUARDANDO_VISTORIA", "backoffice") },
  AGUARDANDO_VISTORIA: { "concluir-vistoria": pelaAlcada("backoffice"), "devolver-para-analise": para("EM_ANALISE", "backoffice") },
});
const SEM_VISTORIA = comCancelar(COMUM);
/** PROPOSTA (fase 2): auto e eletro seguem o fluxo do serviço. */
const FLUXOS: Record<Tipo, Tabela> = { IMOBILIARIO, SERVICO: SEM_VISTORIA, AUTO: SEM_VISTORIA, ELETRO: SEM_VISTORIA };

/** Texto da ação nas mensagens: "Não é possível [descrição] um processo...". (AcaoProcesso.descricao) */
const DESCRICAO: Record<Acao, string> = {
  "iniciar-analise": "iniciar a análise de", "solicitar-documento": "solicitar documento para",
  "receber-documento": "receber documento de", "reprovar": "reprovar", "concluir-analise": "concluir a análise de",
  "concluir-vistoria": "concluir a vistoria de", "aprovar-na-alcada": "aprovar na alçada",
  "devolver-para-analise": "devolver para análise", "solicitar-pagamento": "solicitar o pagamento de",
  "cancelar": "cancelar", "alterar": "alterar", "propor-alteracao": "propor alteração em",
  "concordar": "concordar com a alteração de", "discordar": "discordar da alteração de",
};
const codigoDaAcao = (a: Acao) => a.toUpperCase().replace(/-/g, "_");

/** Papéis do @RolesAllowed de cada endpoint de ação (a primeira barreira, antes do corpo e do processo). */
const PAPEIS_DO_ENDPOINT: Record<Acao, Papel[]> = {
  "iniciar-analise": ["backoffice"], "concluir-analise": ["backoffice"], "concluir-vistoria": ["backoffice"],
  "aprovar-na-alcada": ["aprovador"], "devolver-para-analise": ["backoffice", "aprovador"],
  "solicitar-pagamento": ["tesouraria"], "cancelar": ["backoffice", "aprovador"], "solicitar-documento": ["backoffice"],
  "receber-documento": ["backoffice"], "reprovar": ["backoffice", "aprovador"], "alterar": ["cliente"],
  "propor-alteracao": ["backoffice"], "concordar": ["cliente"], "discordar": ["cliente"],
};
const EXIGE_MOTIVO: Acao[] = ["devolver-para-analise", "cancelar", "solicitar-documento", "reprovar", "discordar"];

// ---------- apoio ----------

function processo(id: string) {
  return banco.ler().processos.find((p) => String(p.id) === id);
}
const processoNaoEncontrado = (id: string) => erro(404, `Processo ${id} não encontrado(a)`);

function registrar(p: ProcessoGravado, acao: string, para: Situacao, autor: string, observacao: string | null = null) {
  p.historico.push({ de: p.estado, para, acao, quando: agora(), autor, observacao, valorAnterior: null, valorNovo: null });
  p.estado = para;
}

function resumo(p: ProcessoGravado, b: Banco) {
  return { id: p.id, cotaId: p.cotaId, cotaNumero: b.cotas.find((c) => c.id === p.cotaId)?.numero ?? "", tipo: p.tipo, valor: p.valor, estado: p.estado, protocoloPagamento: p.protocolo };
}

function cotaDetalhe(cotaId: number, b: Banco) {
  const c = b.cotas.find((x) => x.id === cotaId)!;
  const pagos = b.processos.filter((p) => p.cotaId === cotaId && p.estado === "PAGO").reduce((s, p) => s + p.valor, 0);
  const atual = b.processos.find((p) => p.cotaId === cotaId && !SITUACOES[p.estado].final);
  return {
    id: c.id, numero: c.numero, valorCredito: c.valorCredito, creditoDisponivel: c.valorCredito - pagos, clienteId: c.clienteId,
    clienteNome: b.clientes.find((x) => x.id === c.clienteId)?.nome ?? "",
    processoAtualId: atual?.id ?? null, processoAtualTipo: atual?.tipo ?? null, processoAtualEstado: atual?.estado ?? null,
  };
}

const DOCUMENTO_VALIDO = /^(\d{11}|\d{14})$/;
const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const DUAS_CASAS = (v: unknown) => typeof v === "number" && v > 0 && Math.round(v * 100) === v * 100;

type Campo = { campo: string; erro: string };
function textoObrigatorio(campos: Campo[], campo: string, valor: unknown, max?: number) {
  if (typeof valor !== "string" || !valor.trim()) campos.push({ campo, erro: "não deve estar em branco" });
  else if (max && valor.length > max) campos.push({ campo, erro: `tamanho deve ser entre 0 e ${max}` });
}
function valorPositivo(campos: Campo[], campo: string, valor: unknown) {
  if (valor == null) campos.push({ campo, erro: "não deve ser nulo" });
  else if (!DUAS_CASAS(valor)) campos.push({ campo, erro: "deve ser maior que 0, com até duas casas decimais" });
}
const dadosInvalidos = (campos: Campo[]) => erro(400, "Dados inválidos", campos.sort((a, b) => a.campo.localeCompare(b.campo)));

/** R15 e R29: favorecidos sem repetir documento e com partes que somam exatamente o valor (mensagens do domínio). */
function regraDosFavorecidos(valor: number, favorecidos: Favorecido[]): string | null {
  const vistos = new Set<string>();
  let soma = 0;
  for (const f of favorecidos) {
    if (vistos.has(f.documento)) return `Favorecido repetido: o documento ${f.documento} aparece mais de uma vez`;
    vistos.add(f.documento);
    soma += f.valor;
  }
  soma = Math.round(soma * 100) / 100;
  if (soma < valor) return `As partes dos favorecidos somam ${dinheiro(soma)} e faltam ${dinheiro(valor - soma)} para o valor do processo (${dinheiro(valor)})`;
  if (soma > valor) return `As partes dos favorecidos somam ${dinheiro(soma)} e passam ${dinheiro(soma - valor)} do valor do processo (${dinheiro(valor)})`;
  return null;
}

/** PROPOSTA (fase 2): os documentos que cada parte envia; os dos bens ficam com o comprador. */
function documentosExigidos(p: ProcessoGravado, inicio: number): DocumentoGravado[] {
  const lista: DocumentoGravado[] = [];
  const novo = (parteId: number, codigo: string, texto: string, bem: string | null) => lista.push({
    id: inicio + lista.length, processoId: p.id, parteId, codigo, texto, bem,
    situacao: "PENDENTE", arquivo: null, enviadoEm: null, motivoRecusa: null,
  });
  for (const parte of p.partes) {
    const pessoais = parte.documento.length === 14 ? DOCUMENTOS_PESSOAIS.juridica : DOCUMENTOS_PESSOAIS.fisica;
    pessoais.forEach((d) => novo(parte.id, d.codigo, d.texto, null));
    if (parte.papel === "comprador") {
      p.bens.forEach((bem, i) => {
        const nome = `${TIPOS[p.tipo].bem[0].toUpperCase()}${TIPOS[p.tipo].bem.slice(1)} ${i + 1}: ${Object.values(bem.dados)[0] ?? ""}`;
        TIPOS[p.tipo].documentosDoBem.forEach((d) => novo(parte.id, d.codigo, d.texto, nome));
      });
    }
  }
  return lista;
}

function novoEmail(b: Banco, p: ProcessoGravado, parte: ParteGravada, tipo: Email["tipo"], detalhe = "") {
  const convite = tipo === "CONVITE";
  b.emails.push({
    id: proximoId(b.emails), para: parte.email, nome: parte.nome, quando: agora(), link: parte.link, tipo, processoId: p.id,
    assunto: convite ? `Envio de documentos: processo de pagamento nº ${p.id}` : `Documento recusado: processo nº ${p.id}`,
    texto: convite
      ? `Olá, ${parte.nome}. Você foi indicado(a) como ${parte.papel} no processo de pagamento nº ${p.id}, de consórcio. Envie os seus documentos pelo link abaixo. Só você vê os seus documentos.`
      : `Olá, ${parte.nome}. Um documento que você enviou para o processo nº ${p.id} foi recusado: ${detalhe}. Envie um novo pelo link abaixo.`,
  });
}

const linkNovo = () => Array.from(crypto.getRandomValues(new Uint8Array(12)), (x) => x.toString(16).padStart(2, "0")).join("");

// ---------- rotas ----------

/** As ações que só mudam a situação: um endpoint por ação, como no ProcessoResource. */
const rotasDeAcao: RotaFalsa[] = (Object.keys(DESCRICAO) as Acao[]).map((acao) => ({
  metodo: "POST" as const,
  caminho: `/processos/:id/${acao}`,
  tratar({ parametros: { id }, corpo, cabecalhos }) {
    const autor = exigir(cabecalhos, ...PAPEIS_DO_ENDPOINT[acao]);
    if (recusou(autor)) return autor;
    const motivo = (corpo as { motivo?: string } | null)?.motivo;
    if (EXIGE_MOTIVO.includes(acao) && !motivo?.trim()) return dadosInvalidos([{ campo: "motivo", erro: "não deve estar em branco" }]);
    if (["alterar", "propor-alteracao", "concordar", "discordar"].includes(acao)) {
      return erro(409, `${DESCRICAO[acao]}: não simulado neste protótipo (não faz parte das três jornadas)`);
    }
    const p = processo(id);
    if (!p) return processoNaoEncontrado(id);
    const linha = FLUXOS[p.tipo][p.estado]?.[acao];
    if (!linha) return erro(409, `Não é possível ${DESCRICAO[acao]} um processo no estado ${p.estado}`);
    if (!autor.papeis.some((x) => linha.papeis.includes(x))) {
      return erro(403, `Para ${DESCRICAO[acao]} um processo no estado ${p.estado} é preciso ter o papel ${[...linha.papeis].sort().join(" ou ")}`);
    }
    if (linha.segundaPessoa && autor.usuario === p.autorUltimaConclusao) {
      return erro(403, `Quem concluiu a análise não pode ${DESCRICAO[acao]} o mesmo processo (R17)`);
    }
    // PROPOSTA (fase 2): a análise só se conclui com todos os documentos aceitos.
    if (acao === "concluir-analise") {
      const faltam = banco.ler().documentos.filter((d) => d.processoId === p.id && d.situacao !== "ACEITO");
      if (faltam.length) return erro(409, `Há ${faltam.length} documento(s) ainda não aceito(s): ${faltam.slice(0, 3).map((d) => d.texto).join("; ")}${faltam.length > 3 ? "..." : ""}`);
    }
    let destino: Situacao = linha.destino(p);
    mudar((b) => {
      const x = b.processos.find((y) => y.id === p.id)!;
      registrar(x, codigoDaAcao(acao), destino, autor.usuario, motivo?.trim() || null);
      if (acao === "concluir-analise") x.autorUltimaConclusao = autor.usuario;
      destino = x.estado;
    });
    return { status: 200, resposta: { estado: destino } };
  },
}));

export const rotasDoServicoFalso: RotaFalsa[] = [
  // ----- cadastro (já existe) -----
  {
    metodo: "POST", caminho: "/clientes",
    tratar({ corpo, cabecalhos }) {
      const autor = exigir(cabecalhos, "backoffice");
      if (recusou(autor)) return autor;
      const c = (corpo ?? {}) as { nome?: string; cpf?: string };
      const campos: Campo[] = [];
      textoObrigatorio(campos, "nome", c.nome, 200);
      textoObrigatorio(campos, "cpf", c.cpf, 14);
      if (campos.length) return dadosInvalidos(campos);
      let id = 0;
      mudar((b) => { id = proximoId(b.clientes); b.clientes.push({ id, nome: c.nome!.trim(), cpf: c.cpf!.trim() }); });
      return { status: 201, resposta: { id } };
    },
  },
  {
    metodo: "POST", caminho: "/cotas",
    tratar({ corpo, cabecalhos }) {
      const autor = exigir(cabecalhos, "backoffice");
      if (recusou(autor)) return autor;
      const c = (corpo ?? {}) as { numero?: string; valorCredito?: number; clienteId?: number };
      const campos: Campo[] = [];
      textoObrigatorio(campos, "numero", c.numero, 30);
      valorPositivo(campos, "valorCredito", c.valorCredito);
      if (c.clienteId == null) campos.push({ campo: "clienteId", erro: "não deve ser nulo" });
      if (campos.length) return dadosInvalidos(campos);
      if (!banco.ler().clientes.some((x) => x.id === c.clienteId)) return erro(404, `Cliente ${c.clienteId} não encontrado(a)`);
      let id = 0;
      mudar((b) => { id = proximoId(b.cotas); b.cotas.push({ id, numero: c.numero!.trim(), valorCredito: c.valorCredito!, clienteId: c.clienteId! }); });
      return { status: 201, resposta: { id } };
    },
  },
  {
    metodo: "GET", caminho: "/cotas/:id",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const b = banco.ler();
      return b.cotas.some((c) => String(c.id) === id) ? { status: 200, resposta: cotaDetalhe(Number(id), b) } : erro(404, `Cota ${id} não encontrado(a)`);
    },
  },
  {
    // PROPOSTA (fase 2): as cotas de um cliente, para ele escolher a cota do processo.
    metodo: "GET", caminho: "/clientes/:id/cotas",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      if (autor.papeis.length === 1 && autor.papeis[0] === "cliente" && String(autor.clienteId) !== id) return SEM_PERMISSAO;
      const b = banco.ler();
      return { status: 200, resposta: b.cotas.filter((c) => String(c.clienteId) === id).map((c) => cotaDetalhe(c.id, b)) };
    },
  },

  // ----- processos -----
  {
    metodo: "POST", caminho: "/processos",
    tratar({ corpo, cabecalhos }) {
      const autor = exigir(cabecalhos, "cliente");
      if (recusou(autor)) return autor;
      const c = (corpo ?? {}) as Partial<{ cotaId: number; tipo: Tipo; valor: number; bens: Bem[]; partes: Omit<Parte, "id">[]; favorecidos: Favorecido[] }>;

      // 400 de formato (Bean Validation no entrypoint)
      const campos: Campo[] = [];
      if (c.cotaId == null) campos.push({ campo: "cotaId", erro: "não deve ser nulo" });
      if (!c.tipo) campos.push({ campo: "tipo", erro: "não deve ser nulo" });
      else if (!(c.tipo in TIPOS)) campos.push({ campo: "tipo", erro: "valor inválido" });
      valorPositivo(campos, "valor", c.valor);
      if (!c.favorecidos?.length) campos.push({ campo: "favorecidos", erro: "não deve estar vazio" });
      c.favorecidos?.forEach((f, i) => {
        textoObrigatorio(campos, `favorecidos[${i}].nome`, f.nome, 120);
        if (!DOCUMENTO_VALIDO.test(f.documento ?? "")) campos.push({ campo: `favorecidos[${i}].documento`, erro: "deve ter 11 (CPF) ou 14 (CNPJ) dígitos" });
        textoObrigatorio(campos, `favorecidos[${i}].papel`, f.papel, 50);
        valorPositivo(campos, `favorecidos[${i}].valor`, f.valor);
      });
      // PROPOSTA (fase 2): bens e partes
      if (!c.bens?.length) campos.push({ campo: "bens", erro: "não deve estar vazio" });
      c.bens?.forEach((bem, i) => {
        if (c.tipo && c.tipo in TIPOS) TIPOS[c.tipo].campos.forEach((k) => textoObrigatorio(campos, `bens[${i}].dados.${k.chave}`, bem.dados?.[k.chave], 200));
        valorPositivo(campos, `bens[${i}].valor`, bem.valor);
      });
      if (!c.partes?.length) campos.push({ campo: "partes", erro: "não deve estar vazio" });
      c.partes?.forEach((p, i) => {
        textoObrigatorio(campos, `partes[${i}].nome`, p.nome, 120);
        if (!DOCUMENTO_VALIDO.test(p.documento ?? "")) campos.push({ campo: `partes[${i}].documento`, erro: "deve ter 11 (CPF) ou 14 (CNPJ) dígitos" });
        textoObrigatorio(campos, `partes[${i}].papel`, p.papel, 50);
        if (!EMAIL_VALIDO.test(p.email ?? "")) campos.push({ campo: `partes[${i}].email`, erro: "deve ser um endereço de e-mail bem formado" });
      });
      if (campos.length) return dadosInvalidos(campos);

      // 400 de domínio: R15, R29 e (PROPOSTA) bens e partes
      const valor = c.valor!;
      const somaDosBens = Math.round(c.bens!.reduce((s, x) => s + x.valor, 0) * 100) / 100;
      if (somaDosBens !== valor) return erro(400, `Os bens somam ${dinheiro(somaDosBens)} e o valor do processo é ${dinheiro(valor)}: o valor do processo é a soma dos bens`);
      const compradores = c.partes!.filter((p) => p.papel === "comprador").length;
      if (compradores !== 1) return erro(400, `O processo precisa de exatamente um comprador entre as partes (há ${compradores})`);
      const docs = c.partes!.map((p) => p.documento);
      const repetido = docs.find((d, i) => docs.indexOf(d) !== i);
      if (repetido) return erro(400, `Parte repetida: o documento ${repetido} aparece mais de uma vez`);
      const r15 = regraDosFavorecidos(valor, c.favorecidos!);
      if (r15) return erro(400, r15);

      // 404, 403 do dono (R18), 409 da cota em uso (R6) e do crédito (R13): a ordem do CriarProcessoUseCase
      const b = banco.ler();
      const cota = b.cotas.find((x) => x.id === c.cotaId);
      if (!cota) return erro(404, `Cota ${c.cotaId} não encontrado(a)`);
      if (autor.clienteId !== cota.clienteId) return erro(403, `Só o cliente da cota ${cota.numero} pode agir por ela`);
      if (b.processos.some((p) => p.cotaId === cota.id && !SITUACOES[p.estado].final)) return erro(409, `Cota ${cota.id} já possui um processo em andamento`);
      const disponivel = cotaDetalhe(cota.id, b).creditoDisponivel;
      if (valor > disponivel) return erro(409, `Valor ${dinheiro(valor)} acima do crédito disponível da cota: ${dinheiro(disponivel)}`);

      let id = 0;
      mudar((bb) => {
        id = proximoId(bb.processos);
        let parteId = bb.processos.reduce((m, p) => Math.max(m, ...p.partes.map((x) => x.id), 0), 0);
        const p: ProcessoGravado = {
          id, cotaId: cota.id, tipo: c.tipo!, valor, estado: "AGUARDANDO_DOCUMENTOS", protocolo: null, autorUltimaConclusao: null,
          bens: c.bens!.map((x) => ({ dados: { ...x.dados }, valor: x.valor })),
          partes: c.partes!.map((x) => ({ ...x, id: ++parteId, valor: x.valor ?? null, link: linkNovo() })),
          favorecidos: c.favorecidos!, historico: [],
        };
        p.historico.push({ de: "AGUARDANDO_DOCUMENTOS", para: "AGUARDANDO_DOCUMENTOS", acao: "CRIAR", quando: agora(), autor: autor.usuario, observacao: null, valorAnterior: null, valorNovo: valor });
        bb.processos.push(p);
        bb.documentos.push(...documentosExigidos(p, proximoId(bb.documentos)));
        p.partes.forEach((parte) => novoEmail(bb, p, parte, "CONVITE"));
      });
      return { status: 201, resposta: { id } };
    },
  },
  {
    metodo: "GET", caminho: "/processos",
    tratar({ busca, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const b = banco.ler();
      const estado = busca.get("estado");
      const tipo = busca.get("tipo");
      const tamanho = Math.max(1, Number(busca.get("tamanho") ?? 20));
      const pagina = Math.max(1, Number(busca.get("pagina") ?? 1));
      const todos = b.processos.filter((p) => (!estado || p.estado === estado) && (!tipo || p.tipo === tipo)).sort((x, y) => y.id - x.id);
      return { status: 200, resposta: { itens: todos.slice((pagina - 1) * tamanho, pagina * tamanho).map((p) => resumo(p, b)), pagina, tamanho, total: todos.length } };
    },
  },
  {
    // PROPOSTA (fase 2): o detalhe do processo, com bens e partes.
    metodo: "GET", caminho: "/processos/:id",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const p = processo(id);
      if (!p) return processoNaoEncontrado(id);
      return { status: 200, resposta: { ...resumo(p, banco.ler()), bens: p.bens, partes: p.partes.map(({ link: _, ...parte }) => parte) } };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/acoes",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const p = processo(id);
      if (!p) return processoNaoEncontrado(id);
      return { status: 200, resposta: { acoes: Object.keys(FLUXOS[p.tipo][p.estado] ?? {}) } };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/historico",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const p = processo(id);
      return p ? { status: 200, resposta: p.historico } : erro(404, `Processo ${id} não encontrado`);
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/favorecidos",
    tratar({ parametros: { id }, cabecalhos }) {
      const autor = exigir(cabecalhos);
      if (recusou(autor)) return autor;
      const p = processo(id);
      return p ? { status: 200, resposta: p.favorecidos } : erro(404, `Processo ${id} não encontrado`);
    },
  },
  ...rotasDeAcao,

  // ----- documentos, na análise (PROPOSTA, fase 2) -----
  {
    metodo: "GET", caminho: "/processos/:id/documentos",
    tratar({ parametros: { id }, cabecalhos }) {
      // LGPD: o cliente não vê os documentos das outras partes; só quem analisa vê todos.
      const autor = exigir(cabecalhos, "backoffice", "aprovador", "tesouraria");
      if (recusou(autor)) return autor;
      const p = processo(id);
      if (!p) return processoNaoEncontrado(id);
      return { status: 200, resposta: banco.ler().documentos.filter((d) => d.processoId === p.id).map(({ processoId: _, ...d }) => d) };
    },
  },
  {
    metodo: "GET", caminho: "/processos/:id/documentos/:doc/arquivo",
    tratar({ parametros: { id, doc }, cabecalhos }) {
      const autor = exigir(cabecalhos, "backoffice", "aprovador", "tesouraria");
      if (recusou(autor)) return autor;
      const d = banco.ler().documentos.find((x) => String(x.processoId) === id && String(x.id) === doc);
      if (!d) return erro(404, `Documento ${doc} não encontrado(a)`);
      const arquivo = arquivos.get(d.id);
      if (!arquivo) return erro(404, d.arquivo ? "Arquivo indisponível: no modo simulado os arquivos ficam só na memória e somem ao recarregar a página" : "Documento ainda não enviado");
      return { status: 200, resposta: arquivo };
    },
  },
  ...(["aceitar", "recusar"] as const).map((decisao): RotaFalsa => ({
    metodo: "POST", caminho: `/processos/:id/documentos/:doc/${decisao}`,
    tratar({ parametros: { id, doc }, corpo, cabecalhos }) {
      const autor = exigir(cabecalhos, "backoffice");
      if (recusou(autor)) return autor;
      const motivo = (corpo as { motivo?: string } | null)?.motivo?.trim();
      if (decisao === "recusar" && !motivo) return dadosInvalidos([{ campo: "motivo", erro: "não deve estar em branco" }]);
      const p = processo(id);
      if (!p) return processoNaoEncontrado(id);
      const d = banco.ler().documentos.find((x) => x.processoId === p.id && String(x.id) === doc);
      if (!d) return erro(404, `Documento ${doc} não encontrado(a)`);
      if (p.estado !== "EM_ANALISE" && p.estado !== "PENDENTE_DOCUMENTO") return erro(409, `Documentos só são conferidos com o processo em análise (está ${SITUACOES[p.estado].texto.toLowerCase()})`);
      if (decisao === "aceitar" && d.situacao !== "ENVIADO") return erro(409, `Só documento enviado pode ser aceito (este está ${d.situacao.toLowerCase()})`);
      if (decisao === "recusar" && d.situacao !== "ENVIADO" && d.situacao !== "ACEITO") return erro(409, `Só documento enviado ou aceito pode ser recusado (este está ${d.situacao.toLowerCase()})`);
      let atualizado: DocumentoGravado | undefined;
      mudar((b) => {
        const x = b.documentos.find((y) => y.id === d.id)!;
        const pr = b.processos.find((y) => y.id === p.id)!;
        const parte = pr.partes.find((y) => y.id === x.parteId)!;
        const nome = `${x.texto}${x.bem ? ` (${x.bem})` : ""}, de ${parte.nome}`;
        if (decisao === "aceitar") {
          x.situacao = "ACEITO";
          registrar(pr, "ACEITAR_DOCUMENTO", pr.estado, autor.usuario, nome);
        } else {
          x.situacao = "RECUSADO";
          x.motivoRecusa = motivo!;
          // Recusar leva a pendente de documento e avisa a parte por e-mail; ela reenvia só aquele.
          registrar(pr, "RECUSAR_DOCUMENTO", "PENDENTE_DOCUMENTO", autor.usuario, `${nome}: ${motivo}`);
          novoEmail(b, pr, parte, "REENVIO", `${x.texto} (${motivo})`);
        }
        atualizado = x;
      });
      const { processoId: _, ...resposta } = atualizado!;
      return { status: 200, resposta };
    },
  })),

  // ----- envio pela parte, pelo link (PROPOSTA, fase 2); sem login, o link basta (identificação fora do escopo) -----
  {
    metodo: "GET", caminho: "/envios/:link",
    tratar({ parametros: { link } }) {
      const b = banco.ler();
      const p = b.processos.find((x) => x.partes.some((y) => y.link === link));
      if (!p) return erro(404, "Link inválido ou expirado");
      const parte = p.partes.find((y) => y.link === link)!;
      // LGPD: só os dados e os documentos da própria parte.
      return {
        status: 200, resposta: {
          processoId: p.id, tipo: p.tipo, situacaoDoProcesso: p.estado, parte: { nome: parte.nome, papel: parte.papel },
          documentos: b.documentos.filter((d) => d.parteId === parte.id && d.processoId === p.id).map(({ processoId: _, ...d }) => d),
        },
      };
    },
  },
  {
    metodo: "POST", caminho: "/envios/:link/documentos/:doc",
    tratar({ parametros: { link, doc }, corpo }) {
      const arquivo = (corpo as { arquivo?: unknown } | null)?.arquivo;
      if (!(arquivo instanceof Blob)) return dadosInvalidos([{ campo: "arquivo", erro: "não deve ser nulo" }]);
      const nome = arquivo instanceof File ? arquivo.name : "arquivo";
      if (!/\.(pdf|jpe?g|png)$/i.test(nome)) return dadosInvalidos([{ campo: "arquivo", erro: "deve ser PDF, JPG ou PNG" }]);
      if (arquivo.size > 10 * 1024 * 1024) return dadosInvalidos([{ campo: "arquivo", erro: "deve ter até 10 MB" }]);
      const b = banco.ler();
      const p = b.processos.find((x) => x.partes.some((y) => y.link === link));
      if (!p) return erro(404, "Link inválido ou expirado");
      const parte = p.partes.find((y) => y.link === link)!;
      const d = b.documentos.find((x) => x.processoId === p.id && x.parteId === parte.id && String(x.id) === doc);
      if (!d) return erro(404, `Documento ${doc} não encontrado(a)`);
      if (SITUACOES[p.estado].final) return erro(409, "O processo está encerrado e não recebe mais documentos");
      const antesDaAnalise = p.estado === "AGUARDANDO_DOCUMENTOS";
      if (!antesDaAnalise && d.situacao !== "RECUSADO") return erro(409, "Este documento já está com a análise e só pode ser trocado se for recusado");

      let atualizado: DocumentoGravado | undefined;
      mudar((bb) => {
        const x = bb.documentos.find((y) => y.id === d.id)!;
        const pr = bb.processos.find((y) => y.id === p.id)!;
        const eraRecusado = x.situacao === "RECUSADO";
        Object.assign(x, { situacao: "ENVIADO", arquivo: nome, enviadoEm: agora(), motivoRecusa: null });
        const doProcesso = bb.documentos.filter((y) => y.processoId === pr.id);
        if (antesDaAnalise && doProcesso.every((y) => y.situacao === "ENVIADO")) {
          registrar(pr, "DOCUMENTOS_COMPLETOS", "ABERTO", "sistema");
        } else if (eraRecusado) {
          const voltaParaAnalise = pr.estado === "PENDENTE_DOCUMENTO" && !doProcesso.some((y) => y.situacao === "RECUSADO");
          registrar(pr, "REENVIAR_DOCUMENTO", voltaParaAnalise ? "EM_ANALISE" : pr.estado, parte.nome, x.texto);
        }
        atualizado = x;
      });
      arquivos.set(d.id, arquivo);
      const { processoId: _, ...resposta } = atualizado!;
      return { status: 200, resposta };
    },
  },

  // ----- só em desenvolvimento (PROPOSTA, fase 2) -----
  {
    metodo: "GET", caminho: "/dev/emails",
    tratar: () => ({ status: 200, resposta: [...banco.ler().emails].sort((a, b) => b.id - a.id) }),
  },
  {
    // A rotina de pagamento (R20), sem esperar 5 minutos: a tesouraria simulada confirma tudo.
    metodo: "POST", caminho: "/dev/rotina-de-pagamento",
    tratar() {
      let processados = 0;
      mudar((b) => b.processos.filter((p) => p.estado === "PAGAMENTO_SOLICITADO").forEach((p) => {
        p.protocolo = `TES-${p.id}-${Date.now() % 100000}`;
        registrar(p, "CONFIRMAR_PAGAMENTO", "PAGO", "sistema", `Protocolo ${p.protocolo}`);
        processados++;
      }));
      return { status: 200, resposta: { processados } };
    },
  },
];
