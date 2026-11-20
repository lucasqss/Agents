---
name: teste-aceitacao
description: Use ao criar, alterar ou revisar testes de aceitação com cenários de negócio em YAML, executados pelo Karate contra os endpoints HTTP no par *Test (@QuarkusTest) e *IT (@QuarkusIntegrationTest), sobre um banco novo a cada execução com massa de IDs fixos (reprodutível em todo build). Serve para transformar os cenários que o analista de negócio define em testes de build. Não use para regra exaustiva (teste-unidade) nem para erro técnico ou integração do serviço montado (teste-componente).
---

# Teste de aceitação por cenários de negócio

Cenários de negócio, de ponta a ponta, pelos endpoints HTTP. O analista define o cenário; o dev o escreve como **um item em `cenarios.yaml`**. O Karate lê o arquivo e executa: cenário novo é um item novo, sem código novo. O analista revisa no pull request, onde o diff mostra só o cenário que mudou.

**Orçamento: 1 a 3 cenários por regra** (o caminho feliz, a recusa principal, o efeito conferido). A matriz exaustiva é da `teste-unidade`.

## IDs reprodutíveis: a massa SQL

Cada execução (no `*Test` e no `*IT`) sobe um **PostgreSQL novo** (`BancoDaAceitacao`), aplica as migrations de produção e, por cima, `src/test/resources/db/massa/R__massa_aceitacao.sql`. A massa grava os registros com **IDs fixos** e reinicia as sequências em **1000**. Por isso, em todo build:

- o cenário cita o registro da massa pelo ID (`processo: 3`) e começa direto na situação que precisa, sem percorrer o fluxo;
- o que os cenários criam ganha 1000, 1001... **na ordem do arquivo**, e o cenário pode conferir o ID (`espera: { id: 1000 }`).

Detalhes e armadilhas: [IDs fixos](references/ids-fixos.md).

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md): quais cenários o analista definiu, de que situação cada um parte, o que já está coberto nos outros níveis.
2. **Monte o plano** no formato abaixo.
3. **Pare e aguarde a confirmação do dev.**
4. Para cada cenário que parte de uma situação pronta, **uma linha nova na massa SQL** com o próximo ID livre (abaixo de 1000). Depois, o item em `cenarios.yaml`. Ação nova vira uma cena nova em `passos.feature` ([Karate](references/karate.md)).
5. Rode `./mvnw verify` (o `*Test` e o `*IT` rodam no mesmo comando).
6. Faça uma [sessão exploratória](references/exploratorios.md) por capacidade nova.
7. Relate cenários, divergências e dúvidas.

## Formato do plano

1. **Cenários:** `cenario`, `regra`, `descricao`, `tipo` (positivo ou negativo) e os passos, em linguagem de negócio.
2. **Massa:** de qual registro (ID e situação) cada cenário parte, e as linhas novas do SQL.
3. **Fica para outro nível:** variações exaustivas (unidade), erros técnicos e integração (componente).
4. **Dúvidas:** com a pergunta exata. Nenhuma dúvida vira resultado esperado.

## Estrutura

```
src/test/resources/
  karate-config.js                     URL da aplicação em teste (do runner)
  db/massa/R__massa_aceitacao.sql      massa com IDs fixos; sequências reiniciadas em 1000
  aceitacao/<capacidade>/
    <capacidade>.feature               lê e valida os cenários; um teste por cenário
    passos.feature                     uma cena por ação (@abrir, @executar...): o passo vira requisição
    cenarios.yaml                      um item por cenário, com a lista de passos
src/test/java/<pacote raiz>/aceitacao/
  BancoDaAceitacao.java, Cenarios.java genéricos (assets/java)
  <Capacidade>AceitacaoTest.java       runner Karate, @QuarkusTest @WithTestResource(BancoDaAceitacao.class)
  <Capacidade>AceitacaoIT.java         herda vazio, @QuarkusIntegrationTest
```

Modelo completo neste projeto: `aceitacao/processo/` e `ProcessoAceitacaoTest`. Sistema externo simulado por WireMock a partir de massa YAML: `assets/exemplo-pedidos/` ([dados](references/dados.md)).

## Regras

1. **Só HTTP.** Nada de `@Inject` nem import de classe de produção. A massa entra pelo SQL; o resto, pelos endpoints.
2. **Um registro da massa por cenário.** Dois cenários sobre o mesmo ID ficam dependentes da ordem. `Cenarios.validar` barra antes de rodar.
3. **ID da massa abaixo de 1000; ID gerado só se confere quando é o ponto do cenário.** Cenário novo que cria registro antes de outro muda o número dos seguintes.
4. **Confira o efeito por outro endpoint** depois de cada passo (a situação gravada, as ações disponíveis, o histórico), não só o status.
5. **O esperado vem do analista, via dev**, não do código. Divergência é defeito: o cenário fica vermelho e é relatado. Não mude o esperado para passar.
6. **Todo `*Test` tem o `*IT`** de corpo vazio, que herda. Os dois rodam os mesmos cenários sobre um banco novo, então esperam os mesmos IDs.
7. **O recurso de teste só entrega o endereço do banco.** No `*IT`, a configuração que ele devolve vale também para os jars dos outros `*IT`: nada de desligar Flyway ou mudar comportamento por ali.

## Antes de terminar

1. `./mvnw verify` verde; `target/surefire-reports` e `target/failsafe-reports` com o mesmo número de cenários.
2. Relatório do Karate em `target/karate-reports/` (cada passo, requisição e resposta).
3. Nenhum arquivo em `aceitacao/` importa classe de produção.
4. Relate defeitos, dúvidas abertas e o resultado da sessão exploratória.
