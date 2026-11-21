import { expect, test, type Page } from "@playwright/test";

// Jornadas do README, contra o serviço simulado (o Playwright sobe o build em modo falso).
// Os textos procurados são os da tela, em linguagem de negócio: o teste lê a tela como o usuário.

/** Troca a persona pelo usuário (o valor da opção): "ana", "vitor", "carla", "tesouraria". */
async function souEu(page: Page, usuario: string) {
  await page.getByLabel("Quem sou eu").selectOption(usuario);
}

const historico = (page: Page) => page.locator("section", { has: page.getByRole("heading", { name: "Histórico" }) });

async function agir(page: Page, acao: string, motivo?: string) {
  await page.getByRole("button", { name: acao, exact: true }).click();
  if (motivo !== undefined) {
    await page.getByRole("dialog").getByRole("textbox").fill(motivo);
    await page.getByRole("dialog").getByRole("button", { name: "Confirmar" }).click();
  }
  await expect(page.getByRole("status")).toContainText(`${acao}: feito`);
}

async function abrir(page: Page, tipo: string, cota: string, valor: string) {
  await page.goto("./#/abrir");
  await page.getByText(tipo, { exact: true }).click();
  await page.getByLabel("Número da cota").fill(cota);
  await page.getByLabel("Valor a pagar (R$)").fill(valor);
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(page.getByRole("status")).toHaveText("Processo aberto.");
}

test.beforeEach(async ({ page }) => {
  await page.goto("./");
  await page.evaluate(() => localStorage.clear());
});

test("[J-01] Imobiliário acima da alçada: análise, vistoria, alçada e pagamento", async ({ page }) => {
  const erros: string[] = [];
  page.on("console", (m) => m.type() === "error" && erros.push(m.text()));

  await abrir(page, "Imobiliário", "COTA-80K", "80.000,00");

  await souEu(page, "ana");
  await agir(page, "Iniciar a análise");
  await agir(page, "Concluir a análise");
  await expect(page.getByRole("status")).toContainText("Aguardando vistoria");

  await souEu(page, "vitor");
  await agir(page, "Concluir a vistoria");
  await expect(page.getByRole("status")).toContainText("Aguardando alçada");

  await souEu(page, "carla");
  await agir(page, "Aprovar na alçada");

  await souEu(page, "tesouraria");
  await agir(page, "Pagar");
  await expect(page.getByText("Nenhuma ação: o processo está numa situação final.")).toBeVisible();

  for (const autor of ["ana", "vitor", "carla", "tesouraria"]) await expect(historico(page)).toContainText(`por ${autor}`);

  await page.getByText("Por trás da tela").click();
  await expect(page.getByTestId("ultima-chamada")).toContainText("X-Usuario: tesouraria");
  expect(erros).toEqual([]);
});

test("[J-02] Serviço até a alçada é aprovado direto na análise", async ({ page }) => {
  await abrir(page, "Serviço", "COTA-50K", "50.000,00");
  await agir(page, "Iniciar a análise");
  await agir(page, "Concluir a análise");
  await expect(page.getByRole("status")).toContainText("O processo agora está Aprovado");
});

test("[J-03] Reprovar pede motivo, e o motivo vai para o histórico", async ({ page }) => {
  await abrir(page, "Serviço", "COTA-REP", "1.000,00");
  await agir(page, "Iniciar a análise");
  await agir(page, "Reprovar", "Documentação inconsistente");
  await expect(historico(page)).toContainText("Documentação inconsistente");
});

test("[J-04] Valor em formato errado é apontado no campo, sem chamar a API", async ({ page }) => {
  await page.goto("./#/abrir");
  await page.getByLabel("Número da cota").fill("COTA-X");
  await page.getByLabel("Valor a pagar (R$)").fill("oitenta mil");
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(page.getByText("Use o formato 80.000,00")).toBeVisible();
});
