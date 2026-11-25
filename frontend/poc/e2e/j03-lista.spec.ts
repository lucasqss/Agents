// PROPOSTA: lista de todos os processos, com filtro por situação e páginas de 5.
import { agir, expect, semProblemasDeAcessibilidade, test } from "./apoio";
import { abrirProcesso } from "./passos-processo";

test.describe("Todos os processos", { tag: "@proposta" }, () => {
  test("[L-01] Filtra por situação e pagina de 5 em 5", async ({ page }) => {
    for (let i = 1; i <= 6; i++) await abrirProcesso(page, "Serviço", `COTA-L${i}`, "1.000,00");
    await agir(page, "Iniciar a análise"); // o último aberto (COTA-L6) vai para Em análise

    await page.getByRole("link", { name: "Todos os processos" }).click();
    await expect(page.getByRole("heading", { name: "Todos os processos", level: 1 })).toBeVisible();
    await expect(page.getByText("Página 1 de 2 · 6 no total")).toBeVisible();
    await expect(page.getByRole("row", { name: /COTA-L6/ })).toBeVisible();
    await expect(page.getByRole("row", { name: /COTA-L1/ })).toHaveCount(0);
    await semProblemasDeAcessibilidade(page);

    await page.getByRole("button", { name: "Próxima" }).click();
    await expect(page.getByText("Página 2 de 2 · 6 no total")).toBeVisible();
    await expect(page.getByRole("row", { name: /COTA-L1/ })).toBeVisible();

    await page.getByLabel("Situação").selectOption({ label: "Em análise" });
    await expect(page.getByText("Página 1 de 1 · 1 no total")).toBeVisible();
    await expect(page.getByRole("row", { name: /COTA-L6/ })).toContainText("Em análise");

    await page.getByLabel("Situação").selectOption({ label: "Pago" });
    await expect(page.getByText("Nenhum processo nesta situação.")).toBeVisible();
  });
});
