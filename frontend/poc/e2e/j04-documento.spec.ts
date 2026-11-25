// PROPOSTA: anexar um documento ao processo, num assistente de 3 passos.
import { aviso, bloco, expect, semProblemasDeAcessibilidade, test } from "./apoio";
import { abrirProcesso } from "./passos-processo";

const pdf = { name: "identidade.pdf", mimeType: "application/pdf", buffer: Buffer.from("%PDF-1.4 documento de teste") };

test.describe("Anexar documento", { tag: "@proposta" }, () => {
  test("[D-01] Tipo, arquivo, conferir e enviar: o documento aparece no processo", async ({ page }) => {
    await abrirProcesso(page, "Serviço", "COTA-DOC", "1.000,00");
    await expect(bloco(page, "Documentos")).toContainText("Nenhum documento anexado ainda.");
    await bloco(page, "Documentos").getByRole("link", { name: "Anexar documento" }).click();

    await expect(page.getByRole("button", { name: "Continuar" })).toBeDisabled();
    await page.getByText("Documento de identidade", { exact: true }).click();
    await semProblemasDeAcessibilidade(page);
    await page.getByRole("button", { name: "Continuar" }).click();

    await page.getByLabel("Arquivo do documento").setInputFiles(pdf);
    await expect(page.getByText("identidade.pdf")).toBeVisible();
    await semProblemasDeAcessibilidade(page);
    await page.getByRole("button", { name: "Continuar" }).click();

    await expect(page.getByText("Documento de identidade")).toBeVisible();
    await semProblemasDeAcessibilidade(page);
    await page.getByRole("button", { name: "Enviar documento" }).click();

    await expect(aviso(page)).toHaveText("Documento anexado.");
    await expect(bloco(page, "Documentos")).toContainText("identidade.pdf");
    await expect(bloco(page, "Documentos")).toContainText("por ana");
  });

  test("[D-02] Formato não aceito é apontado antes do envio", async ({ page }) => {
    await abrirProcesso(page, "Serviço", "COTA-DOC2", "1.000,00");
    await bloco(page, "Documentos").getByRole("link", { name: "Anexar documento" }).click();
    await page.getByText("Outro documento", { exact: true }).click();
    await page.getByRole("button", { name: "Continuar" }).click();
    await page.getByLabel("Arquivo do documento").setInputFiles({ name: "planilha.xlsx", mimeType: "application/octet-stream", buffer: Buffer.from("x") });
    await expect(page.getByText("Formato não aceito: envie .pdf, .jpg, .jpeg, .png")).toBeVisible();
    await expect(page.getByRole("button", { name: "Continuar" })).toBeDisabled();
  });
});
