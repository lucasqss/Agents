---
name: prototipo-de-jornada
description: Use para conduzir com o usuário a UI de rascunho e prototipação das jornadas de um serviço - telas que preenchem e disparam as requisições da API, mostram as jornadas ao negócio (inclusive o que o backend ainda não tem) e rodam também sem backend - pedido a pedido, com screenshot a cada mudança. Use quando pedirem "telas para mostrar/testar a jornada X", "um front para testar a API", "protótipo de tela", ou ao ajustar telas, campos e botões a pedido. A stack, as camadas e as receitas são das skills ui-jornada e react-jornada (frontend/), com o modelo em frontend/poc. Não use para testar regra de negócio (é da teste-unidade/teste-aceitacao).
---

# Protótipo de jornada: o ciclo com o usuário

Uma UI **de rascunho** por serviço, montada aos poucos com o usuário: ele descreve, você altera, mostra e espera o próximo pedido. Serve para conversar sobre as jornadas com telas de verdade (inclusive o que o serviço ainda não tem) e para testar a API sem montar JSON à mão. **Não é o produto final.**

**Como fazer cada coisa** (pastas, componentes, dados, contrato, testes) está nas skills de `frontend/`: `ui-jornada` (a arquitetura: o front descreve, o backend decide) e `react-jornada` (as receitas, com o arquivo-modelo de cada tarefa). O modelo é a POC `frontend/poc`, que é o protótipo das jornadas deste `exemplo-testes`. Esta skill cuida só do **ciclo com o usuário**.

## Projeto novo

1. `frontend/novo-front.sh <serviço>/telas <contrato do serviço> "<nome>"` (o contrato é o `docs/api/openapi-base.json` do serviço, ou a URL do `/q/openapi` em dev). Fica fora de `src/` do serviço: o build dele não pode vê-lo.
2. `npm ci && npm run gerar:api && npm run verificar` na pasta nova.
3. **CORS só em dev** no serviço: `%dev.quarkus.http.cors.enabled=true` e `%dev.quarkus.http.cors.origins=http://localhost:5173,http://127.0.0.1:5173`. Nada fora de `%dev`.
4. Leia o contrato e as especificações de aceitação do serviço: o vocabulário de negócio vem delas. Comece pela primeira jornada (receitas da `react-jornada`).

## Ciclo com o usuário

Para cada pedido ("põe o campo X", "essa tela precisa de um filtro", "o botão Y some depois de pagar", "quero mostrar como seria anexar documento"):

1. **Entenda o pedido em termos da jornada.** Se o serviço ainda não tem o que a tela precisa, é **telas primeiro**: a operação vai para o contrato proposto (receita da `react-jornada`) e aparece com o selo "Proposta".
2. **Spec antes da tela**, se a jornada é nova ou muda: o `e2e/jNN-<jornada>.spec.ts` a partir do que o usuário descreveu, com os textos dele.
3. **Altere o mínimo**, pela receita da `react-jornada`.
4. **Verifique:** `npm run verificar` e `npm run test:e2e` (o harness do agente já roda o `verificar` ao fim do turno).
5. **Mostre:** abra no navegador (`npm run dev:falso`, ou `npm run dev` com o serviço no ar), percorra o trecho que mudou, tire screenshot em 1280px e 390px e leia o console. Mostre o screenshot e diga o que mudou, e o que ficou como proposta.
6. **Espere o próximo pedido.**

## Regras do rascunho

- **Selo "Protótipo"** sempre visível, e **"Proposta"** em tudo o que o serviço ainda não tem.
- **Linguagem de negócio** em rótulos, botões e mensagens.
- **O serviço simulado é cópia:** a regra que vale é a do serviço. `npm run test:e2e:real` (com o serviço em dev) acusa a divergência; o que vira caso de uso sai do contrato proposto para o serviço (skills `especificar-caso-de-uso` e `arquiteto-padrao`).
- **Os e2e provam que a jornada é navegável e acessível.** Não repita neles regra que já é da aceitação (Cucumber).

Verificação completa antes de dizer "pronto": [checklist](references/verificacao.md).

## Exemplos

- `frontend/poc`: a referência (as jornadas do `exemplo-testes`, com lista paginada, assistente com upload e o contrato proposto).
- `exemplo-negocial/prototipo-telas`: três jornadas, JWT de dev e "telas primeiro" na organização anterior (stack sem shadcn, contrato proposto em `src/jornada/contrato-proposto.md`). Tela nova segue a POC.
