// GERADO por `npm run gerar:api`. As operações do contrato proposto que o serviço ainda não publicou.
// No modo real, a camada api não as chama (devolve 501 "ainda é proposta"); no modo simulado, o simulado atende.
export const PROPOSTAS_PENDENTES: ReadonlySet<string> = new Set([
  "GET /processos",
  "GET /processos/{id}/documentos",
  "POST /processos/{id}/documentos"
]);
