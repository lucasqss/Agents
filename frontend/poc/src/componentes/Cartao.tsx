import { useId, type ReactNode } from "react";
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/componentes/ui/card";

/**
 * Um bloco da tela, com título e, opcionalmente, ações à direita do título. Com título, é uma região nomeada
 * (<section aria-labelledby>): o leitor de tela e os testes o acham pelo título.
 */
export function Cartao({ titulo, acoes, children }: { titulo?: string; acoes?: ReactNode; children: ReactNode }) {
  const id = useId();
  const cartao = (
    <Card>
      {(titulo || acoes) && (
        <CardHeader>
          {titulo && <CardTitle><h2 id={id}>{titulo}</h2></CardTitle>}
          {acoes && <CardAction className="flex flex-wrap items-center gap-2">{acoes}</CardAction>}
        </CardHeader>
      )}
      <CardContent>{children}</CardContent>
    </Card>
  );
  return titulo ? <section aria-labelledby={id}>{cartao}</section> : cartao;
}
