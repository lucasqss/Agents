# Instruções para agentes de IA: front

Convenções de front para os serviços deste repositório: telas que **desenham a jornada e acionam os endpoints do backend** (thin client). A stack é **React 19 + Vite + TypeScript strict + Tailwind 4 + shadcn/ui**, com o cliente gerado do OpenAPI. A referência de tudo é a POC em `poc/`.

**Use as skills:** `ui-jornada` para a arquitetura (o front descreve, o backend decide; as cinco camadas; as regras UI-001 a UI-009) e `react-jornada` para a receita de cada tarefa em React, com o arquivo da POC que serve de modelo.

## Como trabalhar

1. **Copie o vizinho.** Antes de criar uma tela ou um componente, abra o modelo na POC (a `react-jornada` diz qual) e faça igual.
2. **Mude o mínimo.** Só o que a tarefa pede.
3. **Verifique rodando:** `npm run verificar` (contrato, typecheck, lint, arquitetura) e `npm run test:e2e`. Código que você não rodou não está pronto.
4. **Relate o que é fato:** o que mudou em cada camada, o que rodou e o resultado.

## Proibido

- Regra de negócio na tela (comparar valor com limite, decidir se uma ação vale). Quem decide é a API.
- `fetch`, ou o cliente do openapi-fetch, fora do núcleo e da camada api; caminho de endpoint fora da camada api.
- Código da API (`EM_ANALISE`, `"PAGO"`) fora da jornada.
- Editar `src/api/esquema.d.ts` à mão: ele é gerado (`npm run gerar:api`).
- Desligar, afrouxar ou editar o `verificar.sh`, as regras de lint ou o `tsconfig` para o build passar. Se achar a regra errada, diga e pare.
