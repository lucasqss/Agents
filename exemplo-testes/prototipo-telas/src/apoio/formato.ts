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
