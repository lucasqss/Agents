import { Skeleton } from "@/componentes/ui/skeleton";

/**
 * Enquanto a tela busca os dados: blocos cinza no lugar do conteúdo. `linhas` aproxima a altura do que vai chegar.
 * O texto fica para o leitor de tela (sem role="status": esse papel é do aviso de resultado).
 */
export function Carregando({ linhas = 3, texto = "Carregando..." }: { linhas?: number; texto?: string }) {
  return (
    <div aria-busy="true" className="space-y-3 py-2">
      <span className="sr-only">{texto}</span>
      {Array.from({ length: linhas }, (_, i) => <Skeleton key={i} className={i === 0 ? "h-5 w-2/5" : "h-4 w-full"} />)}
    </div>
  );
}
