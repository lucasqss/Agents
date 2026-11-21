import { expect, test } from "@playwright/test";
import { comecarDoZero, criarProcessoImobiliario, enviarTudoComo } from "./apoio";

test.beforeEach(async ({ page }) => comecarDoZero(page));

test("[J-02] Cada parte envia os próprios documentos; com tudo enviado, o processo fica pronto para análise", async ({ page }) => {
  await criarProcessoImobiliario(page);

  // O vendedor (pessoa física) vê só os dele: identidade, residência, estado civil
  await enviarTudoComo(page, "João Pereira");
  await expect(page.getByText("3/3")).toBeVisible();
  await expect(page.getByText("Matrícula atualizada")).toHaveCount(0); // documentos do bem são do comprador

  // A compradora envia os dela e os dos dois imóveis: 3 + 2 × 2
  await enviarTudoComo(page, "Maria Souza");
  await expect(page.getByText("7/7")).toBeVisible();

  await page.goto("./#/analise");
  await expect(page.getByRole("tab", { name: /Prontos para análise/ })).toContainText("1");
});

test("[J-02] Link desconhecido é recusado", async ({ page }) => {
  await page.goto("./#/documentos/link-que-nao-existe");
  await expect(page.getByRole("alert")).toHaveText("Link inválido ou expirado");
});
