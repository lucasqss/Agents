import { FileUp } from "lucide-react";
import { useRef, useState } from "react";

/**
 * Escolher um arquivo (clicando ou arrastando), conferindo tipo e tamanho antes de enviar.
 * `aoEscolher` recebe o arquivo válido; o envio em si é de quem usa.
 */
export function EnvioDeArquivo({ rotulo, aceitos = [".pdf", ".jpg", ".jpeg", ".png"], tamanhoMaximoMb = 10, desabilitado, aoEscolher }: {
  rotulo: string; aceitos?: string[]; tamanhoMaximoMb?: number; desabilitado?: boolean; aoEscolher: (arquivo: File) => void;
}) {
  const entrada = useRef<HTMLInputElement>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [arrastando, setArrastando] = useState(false);

  function escolher(arquivo: File | undefined) {
    if (!arquivo) return;
    const extensao = "." + (arquivo.name.split(".").pop() ?? "").toLowerCase();
    if (!aceitos.includes(extensao)) return setErro(`Envie um arquivo ${aceitos.join(", ")}`);
    if (arquivo.size > tamanhoMaximoMb * 1024 * 1024) return setErro(`O arquivo passa de ${tamanhoMaximoMb} MB`);
    setErro(null);
    aoEscolher(arquivo);
  }

  return (
    <div>
      <button type="button" disabled={desabilitado} onClick={() => entrada.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setArrastando(true); }} onDragLeave={() => setArrastando(false)}
        onDrop={(e) => { e.preventDefault(); setArrastando(false); escolher(e.dataTransfer.files[0]); }}
        className={`flex w-full items-center justify-center gap-2 rounded-lg border border-dashed px-3 py-2.5 text-sm transition-colors disabled:opacity-50 ${
          arrastando ? "border-blue-500 bg-blue-500/10 text-blue-200" : "border-neutral-700 text-neutral-300 hover:border-neutral-500"}`}>
        <FileUp className="size-4" aria-hidden />
        {rotulo}
      </button>
      <input ref={entrada} type="file" accept={aceitos.join(",")} className="sr-only" aria-label={rotulo} tabIndex={-1}
        onChange={(e) => { escolher(e.target.files?.[0]); e.target.value = ""; }} />
      {erro && <p className="mt-1.5 text-xs text-red-400">{erro}</p>}
    </div>
  );
}
