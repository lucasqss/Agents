# Checklist de verificação do protótipo

Rode antes de dizer "pronto". Relate o que rodou e o resultado; o que não deu para rodar, diga que não rodou e por quê.

## Sempre

- [ ] `npm run typecheck` sem erro.
- [ ] `npm run test:e2e` verde (computador e celular, serviço simulado).
- [ ] No navegador (MCP do Playwright), com `npm run dev:falso`:
  - [ ] a jornada principal do README, do começo ao fim;
  - [ ] um erro da API aparece na tela em linguagem de negócio (ex.: ação inválida, campo obrigatório vazio);
  - [ ] "Por trás da tela" mostra a última chamada, com os cabeçalhos;
  - [ ] console sem erro;
  - [ ] screenshot em 1280px e em 390px, mostrados ao usuário.

## Quando o serviço está disponível (JDK + Docker)

- [ ] `./subir.sh`: a mesma jornada contra o serviço de verdade (prova o CORS de `%dev` e os cabeçalhos).
- [ ] O serviço simulado devolve os mesmos status e mensagens que o serviço para os erros mostrados.

## Ao mudar o serviço

- [ ] `./mvnw verify` do serviço continua verde (o CORS mudou só em `%dev`; o protótipo fica fora de `src/`).
