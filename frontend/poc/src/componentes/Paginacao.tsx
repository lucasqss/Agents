import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/componentes/ui/button";

/** Anterior / próxima, com "Página X de Y". `pagina` começa em 0 (como na API). */
export function Paginacao({ pagina, total, tamanho, aoMudar }: { pagina: number; total: number; tamanho: number; aoMudar: (pagina: number) => void }) {
  const paginas = Math.max(1, Math.ceil(total / tamanho));
  return (
    <nav aria-label="Paginação" className="mt-4 flex flex-wrap items-center justify-between gap-2">
      <span className="text-xs text-muted-foreground">Página {pagina + 1} de {paginas} · {total} no total</span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
          <ChevronLeft aria-hidden />Anterior
        </Button>
        <Button variant="outline" size="sm" disabled={pagina >= paginas - 1} onClick={() => aoMudar(pagina + 1)}>
          Próxima<ChevronRight aria-hidden />
        </Button>
      </div>
    </nav>
  );
}
