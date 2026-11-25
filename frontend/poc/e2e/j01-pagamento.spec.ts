// Jornada do processo de pagamento, contra o serviço simulado. Escrita a partir da especificação:
// os passos e os textos são os que o negócio descreveu, não os que a tela por acaso mostra.
import { agir, aviso, bloco, expect, semProblemasDeAcessibilidade, souEu, test } from "./apoio";
import { abrirProcesso } from "./passos-processo";

test("[J-01] Imobiliário acima da alçada: análise, vistoria, alçada e pagamento", async ({ page }) => {
  const id = await abrirProcesso(page, "Imobiliário", "COTA-80K", "80.000,00");
  await semProblemasDeAcessibilidade(page);

  await souEu(page, "ana");
  await agir(page, "Iniciar a análise");
  await agir(page, "Concluir a análise");
  await expect(aviso(page)).toContainText("Aguardando vistoria");

  await souEu(page, "vitor");
  await agir(page, "Concluir a vistoria");
  await expect(aviso(page)).toContainText("Aguardando alçada");

  await souEu(page, "carla");
  await agir(page, "Aprovar na alçada");

  await souEu(page, "tesouraria");
  await agir(page, "Pagar");
  await expect(page.getByText("Nenhuma ação: o processo está numa situação final.")).toBeVisible();

  for (const autor of ["ana", "vitor", "carla", "tesouraria"]) await expect(bloco(page, "Histórico")).toContainText(`por ${autor}`);

  await page.getByText("Por trás da tela").click();
  await expect(page.getByTestId("chamadas")).toContainText(new RegExp(`POST \\S*\\s?\\S*/processos/${id}/acoes/PAGAR\\nX-Usuario: tesouraria`));
  await semProblemasDeAcessibilidade(page);
});

test("[J-02] Serviço até a alçada é aprovado direto na análise", async ({ page }) => {
  await abrirProcesso(page, "Serviço", "COTA-50K", "50.000,00");
  await agir(page, "Iniciar a análise");
  await agir(page, "Concluir a análise");
  await expect(aviso(page)).toContainText("O processo agora está Aprovado");
});

test("[J-03] Reprovar pede motivo, e o motivo vai para o histórico", async ({ page }) => {
  await abrirProcesso(page, "Serviço", "COTA-REP", "1.000,00");
  await agir(page, "Iniciar a análise");
  await page.getByRole("button", { name: "Reprovar", exact: true }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await semProblemasDeAcessibilidade(page);
  await page.getByRole("dialog").getByRole("textbox").fill("Documentação inconsistente");
  await page.getByRole("dialog").getByRole("button", { name: "Confirmar" }).click();
  await expect(aviso(page)).toContainText("Reprovar: feito");
  await expect(bloco(page, "Histórico")).toContainText("Documentação inconsistente");
});

test("[J-04] Valor em formato errado é apontado no campo, sem chamar a API", async ({ page }) => {
  await page.goto("./#/abrir");
  await page.getByLabel("Número da cota").fill("COTA-X");
  await page.getByLabel("Valor a pagar (R$)").fill("oitenta mil");
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(page.getByText("Use o formato 80.000,00")).toBeVisible();
  await semProblemasDeAcessibilidade(page);
});
