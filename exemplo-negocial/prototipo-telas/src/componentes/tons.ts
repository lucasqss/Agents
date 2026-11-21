/** Os tons de estado usados por etiquetas e avisos. Mudar a cor de um estado é mudar só aqui. */
export type Tom = "neutro" | "andamento" | "espera" | "sucesso" | "erro";

export const CORES_DO_TOM: Record<Tom, string> = {
  neutro: "bg-neutral-800 text-neutral-300 ring-neutral-700",
  andamento: "bg-blue-500/10 text-blue-300 ring-blue-500/30",
  espera: "bg-amber-500/10 text-amber-300 ring-amber-500/30",
  sucesso: "bg-emerald-500/10 text-emerald-300 ring-emerald-500/30",
  erro: "bg-red-500/10 text-red-300 ring-red-500/30",
};
