import { cadastro } from "./api";
import { demonstracao } from "./estado";

/** A cliente da demonstração. O CPF é o que o comprador leva nas partes. */
export const CLIENTE_DA_DEMONSTRACAO = { nome: "Maria Souza", cpf: "12345678909", email: "maria.souza@exemplo.com" };

/**
 * Cria, como Ana (backoffice), a cliente Maria e duas cotas dela: R$ 150.000,00 e R$ 40.000,00.
 * Usa os endpoints de cadastro que já existem; no modo simulado, o mesmo caminho.
 */
export async function prepararDemonstracao(): Promise<string | null> {
  const cliente = await cadastro.criarCliente(CLIENTE_DA_DEMONSTRACAO.nome, CLIENTE_DA_DEMONSTRACAO.cpf);
  if (!cliente.ok) return cliente.erro.mensagem;
  const sufixo = Date.now() % 10000;
  const ids: number[] = [];
  for (const [numero, credito] of [[`COTA-${sufixo}-A`, 150_000], [`COTA-${sufixo}-B`, 40_000]] as const) {
    const cota = await cadastro.criarCota(numero, credito, cliente.dados.id);
    if (!cota.ok) return cota.erro.mensagem;
    ids.push(cota.dados.id);
  }
  demonstracao.gravar({ clienteId: cliente.dados.id, cotas: ids });
  return null;
}
