import { Label } from "@/componentes/ui/label";
import { RadioGroup, RadioGroupItem } from "@/componentes/ui/radio-group";
import { cn } from "cn";

/** Escolha única em cartões, com título e explicação de cada opção. */
export function Opcoes<T extends string>({ rotulo, valor, opcoes, aoMudar }: {
  rotulo: string; valor: T; opcoes: { valor: T; titulo: string; explicacao?: string }[]; aoMudar: (v: T) => void;
}) {
  return (
    <RadioGroup aria-label={rotulo} value={valor} onValueChange={(v) => aoMudar(v as T)} className="grid gap-3 sm:grid-cols-2">
      {opcoes.map((o) => (
        <Label key={o.valor} className={cn("cursor-pointer flex-col items-start gap-1 rounded-lg border p-3.5 font-normal",
          valor === o.valor ? "border-primary bg-primary/10" : "hover:border-muted-foreground/40")}>
          <span className="flex items-center gap-2 font-medium"><RadioGroupItem value={o.valor} />{o.titulo}</span>
          {o.explicacao && <span className="text-xs text-muted-foreground">{o.explicacao}</span>}
        </Label>
      ))}
    </RadioGroup>
  );
}
