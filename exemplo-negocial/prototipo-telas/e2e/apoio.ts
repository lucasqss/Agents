import { expect, type Page } from "@playwright/test";

/** Um PDF de mentira, para os envios. */
export const pdf = (nome: string) => ({ name: nome, mimeType: "application/pdf", buffer: Buffer.from("%PDF-1.4\n% documento de teste\n") });

export async function comecarDoZero(page: Page) {
  await page.goto("./");
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.getByRole("button", { name: "Preparar demonstração" }).click();
  await expect(page.getByText(/Demonstração pronta/)).toBeVisible();
}

export async function souEu(page: Page, usuario: string) {
  await page.getByLabel("Quem sou eu").selectOption(usuario);
}

/** Jornada 1 inteira: imobiliário de R$ 100.000,00 em dois imóveis, comprador e um vendedor que recebe tudo. */
export async function criarProcessoImobiliario(page: Page) {
  await page.getByRole("link", { name: /1\. Criar o processo/ }).first().click();

  await page.getByRole("button", { name: /^Cota COTA-\d+-A / }).click(); // a cota de R$ 150.000,00
  await page.getByRole("button", { name: "Continuar" }).click();

  await page.getByRole("button", { name: /Imobiliário/ }).click();
  await page.getByRole("button", { name: "Continuar" }).click();

  const preencherImovel = async (i: number, endereco: string, valor: string) => {
    const bloco = page.getByRole("group", { name: `Imóvel ${i}` });
    await bloco.getByLabel("Endereço").fill(endereco);
    await bloco.getByLabel("Matrícula").fill(`45.87${i}`);
    await bloco.getByLabel("Cartório de registro").fill("3º Registro de Imóveis de São Paulo");
    await bloco.getByLabel("Valor (R$)").fill(valor);
  };
  await preencherImovel(1, "Rua das Flores, 120", "60.000,00");
  await page.getByRole("button", { name: "Acrescentar imóvel" }).click();
  await preencherImovel(2, "Rua das Flores, 120, garagem", "40.000,00");
  await expect(page.getByText("R$ 100.000,00")).toBeVisible();
  await page.getByRole("button", { name: "Continuar" }).click();

  await page.getByRole("button", { name: "Acrescentar parte" }).click();
  const vendedor = page.getByRole("group", { name: "Parte 2" });
  await vendedor.getByLabel("Nome").fill("João Pereira");
  await vendedor.getByLabel("CPF ou CNPJ").fill("98765432100");
  await vendedor.getByLabel(/E-mail/).fill("joao.pereira@exemplo.com");
  await vendedor.getByLabel("Quanto recebe (R$)").fill("100.000,00");
  await expect(page.getByText("Fechou o valor do processo")).toBeVisible();
  await page.getByRole("button", { name: "Continuar" }).click();

  await page.getByRole("button", { name: "Criar processo" }).click();
  await expect(page.getByText(/Processo \d+ criado\. Ele aguarda os documentos/)).toBeVisible();
  return Number((await page.getByRole("heading", { level: 1 }).textContent())!.match(/\d+/)![0]);
}

/** Jornada 2: a parte abre o próprio e-mail e envia todos os documentos pendentes. */
export async function enviarTudoComo(page: Page, nome: string) {
  await page.goto("./#/documentos");
  await page.getByRole("button", { name: new RegExp(nome) }).first().click();
  await page.getByRole("article", { name: "E-mail aberto" }).getByRole("link", { name: "Enviar documentos" }).click();
  await expect(page.getByText(`Olá, ${nome}`, { exact: true })).toBeVisible();
  const pendentes = page.getByLabel(/^Enviar /);
  for (let faltam = await pendentes.count(), i = 1; faltam > 0; faltam--, i++) {
    await pendentes.first().setInputFiles(pdf(`documento-${i}.pdf`));
    await expect(pendentes).toHaveCount(faltam - 1); // espera o envio voltar antes do próximo
  }
}
