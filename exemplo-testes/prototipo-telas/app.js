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

// Ordem dos botões e texto de cada ação; "motivo" diz se a ação exige motivo (regra R5).
const ACOES = {
  INICIAR_ANALISE: { texto: "Iniciar a análise" },
  SOLICITAR_DOCUMENTO: { texto: "Solicitar documento", motivo: true },
  RECEBER_DOCUMENTO: { texto: "Receber o documento" },
  CONCLUIR_ANALISE: { texto: "Concluir a análise" },
  CONCLUIR_VISTORIA: { texto: "Concluir a vistoria" },
  APROVAR_NA_ALCADA: { texto: "Aprovar na alçada" },
  DEVOLVER_PARA_ANALISE: { texto: "Devolver para análise", motivo: true },
  REPROVAR: { texto: "Reprovar", motivo: true },
  PAGAR: { texto: "Pagar" },
  CANCELAR: { texto: "Cancelar", motivo: true },
};

const TIPOS = {
  SERVICO: { texto: "Serviço", explicacao: "análise e, acima da alçada, aprovação" },
  IMOBILIARIO: { texto: "Imobiliário", explicacao: "análise, vistoria do imóvel e, acima da alçada, aprovação" },
};

// O caminho principal de cada tipo, para o negócio ver onde o processo está e o que falta.
// "talvez": só acontece acima da alçada.
const CAMINHOS = {
  SERVICO: ["ABERTO", "EM_ANALISE", { estado: "AGUARDANDO_ALCADA", talvez: true }, "APROVADO", "PAGO"],
  IMOBILIARIO: ["ABERTO", "EM_ANALISE", "AGUARDANDO_VISTORIA", { estado: "AGUARDANDO_ALCADA", talvez: true }, "APROVADO", "PAGO"],
};

const PERSONAS = [
  { usuario: "ana", texto: "Ana · analista" },
  { usuario: "vitor", texto: "Vitor · vistoriador" },
  { usuario: "carla", texto: "Carla · aprovadora (alçada)" },
  { usuario: "tesouraria", texto: "Tesouraria · pagamento" },
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
  const classe = estado === "PAGO" ? "final"
    : ["REPROVADO", "CANCELADO"].includes(estado) ? "fim-ruim"
    : ["AGUARDANDO_ALCADA", "AGUARDANDO_VISTORIA", "PENDENTE_DOCUMENTO"].includes(estado) ? "espera" : "";
  return `<span class="etiqueta ${classe}">${h(SITUACOES[estado] ?? estado)}</span>`;
}

function persona() {
  return document.getElementById("persona").value;
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

// ---------- chamadas à API (e o "por trás da tela") ----------

async function chamar(metodo, caminho, corpo) {
  const pedido = { method: metodo, headers: { "X-Usuario": persona() } };
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
    `${metodo} ${API}${caminho}\nX-Usuario: ${persona()}` +
    (corpo !== undefined ? `\n\n${JSON.stringify(corpo, null, 2)}` : "") +
    `\n\n→ ${status || "sem resposta"}\n${JSON.stringify(resposta, null, 2)}`;
  return { ok: status >= 200 && status < 300, status, corpo: resposta };
}

// ---------- telas ----------

async function telaInicio() {
  const recentes = ler("recentes", []);
  tela.innerHTML = `
    <section class="cartao">
      <h1>Processos de pagamento</h1>
      <p class="dica">Abra um processo e acompanhe a jornada dele: análise, vistoria (imobiliário), alçada e pagamento.
        Troque "Quem sou eu" no topo para agir como cada pessoa.</p>
      <div class="linha-acoes">
        <a class="botao principal" href="#/abrir">Abrir processo</a>
        <form id="consultar" class="linha-acoes">
          <input id="numero" inputmode="numeric" placeholder="Número do processo" style="max-width: 200px">
          <button>Consultar</button>
        </form>
      </div>
    </section>
    <section class="cartao">
      <h2>Recentes</h2>
      <p class="dica">Lista deste navegador (os processos que você abriu ou consultou aqui).</p>
      <div id="lista">${recentes.length ? "Carregando..." : "Nenhum processo ainda. Que tal abrir o primeiro?"}</div>
    </section>`;

  document.getElementById("consultar").addEventListener("submit", (e) => {
    e.preventDefault();
    const numero = document.getElementById("numero").value.trim();
    if (numero) location.hash = `#/processo/${encodeURIComponent(numero)}`;
  });

  if (!recentes.length) return;
  const linhas = await Promise.all(recentes.map(async (id) => {
    const r = await chamar("GET", `/processos/${id}`);
    if (!r.ok) return `<tr><td>${h(id)}</td><td colspan="4" class="dica">não encontrado</td></tr>`;
    const p = r.corpo;
    return `<tr>
      <td><a href="#/processo/${p.id}">${p.id}</a></td>
      <td>${h(p.numeroCota)}</td>
      <td>${h(TIPOS[p.tipo]?.texto ?? p.tipo)}</td>
      <td>${reais(p.valor)}</td>
      <td>${etiqueta(p.estado)}</td>
    </tr>`;
  }));
  document.getElementById("lista").innerHTML = `
    <table>
      <thead><tr><th>Número</th><th>Cota</th><th>Tipo</th><th>Valor</th><th>Situação</th></tr></thead>
      <tbody>${linhas.join("")}</tbody>
    </table>`;
}

function telaAbrir() {
  tela.innerHTML = `
    <section class="cartao">
      <h1>Abrir processo de pagamento</h1>
      <form id="abrir" novalidate>
        <div class="campo">
          <span class="dica">Tipo do processo</span>
          <div class="opcoes">
            ${Object.entries(TIPOS).map(([codigo, t], i) => `
              <label><input type="radio" name="tipo" value="${codigo}" ${i === 0 ? "checked" : ""}>
                <strong>${h(t.texto)}</strong><small>${h(t.explicacao)}</small></label>`).join("")}
          </div>
          <div class="erro-campo" data-campo="tipo"></div>
        </div>
        <label class="campo"><span>Número da cota</span>
          <input name="numeroCota" maxlength="30" placeholder="ex.: COTA-123" autocomplete="off">
          <div class="erro-campo" data-campo="numeroCota"></div>
        </label>
        <label class="campo"><span>Valor a pagar (R$)</span>
          <input name="valor" inputmode="decimal" placeholder="ex.: 80.000,00">
          <div class="erro-campo" data-campo="valor"></div>
        </label>
        <p class="dica">Até R$ 50.000,00 o processo é aprovado na análise (ou na vistoria); acima, precisa de quem tem alçada.</p>
        <div id="mensagem"></div>
        <div class="linha-acoes">
          <button class="principal" id="confirmar">Abrir processo</button>
          <a href="#/">Voltar</a>
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
  tela.innerHTML = `<section class="cartao">Carregando o processo ${h(id)}...</section>`;
  const [processo, acoes, historico] = await Promise.all([
    chamar("GET", `/processos/${encodeURIComponent(id)}`),
    chamar("GET", `/processos/${encodeURIComponent(id)}/acoes`),
    chamar("GET", `/processos/${encodeURIComponent(id)}/historico`),
  ]);
  if (!processo.ok) {
    tela.innerHTML = `<section class="cartao"><div class="aviso erro">${h(processo.corpo?.mensagem ?? "Processo não encontrado")}</div>
      <a href="#/">Voltar ao início</a></section>`;
    return;
  }
  const p = processo.corpo;
  lembrar(p.id);

  const disponiveis = Object.keys(ACOES).filter((a) => (acoes.corpo ?? []).includes(a));
  tela.innerHTML = `
    <section class="cartao">
      <p><a href="#/">← Início</a></p>
      <h1>Processo ${p.id}</h1>
      ${aviso ? `<div class="aviso ${aviso.tipo}">${h(aviso.texto)}</div>` : ""}
      <div class="cabecalho-processo">
        <div><small>Cota</small><strong>${h(p.numeroCota)}</strong></div>
        <div><small>Tipo</small><strong>${h(TIPOS[p.tipo]?.texto ?? p.tipo)}</strong></div>
        <div><small>Valor</small><strong>${reais(p.valor)}</strong></div>
        <div><small>Situação</small><strong>${etiqueta(p.estado)}</strong></div>
      </div>
    </section>

    <section class="cartao">
      <h2>Caminho do processo ${h((TIPOS[p.tipo]?.texto ?? "").toLowerCase())}</h2>
      ${caminho(p.tipo, p.estado, historico.corpo ?? [])}
    </section>

    <section class="cartao">
      <h2>O que ${h(PERSONAS.find((x) => x.usuario === persona())?.texto ?? persona())} pode fazer agora</h2>
      ${disponiveis.length
        ? `<div class="linha-acoes">${disponiveis.map((a) => `<button data-acao="${a}" class="${a === "CANCELAR" || a === "REPROVAR" ? "secundario" : ""}">${h(ACOES[a].texto)}</button>`).join("")}</div>
           <p class="dica">Nesta versão qualquer pessoa vê todos os botões da situação; o perfil de cada um ainda vai ser definido.</p>`
        : `<p class="dica">Nenhuma ação: o processo está numa situação final.</p>`}
    </section>

    <section class="cartao">
      <h2>Histórico</h2>
      ${(historico.corpo ?? []).length
        ? `<ol class="linha-do-tempo">${historico.corpo.slice().reverse().map((t) => `
            <li><strong>${h(ACOES[t.acao]?.texto ?? t.acao)}</strong> por ${h(t.autor)}
              <span class="quando">· ${dataHora(t.quando)}</span><br>
              ${etiqueta(t.de)} → ${etiqueta(t.para)}
              ${t.observacao ? `<div class="motivo">"${h(t.observacao)}"</div>` : ""}
            </li>`).join("")}</ol>`
        : `<p class="dica">Nenhuma ação ainda.</p>`}
    </section>`;

  tela.querySelectorAll("button[data-acao]").forEach((b) => b.addEventListener("click", () => executar(p.id, b.dataset.acao)));
}

/** A fila de passos do tipo; o atual em destaque, os já percorridos em traço cheio. */
function caminho(tipo, atual, historico) {
  const percorridos = new Set(historico.flatMap((t) => [t.de, t.para]));
  const passos = (CAMINHOS[tipo] ?? []).map((p) => (typeof p === "string" ? { estado: p } : p));
  const noCaminho = passos.some((p) => p.estado === atual);
  const fila = passos.map((p) => {
    const classe = p.estado === atual ? "atual" : percorridos.has(p.estado) ? "feito" : p.talvez ? "talvez" : "";
    const texto = SITUACOES[p.estado] + (p.talvez && p.estado !== atual ? " (acima da alçada)" : "");
    return `<span class="passo ${classe}">${h(texto)}</span>`;
  }).join('<span class="seta">→</span>');
  const fora = noCaminho ? "" : `<p>Agora: ${etiqueta(atual)} <span class="dica">(fora do caminho principal)</span></p>`;
  return `<div class="caminho">${fila}</div>${fora}`;
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
    ? { tipo: "ok", texto: `${ACOES[acao].texto}: feito. O processo agora está ${SITUACOES[r.corpo.estado] ?? r.corpo.estado}.` }
    : { tipo: "erro", texto: `${ACOES[acao].texto}: não foi possível. ${r.corpo?.mensagem ?? ""}` };
  telaProcesso(id, aviso);
}

function pedirMotivo(titulo) {
  const dialogo = document.getElementById("dialogo-motivo");
  const texto = document.getElementById("dialogo-texto");
  document.getElementById("dialogo-titulo").textContent = `${titulo}: motivo`;
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
    return telaProcesso(decodeURIComponent(partes[1]), aberto ? { tipo: "ok", texto: "Processo aberto." } : undefined);
  }
  return telaInicio();
}

function iniciar() {
  const seletor = document.getElementById("persona");
  seletor.innerHTML = PERSONAS.map((p) => `<option value="${p.usuario}">${h(p.texto)}</option>`).join("");
  seletor.value = ler("persona", "ana");
  seletor.addEventListener("change", () => { guardar("persona", seletor.value); rotear(); });
  window.addEventListener("hashchange", rotear);
  rotear();
}

iniciar();
