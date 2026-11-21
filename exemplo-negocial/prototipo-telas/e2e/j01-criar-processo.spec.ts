import { expect, test } from "@playwright/test";
import { comecarDoZero, criarProcessoImobiliario } from "./apoio";

test.beforeEach(async ({ page }) => comecarDoZero(page));

test("[J-01] Criar processo imobiliário: dois imóveis, comprador e vendedor que recebe", async ({ page }) => {
  const erros: string[] = [];
  page.on("console", (m) => m.type() === "error" && erros.push(m.text()));

  await criarProcessoImobiliario(page);

  // Cada parte recebeu um e-mail
  await expect(page.getByRole("link", { name: "Abrir como Maria" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Abrir como João" })).toBeVisible();
  expect(erros).toEqual([]);
});

test("[J-01] A cota com processo em andamento fica indisponível", async ({ page }) => {
  await criarProcessoImobiliario(page);
  await page.goto("./#/criacao/cota");
  await expect(page.getByText(/Indisponível: o processo \d+ está aguardando documentos/)).toBeVisible();
});

test("[J-01] Favorecidos que não fecham o valor não deixam continuar", async ({ page }) => {
  await page.goto("./#/criacao");
  await page.getByRole("button", { name: /^Cota COTA-\d+-A / }).click();
  await page.getByRole("button", { name: "Continuar" }).click();
  await page.getByRole("button", { name: /Serviço/ }).click();
  await page.getByRole("button", { name: "Continuar" }).click();
  await page.getByLabel("Descrição do serviço").fill("Reforma do banheiro");
  await page.getByLabel("Prestador").fill("Reformas Silva Ltda.");
  await page.getByLabel("Valor (R$)").fill("20.000,00");
  await page.getByRole("button", { name: "Continuar" }).click();
  await page.getByRole("button", { name: "Acrescentar parte" }).click();
  const prestador = page.getByRole("group", { name: "Parte 2" });
  await prestador.getByLabel("Nome").fill("Reformas Silva Ltda.");
  await prestador.getByLabel("CPF ou CNPJ").fill("12345678000190");
  await prestador.getByLabel(/E-mail/).fill("contato@reformassilva.com");
  await prestador.getByLabel("Quanto recebe (R$)").fill("15.000,00");
  await expect(page.getByText("Faltam R$ 5.000,00")).toBeVisible();
  await expect(page.getByRole("button", { name: "Continuar" })).toBeDisabled();
});
