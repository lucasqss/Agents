---
name: teste-aceitacao
description: Use ao criar, alterar ou revisar testes de aceitação escritos como especificações em português (Gherkin, # language: pt) que o negócio lê e valida, executadas pelo Cucumber contra os endpoints HTTP no par *Test (@QuarkusTest) e *IT (@QuarkusIntegrationTest), sobre um banco novo a cada execução com massa de IDs fixos. Serve para transformar as regras que o analista define em especificações executáveis no build. Não use para regra exaustiva (teste-unidade) nem para erro técnico ou integração do serviço montado (teste-componente).
---

# Teste de aceitação: especificações em português

A especificação **é** o teste. O analista lê e valida o `.feature` (em português, nas palavras do negócio); o build o executa contra a API. Um arquivo por regra (ou grupo de regras), numerado para fixar a ordem: `01-abertura.feature`, `04-alcada.feature`.

```gherkin
# language: pt
@R4
Funcionalidade: Alçada de aprovação
  Até a alçada (R$ 50.000,00), inclusive, o processo é aprovado direto.

  Esquema do Cenário: <cenário> Concluir a análise de um serviço de <valor> leva a "<situação>"
    Dado o processo <processo> da massa, de serviço, na situação "Em análise", com valor de <valor>
    Quando "ana" concluir a análise
    Então o processo fica "<situação>"

    Exemplos:
      | cenário | processo | valor     | situação          |
      | 4.1     | 5        | 50.000,00 | Aprovado          |
      | 4.2     | 6        | 50.000,01 | Aguardando alçada |
```

**Orçamento: 1 a 3 cenários por regra** (o caminho feliz, a recusa principal, o efeito conferido). A matriz exaustiva é da `teste-unidade`. Várias combinações da mesma regra cabem num `Esquema do Cenário` com `Exemplos`, que o negócio lê como tabela.

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md): quais regras e cenários o analista definiu, de que situação cada um parte, o que já está coberto nos outros níveis.
2. **Escreva a especificação primeiro**, só com frases do vocabulário ([como escrever](references/escrita.md)), e **leve ao analista antes de implementar**: o esperado é dele.
3. **Pare e aguarde a confirmação do dev** (e, por ele, do negócio).
4. Para cada cenário que parte de uma situação pronta, **uma linha nova na massa SQL** com o próximo ID livre ([IDs fixos](references/ids-fixos.md)).
5. Frase nova só se o vocabulário não der conta: um método em `aceitacao/passos/`, e o termo novo em `Vocabulario`.
6. Rode `./mvnw verify` e, se o projeto deixa os `*IT` para a geração de versão, `./mvnw verify -Pversao`.
7. Faça uma [sessão exploratória](references/exploratorios.md) por capacidade nova.
8. Relate cenários, divergências e dúvidas.

## Estrutura

```
src/test/resources/
  db/massa/R__massa_aceitacao.sql       massa com IDs fixos; o 1 é a sentinela; sequências reiniciadas em 1000
  aceitacao/NN-<regra>.feature          as especificações, em português, numeradas (ordem de execução)
src/test/java/<pacote raiz>/aceitacao/
  ProcessoAceitacaoTest.java            runner: @QuarkusTest + @WithTestResource(BancoDaAceitacao), um teste JUnit por cenário
  ProcessoAceitacaoIT.java              herda vazio, @QuarkusIntegrationTest
  BancoDaAceitacao.java, Resultados.java, AceitacaoSequencialTest.java   genéricos (assets/java)
  passos/Vocabulario.java               termos do negócio <-> códigos da API (situação, ação, tipo, valor)
  passos/PassosDoProcesso.java          as frases (Dado/Quando/Então) em Java com RestAssured
```

Modelo completo, verificado com Java 25 e Quarkus 3.33: o projeto [`exemplo-testes`](../../../../exemplo-testes/) deste repositório (runner, vocabulário, passos, especificações e massa). Relatórios: `target/cucumber/<Runner>.html` (cada cenário, cada passo) e os relatórios do surefire/failsafe, com o nome do cenário em cada teste.

## Regras

1. **Palavras do negócio, não da API.** Nada de código de estado (`EM_ANALISE`), URL, status HTTP ou JSON na especificação. Termo novo entra no `Vocabulario`; termo escrito errado falha listando os válidos.
2. **Só HTTP nos passos.** Nada de `@Inject` nem import de classe de produção. A massa entra pelo SQL; o resto, pelos endpoints. Com identidade (JWT), quem age na especificação é uma pessoa com papel (`Quando a aprovadora "carla" aprovar na alçada`) e o passo monta o token do papel, como o `IdentidadeTeste` dos testes de componente.
3. **Um registro da massa por cenário** (cada linha de `Exemplos` é um cenário). O passo `Dado o processo N da massa` barra o uso repetido, a sentinela e ID que não é da massa.
4. **O `Dado` confere a massa** (tipo, situação, valor): se o SQL mudar, o cenário diz que a pré-condição não vale mais, em vez de falhar adiante sem explicação.
5. **Confira o efeito por outro endpoint** depois de cada ação (`o processo fica`, `as ações disponíveis são`, `o histórico`), não só que a ação foi aceita.
6. **O esperado vem do analista**, não do código. Divergência é defeito: o cenário fica vermelho e é relatado. Não mude o esperado para passar.
7. **Todo `*Test` tem o `*IT`** de corpo vazio, que herda: o mesmo número de cenários nas duas fases, com os mesmos IDs.
8. **Sandbox garantido por construção:** execução própria no surefire (`aceitacao`) e no failsafe (`aceitacao-it`), numa JVM só dela; em sequência (`--order lexical`, sem paralelo; o `AceitacaoSequencialTest` barra o paralelo do JUnit); começa pela sentinela, conferida pelo runner antes do primeiro cenário.
9. **O recurso de teste só entrega o endereço do banco**: nada de desligar Flyway ou mudar comportamento por ali.

## Antes de terminar

1. `./mvnw verify` verde; o mesmo número de cenários no `*Test` e no `*IT`.
2. Nenhum arquivo em `aceitacao/` importa classe de produção; nenhuma especificação traz código da API.
3. O analista viu as especificações novas (diga isso no relato, ou que ainda falta).
4. Relate defeitos, dúvidas abertas e o resultado da sessão exploratória.

Por que Cucumber e não Karate, e como trocar o motor sem perder as especificações: [plano B](references/plano-b.md).
