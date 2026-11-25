// Passos da jornada do processo de pagamento (desta POC). Os passos genéricos (test, aviso, agir, acessibilidade)
// estão em apoio.ts, que vale para qualquer projeto; os de cada jornada ficam num arquivo como este.
import { aviso, expect } from "./apoio";
import type { Page } from "@playwright/test";

/** Abre um processo pela tela "Abrir processo", espera o aviso de aberto e devolve o número dele. */
export async function abrirProcesso(page: Page, tipo: "Serviço" | "Imobiliário", cota: string, valor: string): Promise<number> {
  await page.goto("./#/abrir");
  await page.getByText(tipo, { exact: true }).click();
  await page.getByLabel("Número da cota").fill(cota);
  await page.getByLabel("Valor a pagar (R$)").fill(valor);
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(aviso(page)).toHaveText("Processo aberto.");
  return Number(/#\/processo\/(\d+)/.exec(page.url())?.[1]);
}
