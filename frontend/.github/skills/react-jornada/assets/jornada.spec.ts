// MODELO de spec de jornada (copie para e2e/jNN-<jornada>.spec.ts e preencha).
// Escreva ANTES da tela, a partir da especificação: os passos e os textos são os que o negócio descreveu.
// O spec falha antes da tela existir e passa depois. Se tela e spec discordarem, quem está errada é a tela.
import { abrirProcesso, agir, aviso, bloco, expect, semProblemasDeAcessibilidade, souEu, test } from "./apoio";

test("[NN-01] <o que a pessoa consegue fazer, em linguagem de negócio>", async ({ page }) => {
  // 1. Prepare pela própria tela (nada de API direta): abrir o que a jornada precisa.
  await abrirProcesso(page, "Serviço", "COTA-EXEMPLO", "1.000,00");

  // 2. Chegue à tela como a pessoa chega: pelo menu ou por um link, com o nome que ela vê.
  await page.getByRole("link", { name: "<item do menu>" }).click();
  await expect(page.getByRole("heading", { name: "<título da tela>", level: 1 })).toBeVisible();
  await semProblemasDeAcessibilidade(page);

  // 3. Aja e confira pelo papel e pelo texto exato da especificação.
  //    Abas: getByRole("tab"). Linha de tabela: getByRole("row", { name: /<texto da linha>/ }).
  //    Diálogo: getByRole("dialog"). Aviso: aviso(page) (um só por vez). Bloco com título: bloco(page, "<título>").
  await souEu(page, "ana");
  await agir(page, "Iniciar a análise");
  await expect(aviso(page)).toHaveText("Iniciar a análise: feito. O processo agora está Em análise.");

  // 4. Estado vazio e erro também são jornada: confira o texto que a especificação pede.
  await expect(bloco(page, "<título do bloco>")).toContainText("<texto da especificação>");
});
