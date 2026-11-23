import type { ComponentProps } from "react";
import { cn } from "cn";

/** Um <select> nativo com o visual do tema (acessível e testável como select de verdade). */
export function SeletorNativo({ className, ...resto }: ComponentProps<"select">) {
  return (
    <select {...resto} className={cn("w-full truncate rounded-md border bg-transparent px-2 py-1.5 text-sm focus-visible:ring-2 focus-visible:ring-ring/50 focus-visible:outline-none", className)} />
  );
}
