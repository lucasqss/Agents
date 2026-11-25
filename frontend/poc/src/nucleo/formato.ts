export function reais(valor: number) {
  return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

/** "50.000,01", "50000,01" ou "50000.01" -> 50000.01; texto inválido -> NaN. */
export function lerReais(texto: string) {
  const limpo = String(texto).replace(/[R$\s]/g, "");
  if (!/^\d{1,3}(\.?\d{3})*(,\d{1,2})?$|^\d+(\.\d{1,2})?$/.test(limpo)) return NaN;
  return limpo.includes(",") ? Number(limpo.replace(/\./g, "").replace(",", ".")) : Number(limpo);
}

export function dataHora(iso: string) {
  return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

/** 2048 -> "2 KB"; 3_500_000 -> "3,3 MB". */
export function tamanhoDeArquivo(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} MB`;
}
