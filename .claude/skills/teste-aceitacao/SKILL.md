---
name: teste-aceitacao
description: Use ao criar, alterar ou revisar testes de aceitação no modelo de tabelas que o QA preenche - uma tabela de massa (os cadastros, pela chave natural, em Markdown) e tabelas de cenários em português (Gherkin, # language: pt, Esquema do Cenário com Exemplos, uma por família) - executadas pelo Cucumber contra os endpoints HTTP no par *Test (@QuarkusTest) e *IT (@QuarkusIntegrationTest), sobre um banco novo a cada execução. Serve para transformar as regras que o analista define em especificações executáveis no build. Não use para regra exaustiva (teste-unidade) nem para erro técnico ou integração do serviço montado (teste-componente).
---

# Teste de aceitação: massa e cenários em tabelas

O modelo de planilhas (uma de massa, outras de cenários), só que em **texto versionado no git**:

- **Massa:** `cucumber/massa/processos.md`, uma tabela Markdown com os cadastros que já existem quando os cenários começam. A chave é natural (a cota); nenhum cenário cita um ID.
- **Cenários:** um `.feature` por família ("aba"), com um `Esquema do Cenário` e a tabela `Exemplos`. Cada linha da tabela é um cenário, e o QA preenche linhas sem escrever passo nem SQL.
- **Fluxos de vários passos:** um `Cenário` com a tabela de passos (`Quando os passos abaixo são executados em ordem:`).

```gherkin
# language: pt
Funcionalidade: Ações sobre o processo (R2 a R7)

  Esquema do Cenário: <cenário> <descrição>
    Dado o processo da cota "<cota>" da massa
    Quando "<quem>" pedir para "<ação>" com o motivo "<motivo>"
    Então a ação é "<resultado>" e o processo fica "<situação>"
    E a mensagem é "<mensagem>"
    E as ações disponíveis são "<ações disponíveis>"

    Exemplos:
      | cenário | regra | descrição                       | cota     | quem | ação               | motivo | resultado | situação          | mensagem | ações disponíveis |
      | 4.1     | R4    | Valor igual à alçada aprova     | COTA-005 | ana  | concluir a análise |        | aceita    | Aprovado          |          |                   |
      | 4.2     | R4    | Um centavo acima aguarda alçada | COTA-006 | ana  | concluir a análise |        | aceita    | Aguardando alçada |          |                   |
```

**Célula vazia quer dizer "não conferir".** A exceção é `motivo`: vazio manda a ação sem motivo.

**Orçamento: 1 a 3 cenários por regra** (o caminho feliz, a recusa principal, o efeito conferido). A matriz exaustiva é da `teste-unidade`: uma "aba" de mil linhas é sinal de que a regra está no nível errado.

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md): quais regras e cenários o analista definiu, de que situação cada um parte, o que já está coberto nos outros níveis.
2. **Preencha as tabelas primeiro** ([como preencher](references/escrita.md)): uma linha na massa para cada situação de partida, uma linha em `Exemplos` por cenário. **Leve ao analista antes de implementar**: o esperado é dele.
3. **Pare e aguarde a confirmação do dev** (e, por ele, do negócio).
4. Coluna ou frase nova só se as que existem não derem conta: um método em `cucumber/passos/` e o termo novo em `Vocabulario`.
5. Rode `./mvnw verify` e, se o projeto deixa os `*IT` para a geração de versão, `./mvnw verify -Pversao` (no `exemplo-testes`, o `*Test` e o `*IT` rodam no próprio `verify`).
6. Faça uma [sessão exploratória](references/exploratorios.md) por capacidade nova.
7. Relate cenários, divergências e dúvidas.

## Estrutura

Tudo da aceitação fica numa pasta `cucumber/` própria, na raiz de `src/test/java` e de `src/test/resources`, separada dos pacotes dos outros testes. O `pom.xml` a separa pelo caminho: `cucumber/**` fica fora da execução padrão e roda nas execuções `cucumber` (surefire) e `cucumber-it` (failsafe).

```
src/test/resources/cucumber/
  massa/processos.md                    a tabela de massa (o QA preenche)
  features/01-abertura.feature          uma "aba" por arquivo; o prefixo fixa a ordem de execução
  features/02-acoes.feature
  features/03-fluxos.feature
src/test/java/cucumber/
  ProcessoAceitacaoTest.java            runner: @QuarkusTest + @WithTestResource(BancoDaAceitacao), um teste JUnit por cenário
  ProcessoAceitacaoIT.java              herda vazio, @QuarkusIntegrationTest
  BancoDaAceitacao.java                 banco novo, migrations e a massa
  Resultados.java, AceitacaoSequencialTest.java
  massa/TabelaMarkdown.java             lê qualquer tabela Markdown (genérico)
  massa/MassaDeProcessos.java           valida e grava a massa; cota -> id
  passos/Vocabulario.java               termos do negócio <-> códigos da API
  passos/PassosDoProcesso.java          as frases que leem cada linha das tabelas
```

Os genéricos estão em [`assets/java/`](assets/java/). Modelo completo, verificado com Java 25 e Quarkus 3.33: o projeto [`exemplo-testes`](../../../exemplo-testes/) deste repositório (runner, massa, vocabulário, passos e tabelas). Relatórios: `target/cucumber/<Runner>.html` (cada cenário, cada passo) e os relatórios do surefire/failsafe, com o nome do cenário em cada teste.

## Regras

1. **Palavras do negócio, não da API.** Nada de código de estado (`EM_ANALISE`), URL, status HTTP ou JSON nas tabelas. Termo novo entra no `Vocabulario`; termo escrito errado falha listando os válidos.
2. **Só HTTP nos passos.** Nada de `@Inject` nem import de classe de produção. A massa entra pelo `BancoDaAceitacao`; o resto, pelos endpoints. Com identidade (JWT), `quem` é uma pessoa com papel (uma coluna `papel` ao lado de `quem`) e o passo monta o token do papel, como o `IdentidadeTeste` dos testes de componente.
3. **A massa é citada pela chave natural.** O ID é determinístico (sentinela = 1, depois a ordem do arquivo), mas nenhum cenário o cita: linha nova em qualquer posição da massa não muda nenhum cenário.
4. **Uma cota por cenário** (cada linha de `Exemplos` conta como um). O passo `Dado o processo da cota` barra a cota usada por outro cenário e confere que o processo está como a massa diz.
5. **A massa é validada inteira antes de a aplicação subir:** coluna faltando, cota vazia, repetida, longa ou reservada, termo e valor inválidos. A falha diz `processos.md, linha N`.
6. **Confira o efeito por outro endpoint** depois de cada ação (a situação gravada, as ações disponíveis, o histórico), não só o status.
7. **O esperado vem do analista**, não do código. Divergência é defeito: o cenário fica vermelho e é relatado. Não mude o esperado para passar.
8. **Todo `*Test` tem o `*IT`** de corpo vazio, que herda: o mesmo número de cenários nas duas fases, com os mesmos números gerados.
9. **Sandbox garantido por construção:**
   - execução própria no surefire (`cucumber`) e no failsafe (`cucumber-it`), numa JVM só dela;
   - em sequência (`--order lexical`, sem paralelo; o `AceitacaoSequencialTest` barra o paralelo do JUnit);
   - começa pela sentinela (`COTA-SENTINELA`, gravada pelo carregador), conferida pelo runner antes do primeiro cenário.
10. **O recurso de teste só entrega o endereço do banco**: nada de desligar Flyway ou mudar comportamento por ali.

## Antes de terminar

1. `./mvnw verify` verde; o mesmo número de cenários no `*Test` e no `*IT`.
2. Nenhum arquivo em `src/test/java/cucumber/` importa classe de produção; nenhuma tabela traz código da API.
3. O analista viu as linhas novas (diga isso no relato, ou que ainda falta).
4. Relate defeitos, dúvidas abertas e o resultado da sessão exploratória.

Por que tabelas em texto e não planilhas, e por que Cucumber e não Karate: [plano B](references/plano-b.md) e a ADR 001 do `exemplo-testes`.
