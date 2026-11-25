import { describe, expect, it } from "vitest";
import { lerReais, reais, tamanhoDeArquivo } from "./formato";

describe("formato", () => {
  it.each([
    ["80.000,00", 80000], ["80000,00", 80000], ["80000.5", 80000.5], ["R$ 1.234,56", 1234.56], ["7", 7],
  ])("lerReais(%s) = %s", (texto, valor) => expect(lerReais(texto)).toBe(valor));

  it.each(["oitenta mil", "1,2,3", "", "80.00,00"])("lerReais(%s) não é número", (texto) => expect(lerReais(texto)).toBeNaN());

  it("reais formata em pt-BR", () => expect(reais(80000).replace(/\s/g, " ")).toBe("R$ 80.000,00"));

  it.each([[512, "512 B"], [2048, "2 KB"], [3_500_000, "3,3 MB"]])("tamanhoDeArquivo(%s) = %s", (bytes, texto) =>
    expect(tamanhoDeArquivo(bytes)).toBe(texto));
});
