# Método do `@ArchIgnore`

Todas as regras começam desligadas. Cada camada liberada quebra o build, e o agente corrige até voltar ao verde.

## Pré-requisitos

- Árvore git limpa. Senão o primeiro commit mistura trabalho alheio.
- [Diagnóstico](diagnostico.md) aprovado pelo dono.
- Testes de caracterização nos pontos críticos. O ArchUnit vê estrutura, não comportamento: sem esses testes, o build verde não prova que nada mudou.

## Baseline

1. Copie `src/test/java/**/arquitetura/` do projeto modelo para `src/test/java/<pacote raiz>/arquitetura/` e ajuste a linha `package` de cada arquivo. Inclua `archunit-junit5` no `pom.xml`, como no modelo.
2. Marque **cada regra**, no campo, com a tag da camada dela:

```java
@ArchIgnore(reason = "adequacao:dominio")
@ArchTest
static final ArchRule dom001Framework = ...
```

3. Use o campo, e não o grupo `ArchTests.in` em `ArquiteturaTest`: uma mesma classe `Regras*` tem regras de camadas diferentes, e a anotação no campo é a forma documentada pelo ArchUnit.
4. Rode `./mvnw verify`: deve ficar verde, com todas as regras como ignoradas no relatório. Faça o commit `adequacao(baseline): desliga as regras de arquitetura`.

## Ciclo de cada camada

Sempre nesta ordem:

| # | Tag | Regras | O que costuma exigir |
|---|---|---|---|
| 1 | `estrutura` | GER-001 | [Mover toda classe para uma pasta conhecida](layout-para-modulos.md) |
| 2 | `dominio` | DOM | [Separar JPA de domínio](entidade-jpa-e-dominio.md) |
| 3 | `persistencia` | SHR, TEC | [Panache → Jakarta Data](panache-para-jakarta-data.md), [tirar proibidos](remover-proibidos.md) |
| 4 | `application` | CMD, QRY | [Quebrar god class](service-god-class.md), [isolar client](client-solto-para-gateway.md) |
| 5 | `entrypoint` | CAN | [Request/Response e Assembler](entidade-na-api.md) |
| 6 | `nomes` | NOM | Renomear classes |
| 7 | `ciclos` | GER-002 | Inverter dependências com interface ou mover a classe |

Para cada camada:

1. Remova os `@ArchIgnore` da tag (`grep -rn 'adequacao:<tag>'`).
2. Rode `./mvnw verify`. As falhas formam a lista de trabalho; cada uma traz o ID e o "Corrija:".
3. Corrija o código seguindo a receita.
4. Com o build verde, faça **um commit**: `adequacao(<camada>): <o que mudou>`.

## Regras do método

- **A camada `estrutura` vem sempre primeiro.** O `Raiz` liga `failOnEmptyShould=false`, então, com as pastas erradas, as demais regras passam sem validar nada. A GER-001 é a única que recusa isso (`allowEmptyShould(false)`) e exige que toda classe esteja numa pasta do vocabulário.
- **Build verde depois de liberar não significa que estava certo.** Se liberar uma camada não gerar nenhuma falha, confirme que as classes daquela camada estão no pacote que a regra analisa.
- `@ArchIgnore` só sai. Depois do baseline, nunca adicione nem mude a tag de uma regra.
- Nunca libere a próxima camada com a atual vermelha.
- Nunca altere regra, `allowEmptyShould` ou `Raiz` para passar.
- Se corrigir uma camada exigir quebrar outra já liberada, pare e pergunte ao dono.
- A adequação termina quando `grep -r ArchIgnore src/test` não acha nada. Remova então o import que sobrar.

**Custo aceito:** até uma camada ser liberada, nada impede violação nova nela. Não deixe a adequação aberta por muito tempo.
