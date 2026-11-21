# Instruções para agentes de IA

Serviço Java 25 + Quarkus 3.33: o processo de pagamento, uma máquina de estados com um fluxo por tipo (serviço e imobiliário). O projeto existe para ser **modelo de testes e barreiras de build**: o comportamento e a estrutura são verificados a cada `./mvnw verify`, seja qual for o agente que mexer no código.

**Use as skills:** `teste-unidade` para provar regra de negócio (as matrizes exaustivas ficam aqui); `teste-componente` para o serviço montado (persistência, validação, erros, contrato) e o `*IT` contra o jar; `prototipo-de-jornada` para as telas de rascunho em `prototipo-telas/` (React + Vite + Tailwind, com serviço simulado); `teste-aceitacao` para a aceitação em tabelas que o QA preenche (a massa em `cucumber/massa/processos.md`, os cenários nos `Exemplos` dos `.feature` em português), que o negócio valida e o Cucumber executa. Cada comportamento é provado no nível mais baixo que consegue prová-lo.

## Como trabalhar

1. **Entenda antes de mexer.** Leia o código envolvido e quem o chama. Procure como o projeto já resolve algo parecido e faça igual.
2. **Planeje o que tem mais de um passo.** Liste os passos antes de editar e siga a lista.
3. **Mude o mínimo.** Só o que a tarefa pede.
4. **Verifique rodando.** Código que você não rodou não está pronto. Ao final de cada turno com mudança de código, o harness (`.github/hooks/`) roda a verificação rápida (arquitetura e domínio) e devolve a falha para você corrigir.
5. **Relate o que é fato.** Diga o que mudou, o que você rodou e o resultado. Teste que falhou é relatado como falha.

- **Regra de fluxo nova ou mudada** (estado, ação, transição, tipo): mude a tabela no domínio (`FluxoProcesso.comum()` ou o `Fluxo<Tipo>`) **e** a tabela escrita por extenso em `TabelaDosFluxosTest`, no mesmo commit. Ela é a especificação: se só o código mudou, o build falha mostrando a diferença. Depois, 1 a 3 linhas nas tabelas de cenários (`src/test/resources/cucumber/features/`), com o vocabulário que já existe.
- **Correção de bug.** Primeiro reproduza com um teste que falha pelo motivo do bug, no nível mais baixo possível. Corrija na camada dona da regra. Os testes antigos continuam passando sem terem sido alterados.

## Antes de terminar qualquer tarefa

1. Rode `./mvnw verify` (precisa de Docker: banco do Dev Services e o da aceitação).
2. **Se uma regra ArchUnit falhar, corrija o código, nunca a regra.** Não edite, apague nem afrouxe nada em `src/test/java/**/arquitetura/` nem em `.github/hooks/`. A falha traz um ID e um "Corrija:". Se achar que a regra está errada, diga isso e pare.
3. A aceitação roda isolada (banco próprio, JVM própria, em sequência). Não junte as execuções `cucumber` e `cucumber-it` às outras no `pom.xml`, não ligue execução paralela e não use a cota `COTA-SENTINELA`.
4. Não edite migrations existentes. Mudança de schema é sempre um arquivo novo `V<n+1>__descricao.sql`. A massa da aceitação (`src/test/resources/cucumber/massa/processos.md`) muda por linha nova, com uma cota nova; os cenários a citam pela cota, nunca por número.

## Proibido

- Panache, Lombok, MapStruct, SQL nativo, herança JPA.
- Regra de negócio fora do domínio. Domínio é Java puro, sem JPA, Jakarta Data, CDI, JAX-RS, Bean Validation, Quarkus, Hibernate ou MicroProfile.
- `if (tipo == ...)` no motor: o que é de um tipo fica no `Fluxo<Tipo>`.
- `*Entity` com qualquer método além de getter e setter. Use case importar JPA, Jakarta Data ou `*Entity`.
- Bean Validation fora do entrypoint.
- Desligar, apagar ou afrouxar teste para o build passar (`@Disabled`, `skipTests`, esperado trocado pelo valor atual). Divergência entre teste e código é defeito a relatar.
- Mudar o resultado esperado de um cenário de uma especificação (`.feature`) sem a confirmação do negócio, ou escrever nela código da API (estado em maiúsculas, URL, status HTTP).
- `@SuppressWarnings("NullAway")` ou mudar a configuração do NullAway, do ErrorProne, do Enforcer ou o `pitest.limite` para o build passar.
- Atualizar `docs/api/openapi-base.json` sem que a mudança da API seja aditiva.
