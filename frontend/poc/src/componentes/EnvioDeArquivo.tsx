import { FileUp } from "lucide-react";
import { useId, useRef, useState } from "react";
import { cn } from "cn";

/**
 * Escolher um arquivo (clicando ou arrastando), conferindo formato e tamanho antes de enviar (o backend confere de
 * novo: aqui é só para a pessoa não esperar o envio para saber). `aoEscolher` recebe o arquivo aceito.
 */
export function EnvioDeArquivo({ rotulo, ajuda, aceitos, tamanhoMaximoMb, arquivo, aoEscolher }: {
  rotulo: string; ajuda?: string; aceitos: string[]; tamanhoMaximoMb: number; arquivo: File | null; aoEscolher: (arquivo: File) => void;
}) {
  const id = useId();
  const entrada = useRef<HTMLInputElement>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [arrastando, setArrastando] = useState(false);

  function escolher(f: File | undefined) {
    if (!f) return;
    const extensao = "." + (f.name.split(".").pop() ?? "").toLowerCase();
    if (!aceitos.includes(extensao)) return setErro(`Formato não aceito: envie ${aceitos.join(", ")}`);
    if (f.size > tamanhoMaximoMb * 1024 * 1024) return setErro(`O arquivo passa de ${tamanhoMaximoMb} MB`);
    setErro(null);
    aoEscolher(f);
  }

  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-sm font-medium">{rotulo}</label>
      <div onDragOver={(e) => { e.preventDefault(); setArrastando(true); }} onDragLeave={() => setArrastando(false)}
        onDrop={(e) => { e.preventDefault(); setArrastando(false); escolher(e.dataTransfer.files[0]); }}
        className={cn("flex flex-col items-center gap-2 rounded-lg border border-dashed px-4 py-6 text-center text-sm",
          arrastando ? "border-andamento bg-andamento/10" : "border-border")}>
        <FileUp className="size-6 text-muted-foreground" aria-hidden />
        <span>{arquivo ? <strong>{arquivo.name}</strong> : "Arraste o arquivo para cá, ou"}</span>
        <button type="button" onClick={() => entrada.current?.click()} className="text-sm font-medium text-andamento underline-offset-4 hover:underline">
          {arquivo ? "escolher outro arquivo" : "escolher um arquivo"}
        </button>
        <input ref={entrada} id={id} type="file" accept={aceitos.join(",")} className="sr-only" aria-invalid={erro ? true : undefined}
          aria-describedby={`${id}-mensagem`} onChange={(e) => { escolher(e.target.files?.[0]); e.target.value = ""; }} />
      </div>
      <p id={`${id}-mensagem`} className={cn("text-xs", erro ? "text-destructive" : "text-muted-foreground")}>{erro ?? ajuda}</p>
    </div>
  );
}
