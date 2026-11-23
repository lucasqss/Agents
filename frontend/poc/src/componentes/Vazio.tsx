import type { ReactNode } from "react";

/** Estado vazio ou carregando: <Vazio>Nenhum processo ainda.</Vazio>. */
export function Vazio({ children }: { children: ReactNode }) {
  return <p className="py-6 text-center text-sm text-muted-foreground">{children}</p>;
}
