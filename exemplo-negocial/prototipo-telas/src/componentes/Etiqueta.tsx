import type { ReactNode } from "react";
import { CORES_DO_TOM, type Tom } from "./tons";

export function Etiqueta({ tom = "neutro", children }: { tom?: Tom; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${CORES_DO_TOM[tom]}`}>
      {children}
    </span>
  );
}
