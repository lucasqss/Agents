import { expect, test, type Page } from "@playwright/test";
import { comecarDoZero, criarProcessoImobiliario, enviarTudoComo, pdf, souEu } from "./apoio";

test.beforeEach(async ({ page }) => comecarDoZero(page));

async function agir(page: Page, acao: string) {
  await page.getByRole("button", { name: acao, exact: true }).click();
}

test("[J-03] Análise com recusa e reenvio, vistoria, alçada e pagamento", async ({ page }) => {
  const id = await criarProcessoImobiliario(page);
  await enviarTudoComo(page, "João Pereira");
  await enviarTudoComo(page, "Maria Souza");

  await page.goto(`./#/analise`);
  await souEu(page, "ana");
  await page.getByRole("cell", { name: String(id), exact: true }).click();
  await agir(page, "Iniciar a análise");
  await expect(page.getByRole("status")).toContainText("O processo agora está Em análise");

  // Recusa o comprovante de residência do João: pendente de documento, e o João recebe outro e-mail
  await page.getByRole("button", { name: "Recusar Comprovante de residência de João Pereira" }).click();
  await page.getByRole("dialog").getByRole("textbox").fill("Comprovante com mais de 90 dias");
  await page.getByRole("dialog").getByRole("button", { name: "Confirmar" }).click();
  await expect(page.getByText("Pendente de documento").first()).toBeVisible();

  // O João reenvia só aquele, pelo e-mail de reenvio: o processo volta para em análise
  await page.goto("./#/documentos");
  await page.getByRole("button", { name: /João Pereira.*reenvio/ }).click();
  await page.getByRole("article", { name: "E-mail aberto" }).getByRole("link", { name: "Enviar documentos" }).click();
  await expect(page.getByText("Recusado: Comprovante com mais de 90 dias")).toBeVisible();
  await page.getByLabel("Trocar comprovante de residência").setInputFiles(pdf("comprovante-novo.pdf"));
  await expect(page.getByRole("status").filter({ hasText: "enviado." })).toBeVisible();

  await page.goto(`./#/analise/${id}`);
  await expect(page.getByText("Em análise").first()).toBeVisible();

  // Concluir sem aceitar tudo é recusado
  await agir(page, "Concluir a análise");
  await expect(page.getByRole("alert")).toContainText("ainda não aceito");

  // Aceita todos e conclui: imobiliário vai para a vistoria
  const aceitar = page.getByRole("button", { name: /^Aceitar / });
  for (let faltam = await aceitar.count(); faltam > 0; faltam--) {
    await aceitar.first().click();
    await expect(aceitar).toHaveCount(faltam - 1);
  }
  await agir(page, "Concluir a análise");
  await expect(page.getByRole("status")).toContainText("Aguardando vistoria");
  await agir(page, "Concluir a vistoria");
  await expect(page.getByRole("status")).toContainText("Aguardando alçada"); // R$ 100.000,00 passa da alçada

  // Ana (backoffice) não aprova na alçada; Bruno (aprovador) aprova
  await agir(page, "Aprovar na alçada");
  await expect(page.getByRole("alert")).toContainText("Sem permissão para esta operação");
  await souEu(page, "bruno");
  await agir(page, "Aprovar na alçada");
  await expect(page.getByRole("status")).toContainText("O processo agora está Aprovado");

  await souEu(page, "tesouraria");
  await agir(page, "Solicitar o pagamento");
  await expect(page.getByRole("status")).toContainText("Pagamento solicitado");
  await page.getByRole("button", { name: /Rodar a rotina de pagamento/ }).click();
  await expect(page.getByText("Nenhuma ação: o processo está numa situação final.")).toBeVisible();
  await expect(page.getByText(/Protocolo TES-/)).toBeVisible();
});
