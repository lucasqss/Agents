import type { ReactNode } from "react";

export function Vazio({ children }: { children: ReactNode }) {
  return <p className="py-6 text-center text-sm text-neutral-500">{children}</p>;
}
