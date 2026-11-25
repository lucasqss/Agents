// Estados que toda tela tem: página que não existe, falha de rede com "Tentar de novo", e o carregando.
import { aviso, expect, semProblemasDeAcessibilidade, test } from "./apoio";
import { abrirProcesso } from "./passos-processo";

test("[E-01] Endereço que não existe mostra 'Página não encontrada' e o caminho de volta", async ({ page }) => {
  await page.goto("./#/nao-existe");
  await expect(page.getByRole("heading", { name: "Página não encontrada", level: 1 })).toBeVisible();
  await semProblemasDeAcessibilidade(page);
  await page.getByRole("link", { name: "Voltar aos processos" }).click();
  await expect(page.getByRole("heading", { name: "Processos", level: 1 })).toBeVisible();
});

test("[E-02] Sem rede, a tela diz o que houve e oferece 'Tentar de novo'", { tag: "@simulado" }, async ({ page }) => {
  const id = await abrirProcesso(page, "Serviço", "COTA-REDE", "1.000,00");
  await page.evaluate(() => localStorage.setItem("simulado:sem-rede", "1"));
  await page.goto(`./#/processo/${id}`);
  await expect(page.getByRole("alert")).toContainText("Sem resposta");
  await semProblemasDeAcessibilidade(page);

  await page.evaluate(() => localStorage.removeItem("simulado:sem-rede"));
  await page.getByRole("button", { name: "Tentar de novo" }).click();
  await expect(page.getByRole("heading", { name: "Histórico" })).toBeVisible();
  await expect(aviso(page)).toHaveCount(0);
});
