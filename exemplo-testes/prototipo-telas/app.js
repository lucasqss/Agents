// Protótipo de telas do processo de pagamento. JavaScript puro, sem build: abra, altere, recarregue.
// O vocabulário é o mesmo das especificações de aceitação (src/test/java/cucumber/passos/Vocabulario.java).
"use strict";

const API = window.PROTOTIPO.api;

// ---------- vocabulário: termo do negócio <-> código da API ----------

const SITUACOES = {
  ABERTO: "Aberto",
  EM_ANALISE: "Em análise",
  PENDENTE_DOCUMENTO: "Pendente de documento",
  AGUARDANDO_VISTORIA: "Aguardando vistoria",
  AGUARDANDO_ALCADA: "Aguardando alçada",
  APROVADO: "Aprovado",
  PAGO: "Pago",
  REPROVADO: "Reprovado",
  CANCELADO: "Cancelado",
};

// Cor da etiqueta de cada situação (classes de .etiqueta no estilo.css).
const COR_DA_SITUACAO = {
  EM_ANALISE: "andamento",
  APROVADO: "andamento",
  PENDENTE_DOCUMENTO: "espera",
  AGUARDANDO_VISTORIA: "espera",
  AGUARDANDO_ALCADA: "espera",
  PAGO: "sucesso",
  REPROVADO: "erro",
  CANCELADO: "erro",
};

// Ordem dos botões e texto de cada ação; "motivo" diz se a ação exige motivo (regra R5); "perigo" muda o estilo.
const ACOES = {
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

const TIPOS = {
  SERVICO: { texto: "Serviço", explicacao: "Análise e, acima da alçada, aprovação." },
  IMOBILIARIO: { texto: "Imobiliário", explicacao: "Análise, vistoria do imóvel e, acima da alçada, aprovação." },
};

// O caminho principal de cada tipo, para o negócio ver onde o processo está e o que falta.
// "talvez": só acontece acima da alçada.
const CAMINHOS = {
  SERVICO: ["ABERTO", "EM_ANALISE", { estado: "AGUARDANDO_ALCADA", talvez: true }, "APROVADO", "PAGO"],
  IMOBILIARIO: ["ABERTO", "EM_ANALISE", "AGUARDANDO_VISTORIA", { estado: "AGUARDANDO_ALCADA", talvez: true }, "APROVADO", "PAGO"],
};

const PERSONAS = [
  { usuario: "ana", nome: "Ana", papel: "analista" },
  { usuario: "vitor", nome: "Vitor", papel: "vistoriador" },
  { usuario: "carla", nome: "Carla", papel: "aprovadora (alçada)" },
  { usuario: "tesouraria", nome: "Tesouraria", papel: "pagamento" },
];

// ---------- apoio ----------

const tela = document.getElementById("tela");

function h(texto) {
  return String(texto ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function reais(valor) {
  return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

/** "50.000,01" ou "50000,01" ou "50000.01" -> 50000.01; texto inválido -> NaN. */
function lerReais(texto) {
  const limpo = String(texto).replace(/[R$\s]/g, "");
  if (!/^\d{1,3}(\.?\d{3})*(,\d{1,2})?$|^\d+(\.\d{1,2})?$/.test(limpo)) return NaN;
  return limpo.includes(",") ? Number(limpo.replace(/\./g, "").replace(",", ".")) : Number(limpo);
}

function dataHora(iso) {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function etiqueta(estado) {
  return `<span class="etiqueta ${COR_DA_SITUACAO[estado] ?? ""}">${h(SITUACOES[estado] ?? estado)}</span>`;
}

function persona() {
  return PERSONAS.find((p) => p.usuario === document.getElementById("persona").value) ?? PERSONAS[0];
}

function guardar(chave, valor) {
  try { localStorage.setItem(chave, JSON.stringify(valor)); } catch { /* navegador sem armazenamento: segue sem */ }
}

function ler(chave, padrao) {
  try { return JSON.parse(localStorage.getItem(chave)) ?? padrao; } catch { return padrao; }
}

/** Processos que este navegador abriu ou consultou: a API não tem listagem. */
function lembrar(id) {
  const recentes = ler("recentes", []).filter((r) => r !== id);
  guardar("recentes", [id, ...recentes].slice(0, 20));
}

/** Título, trilha de navegação e item ativo da barra lateral. */
function pagina(titulo, trilha, menu) {
  document.getElementById("titulo").textContent = titulo;
  document.title = `${titulo} · Processos de pagamento`;
  document.getElementById("trilha").innerHTML = trilha
    .map(([texto, link]) => (link ? `<a href="${link}">${h(texto)}</a>` : `<span>${h(texto)}</span>`))
    .join('<span class="sep">›</span>');
  document.querySelectorAll(".menu a").forEach((a) => a.classList.toggle("ativo", a.dataset.menu === menu));
}

// ---------- chamadas à API (e o "por trás da tela") ----------

async function chamar(metodo, caminho, corpo) {
  const pedido = { method: metodo, headers: { "X-Usuario": persona().usuario } };
  if (corpo !== undefined) {
    pedido.headers["Content-Type"] = "application/json";
    pedido.body = JSON.stringify(corpo);
  }
  let status = 0;
  let resposta = null;
  try {
    const r = await fetch(API + caminho, pedido);
    status = r.status;
    const texto = await r.text();
    resposta = texto ? JSON.parse(texto) : null;
  } catch (e) {
    resposta = { mensagem: `Sem resposta da API em ${API}. O ./mvnw quarkus:dev está rodando? (${e.message})` };
  }
  document.getElementById("ultima-chamada").textContent =
    `${metodo} ${API}${caminho}\nX-Usuario: ${persona().usuario}` +
    (corpo !== undefined ? `\n\n${JSON.stringify(corpo, null, 2)}` : "") +
    `\n\n→ ${status || "sem resposta"}\n${JSON.stringify(resposta, null, 2)}`;
  return { ok: status >= 200 && status < 300, status, corpo: resposta };
}

// ---------- telas ----------

async function telaInicio() {
  pagina("Processos", [["Processos"]], "inicio");
  const recentes = ler("recentes", []);
  tela.innerHTML = `
    <section class="cartao">
      <div class="cartao-topo">
        <h2>Recentes</h2>
        <div class="linha">
          <form id="consultar" class="linha">
            <input id="numero" inputmode="numeric" placeholder="Número do processo" style="max-width: 190px" aria-label="Número do processo">
            <button class="botao">Consultar</button>
          </form>
          <a class="botao principal" href="#/abrir">+ Abrir processo</a>
        </div>
      </div>
      <div id="lista">${recentes.length ? '<p class="vazio">Carregando...</p>'
        : '<p class="vazio">Nenhum processo ainda. Abra o primeiro, ou consulte um pelo número.</p>'}</div>
      <p class="apagado" style="margin: 12px 0 0; font-size: .85rem">Lista deste navegador: os processos que você abriu ou consultou aqui.</p>
    </section>`;

  document.getElementById("consultar").addEventListener("submit", (e) => {
    e.preventDefault();
    const numero = document.getElementById("numero").value.trim();
    if (numero) location.hash = `#/processo/${encodeURIComponent(numero)}`;
  });

  if (!recentes.length) return;
  const linhas = await Promise.all(recentes.map(async (id) => {
    const r = await chamar("GET", `/processos/${id}`);
    if (!r.ok) return `<tr><td class="numero">${h(id)}</td><td colspan="4" class="apagado">não encontrado</td></tr>`;
    const p = r.corpo;
    return `<tr data-id="${p.id}">
      <td class="numero">${p.id}</td>
      <td>${h(p.numeroCota)}</td>
      <td>${h(TIPOS[p.tipo]?.texto ?? p.tipo)}</td>
      <td class="numero">${reais(p.valor)}</td>
      <td>${etiqueta(p.estado)}</td>
    </tr>`;
  }));
  document.getElementById("lista").innerHTML = `
    <table class="tabela">
      <thead><tr><th>Número</th><th>Cota</th><th>Tipo</th><th>Valor</th><th>Situação</th></tr></thead>
      <tbody>${linhas.join("")}</tbody>
    </table>`;
  tela.querySelectorAll("tr[data-id]").forEach((tr) => tr.addEventListener("click", () => {
    location.hash = `#/processo/${tr.dataset.id}`;
  }));
}

function telaAbrir() {
  pagina("Abrir processo", [["Processos", "#/"], ["Abrir processo"]], "abrir");
  tela.innerHTML = `
    <section class="cartao">
      <form id="abrir" novalidate>
        <div class="campo">
          <span class="rotulo">Tipo do processo</span>
          <div class="opcoes">
            ${Object.entries(TIPOS).map(([codigo, t], i) => `
              <label class="opcao"><input type="radio" name="tipo" value="${codigo}" ${i === 0 ? "checked" : ""}>
                <strong>${h(t.texto)}</strong><small>${h(t.explicacao)}</small></label>`).join("")}
          </div>
          <div class="erro-campo" data-campo="tipo"></div>
        </div>
        <label class="campo"><span class="rotulo">Número da cota</span>
          <input name="numeroCota" maxlength="30" placeholder="ex.: COTA-123" autocomplete="off">
          <div class="erro-campo" data-campo="numeroCota"></div>
        </label>
        <label class="campo"><span class="rotulo">Valor a pagar (R$)</span>
          <input name="valor" inputmode="decimal" placeholder="ex.: 80.000,00">
          <div class="erro-campo" data-campo="valor"></div>
        </label>
        <p class="secundario" style="margin: 0 0 16px">Até R$ 50.000,00 o processo é aprovado na análise (ou na vistoria); acima, precisa de quem tem alçada.</p>
        <div id="mensagem"></div>
        <div class="linha">
          <button class="botao principal" id="confirmar">Abrir processo</button>
          <a class="botao" href="#/">Cancelar</a>
        </div>
      </form>
    </section>`;

  const form = document.getElementById("abrir");
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    form.querySelectorAll(".erro-campo").forEach((el) => { el.textContent = ""; });
    document.getElementById("mensagem").innerHTML = "";
    const dados = new FormData(form);
    const valor = lerReais(dados.get("valor"));
    if (Number.isNaN(valor)) {
      form.querySelector('[data-campo="valor"]').textContent = "Use o formato 80.000,00";
      return;
    }
    const botao = document.getElementById("confirmar");
    botao.disabled = true;
    const r = await chamar("POST", "/processos", {
      tipo: dados.get("tipo"), numeroCota: String(dados.get("numeroCota")).trim(), valor,
    });
    botao.disabled = false;
    if (r.ok) {
      lembrar(r.corpo.id);
      location.hash = `#/processo/${r.corpo.id}?aberto=1`;
      return;
    }
    for (const c of r.corpo?.campos ?? []) {
      const alvo = form.querySelector(`[data-campo="${c.campo}"]`);
      if (alvo) alvo.textContent = c.erro;
    }
    document.getElementById("mensagem").innerHTML = `<div class="aviso erro">${h(r.corpo?.mensagem ?? "Não foi possível abrir")}</div>`;
  });
}

async function telaProcesso(id, aviso) {
  pagina(`Processo ${id}`, [["Processos", "#/"], [`Processo ${id}`]], "inicio");
  tela.innerHTML = `<section class="cartao"><p class="vazio">Carregando o processo ${h(id)}...</p></section>`;
  const [processo, acoes, historico] = await Promise.all([
    chamar("GET", `/processos/${encodeURIComponent(id)}`),
    chamar("GET", `/processos/${encodeURIComponent(id)}/acoes`),
    chamar("GET", `/processos/${encodeURIComponent(id)}/historico`),
  ]);
  if (!processo.ok) {
    tela.innerHTML = `<div class="aviso erro">${h(processo.corpo?.mensagem ?? "Processo não encontrado")}</div>
      <a class="botao" href="#/">Voltar aos processos</a>`;
    return;
  }
  const p = processo.corpo;
  lembrar(p.id);

  const disponiveis = Object.keys(ACOES).filter((a) => (acoes.corpo ?? []).includes(a));
  const transicoes = historico.corpo ?? [];
  tela.innerHTML = `
    ${aviso ? `<div class="aviso ${aviso.tipo}">${h(aviso.texto)}</div>` : ""}
    <section class="indicadores" style="margin-bottom: var(--espaco)">
      <div class="indicador"><small>Situação</small><strong>${etiqueta(p.estado)}</strong></div>
      <div class="indicador"><small>Cota</small><strong>${h(p.numeroCota)}</strong></div>
      <div class="indicador"><small>Tipo</small><strong>${h(TIPOS[p.tipo]?.texto ?? p.tipo)}</strong></div>
      <div class="indicador"><small>Valor</small><strong class="numero">${reais(p.valor)}</strong></div>
    </section>

    <section class="cartao">
      <h2>Caminho do processo ${h((TIPOS[p.tipo]?.texto ?? "").toLowerCase())}</h2>
      ${caminho(p.tipo, p.estado, transicoes)}
    </section>

    <section class="cartao">
      <div class="cartao-topo">
        <h2>Ações disponíveis</h2>
        <span class="apagado">como ${h(persona().nome)} · ${h(persona().papel)}</span>
      </div>
      ${disponiveis.length
        ? `<div class="linha">${disponiveis.map((a) =>
            `<button data-acao="${a}" class="botao ${ACOES[a].perigo ? "perigo" : ""}">${h(ACOES[a].texto)}</button>`).join("")}</div>
           <p class="apagado" style="margin: 12px 0 0; font-size: .85rem">Nesta versão todos veem as ações da situação; o perfil de cada pessoa ainda vai ser definido.</p>`
        : `<p class="secundario" style="margin: 0">Nenhuma ação: o processo está numa situação final.</p>`}
    </section>

    <section class="cartao">
      <h2>Histórico</h2>
      ${transicoes.length
        ? `<ol class="linha-do-tempo">${transicoes.slice().reverse().map((t) => `
            <li>
              <div><span class="titulo">${h(ACOES[t.acao]?.texto ?? t.acao)}</span>
                <span class="secundario">por ${h(t.autor)}</span>
                <span class="quando">· ${dataHora(t.quando)}</span></div>
              <div class="transicao">${etiqueta(t.de)} → ${etiqueta(t.para)}</div>
              ${t.observacao ? `<div class="motivo">${h(t.observacao)}</div>` : ""}
            </li>`).join("")}</ol>`
        : `<p class="secundario" style="margin: 0">Nenhuma ação ainda.</p>`}
    </section>`;

  tela.querySelectorAll("button[data-acao]").forEach((b) => b.addEventListener("click", () => executar(p.id, b.dataset.acao)));
}

/** O caminho do tipo como passos numerados: os já percorridos com ✓, o atual em destaque, os futuros apagados. */
function caminho(tipo, atual, historico) {
  const percorridos = new Set(historico.flatMap((t) => [t.de, t.para]));
  const passos = (CAMINHOS[tipo] ?? []).map((p) => (typeof p === "string" ? { estado: p } : p));
  const noCaminho = passos.some((p) => p.estado === atual);
  const html = passos.map((p, i) => {
    const classe = p.estado === atual ? "atual" : percorridos.has(p.estado) ? "feito" : p.talvez ? "talvez" : "";
    const marca = classe === "feito" ? "✓" : i + 1;
    const nome = SITUACOES[p.estado] + (classe === "talvez" ? " (acima da alçada)" : "");
    return `<div class="passo ${classe}"><span class="bola">${marca}</span><span class="nome">${h(nome)}</span></div>`;
  }).join("");
  const fora = noCaminho ? ""
    : `<p class="fora-do-caminho secundario">Situação atual fora do caminho principal: ${etiqueta(atual)}</p>`;
  return `<div class="caminho">${html}</div>${fora}`;
}

async function executar(id, acao) {
  let corpo;
  if (ACOES[acao].motivo) {
    const motivo = await pedirMotivo(ACOES[acao].texto);
    if (motivo === null) return;
    corpo = { motivo };
  }
  const r = await chamar("POST", `/processos/${id}/acoes/${acao}`, corpo);
  const aviso = r.ok
    ? { tipo: "sucesso", texto: `${ACOES[acao].texto}: feito. O processo agora está ${SITUACOES[r.corpo.estado] ?? r.corpo.estado}.` }
    : { tipo: "erro", texto: `${ACOES[acao].texto}: não foi possível. ${r.corpo?.mensagem ?? ""}` };
  telaProcesso(id, aviso);
}

function pedirMotivo(titulo) {
  const dialogo = document.getElementById("dialogo-motivo");
  const texto = document.getElementById("dialogo-texto");
  document.getElementById("dialogo-titulo").textContent = titulo;
  texto.value = "";
  dialogo.showModal();
  texto.focus();
  return new Promise((resolver) => {
    dialogo.addEventListener("close", () => {
      resolver(dialogo.returnValue === "confirmar" ? texto.value : null);
    }, { once: true });
  });
}

// ---------- roteador por #hash ----------

function rotear() {
  const [caminhoAtual, busca] = location.hash.replace(/^#/, "").split("?");
  const partes = (caminhoAtual || "/").split("/").filter(Boolean);
  if (partes[0] === "abrir") return telaAbrir();
  if (partes[0] === "processo" && partes[1]) {
    const aberto = new URLSearchParams(busca).has("aberto");
    return telaProcesso(decodeURIComponent(partes[1]), aberto ? { tipo: "sucesso", texto: "Processo aberto." } : undefined);
  }
  return telaInicio();
}

function mostrarPersona() {
  const p = persona();
  document.getElementById("avatar").textContent = p.nome.slice(0, 2).toUpperCase();
}

function iniciar() {
  const seletor = document.getElementById("persona");
  seletor.innerHTML = PERSONAS.map((p) => `<option value="${p.usuario}">${h(p.nome)} · ${h(p.papel)}</option>`).join("");
  seletor.value = ler("persona", "ana");
  mostrarPersona();
  seletor.addEventListener("change", () => { guardar("persona", seletor.value); mostrarPersona(); rotear(); });
  window.addEventListener("hashchange", rotear);
  rotear();
}

iniciar();
