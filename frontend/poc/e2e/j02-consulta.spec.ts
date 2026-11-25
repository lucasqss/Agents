// Jornada de consulta: achar um processo pelo número, na tela inicial. Modelo curto de spec.
import { abrirProcesso, aviso, expect, semProblemasDeAcessibilidade, test } from "./apoio";

test("[C-01] Consultar pelo número abre o processo; a lista de recentes o mostra", async ({ page }) => {
  await abrirProcesso(page, "Serviço", "COTA-CONSULTA", "1.000,00");
  await page.getByRole("link", { name: "Processos", exact: true }).first().click();
  await expect(page.getByRole("row", { name: /COTA-CONSULTA/ })).toBeVisible();
  await semProblemasDeAcessibilidade(page);

  await page.getByLabel("Número do processo").fill("1");
  await page.getByRole("button", { name: "Consultar" }).click();
  await expect(page.getByRole("heading", { name: "Processo 1", level: 1 })).toBeVisible();
  await expect(aviso(page)).toHaveCount(0);
});

test("[C-02] Processo inexistente mostra o erro da API e o caminho de volta", async ({ page }) => {
  await page.getByLabel("Número do processo").fill("999");
  await page.getByRole("button", { name: "Consultar" }).click();
  await expect(page.getByRole("alert")).toContainText("Processo 999 não encontrado");
  await semProblemasDeAcessibilidade(page);
  await page.getByRole("link", { name: "Voltar aos processos" }).click();
  await expect(page.getByRole("heading", { name: "Processos", level: 1 })).toBeVisible();
});
