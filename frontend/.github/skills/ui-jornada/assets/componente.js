// Modelo: componente visual puro (copie para componentes/<nome>.js).
// Regras: recebe dados prontos por parâmetro, devolve a visão; não importa api/, jornadas/ nem telas/;
// todo texto passa por h(); o CSS fica num bloco "/* <nome> */" em estilo/componentes.css, só com tokens.
import { h } from "../nucleo/formato.js";

/**
 * Cartão de resumo com título e linhas rótulo/valor.
 * @param {{titulo: string, linhas: {rotulo: string, valor: string}[]}} p  valor já em HTML seguro (montado pela tela)
 */
export function resumo({ titulo, linhas }) {
  return `<section class="cartao">
    <h2>${h(titulo)}</h2>
    <dl class="resumo">${linhas.map((l) => `<dt>${h(l.rotulo)}</dt><dd>${l.valor}</dd>`).join("")}</dl>
  </section>`;
}
