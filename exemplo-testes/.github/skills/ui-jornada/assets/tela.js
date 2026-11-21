// Modelo: tela em JavaScript puro (copie para telas/<nome>.js e registre a rota na inicialização).
// Regras: busca pela api/, traduz códigos pelo contexto da jornada, monta com componentes; nenhum if de regra de negócio;
// botão de ação só se a API o lista; erro é o da API.
import * as recurso from "../api/RECURSO.js";
import { h } from "../nucleo/formato.js";
import { pagina } from "../componentes/pagina.js";
import { aviso } from "../componentes/aviso.js";
import { etiqueta } from "../componentes/etiqueta.js";
import { jornadaAtual, situacao, acao, ordenarAcoes, texto } from "./contexto.js";

export async function telaNOME({ id }, _consulta, raiz) {
  pagina({ titulo: `Título ${id}`, trilha: [["Início", "#/"], [`Título ${id}`]], menu: "inicio", sistema: jornadaAtual().nome });
  raiz.innerHTML = '<section class="cartao"><p class="vazio">Carregando...</p></section>';

  const [registro, disponiveis] = await Promise.all([recurso.consultar(id), recurso.acoes(id)]);
  if (!registro.ok) {
    raiz.innerHTML = aviso({ tipo: "erro", texto: registro.corpo?.mensagem ?? "Não encontrado" });
    return;
  }
  const r = registro.corpo;
  raiz.innerHTML = `
    <section class="cartao">
      <div class="cartao-topo"><h2>${h(r.nome)}</h2>${etiqueta(situacao(r.estado))}</div>
      <p class="secundario">${h(texto("ajudaDestaTela"))}</p>
      <div class="linha">${ordenarAcoes(disponiveis.corpo ?? []).map((a) =>
        `<button class="botao ${acao(a).estilo === "perigo" ? "perigo" : ""}" data-acao="${h(a)}">${h(acao(a).rotulo)}</button>`).join("")}</div>
    </section>`;
}
