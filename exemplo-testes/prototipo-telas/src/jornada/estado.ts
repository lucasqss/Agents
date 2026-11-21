import { guardado } from "../apoio/guardado";
import { PERSONAS } from "./vocabulario";

/** Quem sou eu: a persona escolhida na barra lateral. */
export const persona = guardado<string>("persona", PERSONAS[0].usuario);

export function personaAtual() {
  return PERSONAS.find((p) => p.usuario === persona.ler()) ?? PERSONAS[0];
}

/** Processos que este navegador abriu ou consultou: a API não tem listagem. */
export const recentes = guardado<number[]>("recentes", []);

export function lembrar(id: number) {
  recentes.gravar([id, ...recentes.ler().filter((r) => r !== id)].slice(0, 20));
}
