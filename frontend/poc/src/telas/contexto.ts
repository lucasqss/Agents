// O que o navegador lembra entre telas: quem sou eu (a persona) e os processos que abri ou consultei (a API não tem
// listagem). Fica nas telas porque é estado de apresentação, não desenho da jornada nem regra.
import { guardado } from "@/nucleo/guardado";
import { PERSONAS } from "@/jornadas/processo-pagamento";

export const persona = guardado<string>("persona", PERSONAS[0].usuario);

/** A persona escolhida em "Quem sou eu" (e a tela se redesenha quando ela muda). */
export function useEu() {
  const usuario = persona.useValor();
  return PERSONAS.find((p) => p.usuario === usuario) ?? PERSONAS[0];
}

export const recentes = guardado<number[]>("recentes", []);

export function lembrar(id: number) {
  recentes.gravar([id, ...recentes.ler().filter((r) => r !== id)].slice(0, 20));
}
