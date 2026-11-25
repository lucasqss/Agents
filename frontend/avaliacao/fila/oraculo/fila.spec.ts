import { expect, test, type Page } from "@playwright/test";

// Oráculo do A/B da react-jornada: a tela "Fila de trabalho". ESTRITO=1: abas acessíveis (role tab) e um aviso por vez.
const ESTRITO = process.env.ESTRITO === "1";

async function abrir(page: Page, cota: string, valor: string) {
  await page.goto("./#/abrir");
  await page.getByText("Serviço", { exact: true }).click();
  await page.getByLabel("Número da cota").fill(cota);
  await page.getByLabel("Valor a pagar (R$)").fill(valor);
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(page.getByText("Processo aberto.")).toBeVisible();
}

async function aba(page: Page, nome: string) {
  if (ESTRITO) await page.getByRole("tab", { name: new RegExp(nome) }).click();
  else await page.getByText(nome, { exact: false }).first().click();
}

const linha = (page: Page, cota: string) => page.getByRole("row", { name: new RegExp(cota) });

async function avisoContem(page: Page, texto: string) {
  if (ESTRITO) {
    await expect(page.getByRole("status")).toHaveCount(1);
    await expect(page.getByRole("status")).toContainText(texto);
  } else {
    await expect(page.getByText(texto)).toBeVisible();
  }
}

test.beforeEach(async ({ page }) => {
  await page.goto("./");
  await page.evaluate(() => localStorage.clear());
});

test("[F-01] Fila vazia mostra que não há processos em andamento", async ({ page }) => {
  await page.goto("./");
  await page.getByRole("link", { name: "Fila de trabalho" }).click();
  await expect(page.getByRole("heading", { name: "Fila de trabalho", level: 1 })).toBeVisible();
  await expect(page.getByText("Nenhum processo em andamento.")).toBeVisible();
});

test("[F-02] Agir pela fila: aviso, motivo e processo finalizado muda de aba", async ({ page }) => {
  const erros: string[] = [];
  page.on("console", (m) => m.type() === "error" && !m.location().url.endsWith("/favicon.ico") && erros.push(m.text()));

  await abrir(page, "COTA-FILA-A", "1.000,00");
  await abrir(page, "COTA-FILA-B", "2.000,00");
  await page.getByRole("link", { name: "Fila de trabalho" }).click();

  await expect(linha(page, "COTA-FILA-A")).toBeVisible();
  await expect(linha(page, "COTA-FILA-B")).toBeVisible();

  await linha(page, "COTA-FILA-A").getByRole("button", { name: "Iniciar a análise", exact: true }).click();
  await avisoContem(page, "Iniciar a análise: feito. O processo agora está Em análise.");
  await expect(linha(page, "COTA-FILA-A")).toContainText("Em análise");

  await linha(page, "COTA-FILA-A").getByRole("button", { name: "Reprovar", exact: true }).click();
  await page.getByRole("dialog").getByRole("textbox").fill("Documento ilegível");
  await page.getByRole("dialog").getByRole("button", { name: "Confirmar" }).click();
  await avisoContem(page, "Reprovar: feito. O processo agora está Reprovado.");

  await expect(linha(page, "COTA-FILA-A")).toHaveCount(0);
  await expect(linha(page, "COTA-FILA-B")).toBeVisible();
  await aba(page, "Finalizados");
  await expect(linha(page, "COTA-FILA-A")).toContainText("Reprovado");
  await expect(linha(page, "COTA-FILA-B")).toHaveCount(0);

  await aba(page, "Em andamento");
  await linha(page, "COTA-FILA-B").getByRole("button", { name: "Iniciar a análise", exact: true }).click();
  await avisoContem(page, "Iniciar a análise: feito.");
  expect(erros).toEqual([]);
});
