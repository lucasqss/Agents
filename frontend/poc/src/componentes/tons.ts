/** Os tons de estado (etiquetas, avisos). Mudar a cor de um estado é mudar só aqui; as cores vêm do tema (index.css). */
export type Tom = "neutro" | "andamento" | "espera" | "sucesso" | "erro";

export const CORES_DO_TOM: Record<Tom, string> = {
  neutro: "bg-muted text-muted-foreground border-border",
  andamento: "bg-primary/15 text-primary border-primary/30",
  espera: "bg-espera/15 text-espera border-espera/30",
  sucesso: "bg-sucesso/15 text-sucesso border-sucesso/30",
  erro: "bg-destructive/15 text-destructive border-destructive/30",
};
