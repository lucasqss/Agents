# Checklist de verificação do protótipo

Rode antes de dizer "pronto". Relate o que rodou e o resultado; o que não deu para rodar, diga que não rodou e por quê.

## Sempre

- [ ] `npm run verificar` verde: contrato em dia, typecheck, lint (hooks e acessibilidade), arquitetura (UI-001 a UI-006), testes de unidade.
- [ ] `npm run test:e2e` verde (computador e celular, serviço simulado, com a checagem de acessibilidade em cada tela).
- [ ] No navegador, com `npm run dev:falso`:
  - [ ] a jornada que mudou, do começo ao fim;
  - [ ] um erro da API aparece na tela em linguagem de negócio (ação inválida, campo obrigatório vazio);
  - [ ] o que é proposta aparece com o selo "Proposta";
  - [ ] "Por trás da tela" mostra as últimas chamadas, com os cabeçalhos;
  - [ ] console sem erro;
  - [ ] screenshot em 1280px e em 390px, mostrados ao usuário.

## Quando o serviço está disponível (JDK + Docker)

- [ ] `npm run test:e2e:real` com o serviço em `quarkus:dev`: os mesmos percursos contra o serviço de verdade (prova o CORS de `%dev`, os cabeçalhos e que o simulado não divergiu). Os `@proposta` ficam de fora.
- [ ] `npm run contrato:propostas`: o que ainda é proposta, e o que o serviço já publicou (sai do `proposto.yaml`).

## Ao mudar o serviço

- [ ] `./mvnw verify` do serviço continua verde (o CORS mudou só em `%dev`; o protótipo fica fora de `src/`).
- [ ] `npm run gerar:api` no front, e `npm run verificar` acusa o que a mudança do contrato quebrou.
