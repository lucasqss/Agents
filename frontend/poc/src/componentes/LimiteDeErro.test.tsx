import { render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { LimiteDeErro } from "./LimiteDeErro";

function Quebrada(): never {
  throw new Error("defeito de teste");
}

it("tela que quebra ao desenhar vira aviso com o caminho de volta", () => {
  vi.spyOn(console, "error").mockImplementation(() => undefined); // o React registra o erro capturado
  render(<LimiteDeErro><Quebrada /></LimiteDeErro>);
  expect(screen.getByRole("alert").textContent).toContain("Algo deu errado ao mostrar esta tela. (defeito de teste)");
  expect(screen.getByRole("link", { name: "Voltar ao início" }).getAttribute("href")).toBe("#/");
});
