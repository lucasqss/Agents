import type { ReactNode } from "react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/componentes/ui/table";
import { cn } from "cn";

export interface Coluna<T> {
  titulo: string;
  valor: (linha: T) => ReactNode;
  /** Números alinhados à direita, com algarismos de mesma largura. */
  numero?: boolean;
}

/** Tabela simples; com `aoEscolher`, a linha inteira vira um link (clique ou Enter). */
export function Tabela<T>({ colunas, linhas, chave, aoEscolher }: {
  colunas: Coluna<T>[]; linhas: T[]; chave: (l: T) => string | number; aoEscolher?: (l: T) => void;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>{colunas.map((c) => <TableHead key={c.titulo} className={cn(c.numero && "text-right")}>{c.titulo}</TableHead>)}</TableRow>
      </TableHeader>
      <TableBody>
        {linhas.map((l) => (
          <TableRow key={chave(l)} className={cn(aoEscolher && "cursor-pointer")} tabIndex={aoEscolher ? 0 : undefined}
            onClick={aoEscolher && (() => aoEscolher(l))} onKeyDown={aoEscolher && ((e) => e.key === "Enter" && aoEscolher(l))}>
            {colunas.map((c) => <TableCell key={c.titulo} className={cn(c.numero && "text-right tabular-nums")}>{c.valor(l)}</TableCell>)}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
