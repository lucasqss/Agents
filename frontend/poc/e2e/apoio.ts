// Apoio dos testes das jornadas. Todo spec importa `test` e `expect` DAQUI (não de @playwright/test):
//   - cada teste começa com o navegador limpo (sem processos simulados nem persona guardada);
//   - cada teste falha se a tela escrever erro no console.
// Os passos falam a língua da tela: papéis acessíveis (botão, aba, diálogo, aviso) e os textos que a pessoa lê.
import AxeBuilder from "@axe-core/playwright";
import { test as base, expect, type Page } from "@playwright/test";

export { expect };

export const test = base.extend({
  page: async ({ page }, usar) => {
    const erros: string[] = [];
    page.on("console", (m) => {
      if (m.type() === "error" && !m.location().url.endsWith("/favicon.ico")) erros.push(m.text());
    });
    await page.goto("./");
    await page.evaluate(() => localStorage.clear());
    await usar(page);
    expect(erros, "erros no console do navegador").toEqual([]);
  },
});

/** Confere a tela atual com o axe (WCAG 2 A e AA): nome de botão, rótulo de campo, contraste, papéis ARIA. */
export async function semProblemasDeAcessibilidade(page: Page) {
  const { violations } = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();
  const resumo = violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(" ")).join(", ")})`);
  expect(resumo, "problemas de acessibilidade").toEqual([]);
}

/** O aviso de resultado: há um só na tela (o novo substitui o anterior). */
export function aviso(page: Page) {
  return page.getByRole("status");
}

/** Troca quem age, pelo usuário (o valor da opção): "ana", "vitor", "carla", "tesouraria". */
export async function souEu(page: Page, usuario: string) {
  await page.getByLabel("Quem sou eu").selectOption(usuario);
}

/** Abre um processo pela tela "Abrir processo" e espera o aviso de aberto. */
export async function abrirProcesso(page: Page, tipo: "Serviço" | "Imobiliário", cota: string, valor: string) {
  await page.goto("./#/abrir");
  await page.getByText(tipo, { exact: true }).click();
  await page.getByLabel("Número da cota").fill(cota);
  await page.getByLabel("Valor a pagar (R$)").fill(valor);
  await page.getByRole("button", { name: "Abrir processo" }).click();
  await expect(aviso(page)).toHaveText("Processo aberto.");
}

/** Clica na ação (escrevendo o motivo no diálogo, se houver) e espera o aviso "<ação>: feito". */
export async function agir(page: Page, acao: string, motivo?: string) {
  await page.getByRole("button", { name: acao, exact: true }).click();
  if (motivo !== undefined) {
    await page.getByRole("dialog").getByRole("textbox").fill(motivo);
    await page.getByRole("dialog").getByRole("button", { name: "Confirmar" }).click();
  }
  await expect(aviso(page)).toContainText(`${acao}: feito`);
}

/** Um bloco da tela pelo título (o Cartao com título é uma região nomeada). */
export function bloco(page: Page, titulo: string) {
  return page.getByRole("region", { name: titulo });
}
