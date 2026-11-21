import { guardado } from "../apoio/guardado";
import { PERSONAS, PERSONAS_DA_JORNADA, type Jornada, type Tipo } from "./vocabulario";

/** Quem sou eu: a persona escolhida na barra lateral. */
export const persona = guardado<string>("persona", "maria");

export function personaAtual() {
  return PERSONAS.find((p) => p.usuario === persona.ler()) ?? PERSONAS[0];
}

/** Ao entrar numa jornada, a persona passa a ser uma das que atuam nela. */
export function garantirPersonaDa(jornada: Jornada) {
  const permitidas = PERSONAS_DA_JORNADA[jornada];
  if (permitidas.length && !permitidas.includes(persona.ler())) persona.gravar(permitidas[0]);
}

/** O cadastro de demonstração: a cliente Maria e as cotas dela, criados pelo botão "Preparar demonstração". */
export const demonstracao = guardado<{ clienteId: number; cotas: number[] } | null>("demonstracao", null);

// ---------- rascunho da criação (o assistente guarda a cada passo) ----------

export interface BemNoRascunho { chave: string; dados: Record<string, string>; valor: string }
export interface ParteNoRascunho {
  chave: string; nome: string; documento: string; papel: string; email: string; recebe: boolean; valor: string;
}
export interface Rascunho {
  cotaId: number | null;
  tipo: Tipo | null;
  bens: BemNoRascunho[];
  partes: ParteNoRascunho[];
}

export const RASCUNHO_VAZIO: Rascunho = { cotaId: null, tipo: null, bens: [], partes: [] };
export const rascunho = guardado<Rascunho>("rascunho", RASCUNHO_VAZIO);

let sequencia = Date.now();
export const novaChave = () => String(sequencia++);
