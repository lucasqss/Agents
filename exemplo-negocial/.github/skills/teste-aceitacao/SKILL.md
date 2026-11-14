---
name: teste-aceitacao
description: Use ao criar, alterar ou revisar testes de aceitação de usuário guiados por tabela - massa de dados (cadastro e sistemas externos) e cenários de negócio em CSV, executados pelo Karate contra os endpoints HTTP, no par *Test (@QuarkusTest) e *IT (@QuarkusIntegrationTest). Serve para transformar os cenários que o analista de negócio define em testes de build, e para proteger os fluxos principais de um sistema existente. Não use para regra exaustiva (teste-unidade) nem para erro técnico, segurança ou integração do serviço montado (teste-componente).
---

# Teste de aceitação por tabela

Cenários de negócio, de ponta a ponta, pelos endpoints HTTP. O analista define o cenário; o dev o escreve como **linha de tabela**. A tabela é CSV no repositório: o GitHub a mostra como tabela (o analista revisa no navegador) e ela abre no Excel. O Karate lê as tabelas e executa: cenário novo é linha nova, sem código novo.

**Orçamento: 1 a 3 cenários por regra** (o caminho feliz, a recusa principal, e o efeito conferido). A matriz exaustiva (cada ação inválida em cada estado, cada limite) é da `teste-unidade`, em milissegundos. Repetir aqui o que a unidade já prova só deixa o build lento.

Os exemplos usam `pedidos`, `cliente` e `antifraude` como ilustração: não procure essas classes no repositório.

## Passo a passo

1. **Entreviste o dev** seguindo o [roteiro](references/entrevista.md): quais cenários o analista definiu, que massa eles usam, quais sistemas externos aparecem, o que já está coberto nos outros níveis. Se existir um `caso-de-uso.md`, use-o como ponto de partida; ele não é obrigatório.
2. **Monte o plano** no formato abaixo.
3. **Pare e aguarde a confirmação do dev.**
4. Escreva as tabelas e, se for a primeira capacidade, a feature e o runner ([Karate](references/karate.md)).
5. Rode `./mvnw verify` (o `*Test`) e `./mvnw verify -Pversao` (o `*IT`, contra o artefato empacotado).
6. Faça uma [sessão exploratória](references/exploratorios.md) por capacidade nova.
7. Relate cenários, divergências e dúvidas.

## Formato do plano

1. **Cenários:** tabela com `cenario`, `regra`, `descricao`, `tipo` (positivo ou negativo) e os passos, em linguagem de negócio.
2. **Massa:** os registros de cadastro e de cada sistema externo que os cenários usam, por chave.
3. **Fica para outro nível:** as variações exaustivas (unidade) e os erros técnicos, segurança e integração (componente).
4. **Dúvidas:** com a pergunta exata. Nenhuma dúvida vira resultado esperado.

## Estrutura

```
src/test/resources/
  karate-config.js                         URL da aplicação em teste (do runner)
  aceitacao/<capacidade>/
    <capacidade>.feature                   lê, valida e agrupa as tabelas; um teste por cenário
    passos.feature                         uma cena por ação (@criar, @cancelar...): a linha vira requisição
    cenarios.csv                           uma linha por passo
    massa/<entidade>.csv                   cadastro, criado pelos endpoints do próprio serviço
    massa/externa-<sistema>.csv            o que cada sistema externo responde
src/test/java/<pacote raiz>/aceitacao/
  Tabelas.java, Massa.java                 genéricos, copiados dos assets
  <Capacidade>AceitacaoTest.java           runner Karate, @QuarkusTest
  <Capacidade>AceitacaoIT.java             herda vazio, @QuarkusIntegrationTest
  <Sistema>Simulado.java                   WireMock alimentado por massa/externa-<sistema>.csv
```

Tudo isso existe pronto em [`assets/`](assets/), verificado com Java 25 e Quarkus 3.33: os genéricos em `assets/java/` e uma capacidade completa de exemplo em `assets/exemplo-pedidos/`. Copie e troque `PACOTE_RAIZ`. Colunas e convenções das tabelas: [tabelas](references/tabelas.md).

## Regras

1. **Só HTTP.** Nada de `@Inject`, banco ou import de classe de produção. A massa de cadastro é criada pelos endpoints.
2. **Dados próprios.** Cada cenário cria a sua massa de cadastro; nunca depende de outro cenário nem de banco limpo.
3. **Confira o efeito por outro endpoint** depois de cada passo (a situação gravada), não só o status.
4. **Chave de massa inexistente falha antes de rodar**, dizendo a linha do CSV (`Tabelas.agrupar`).
5. **O esperado vem do analista, via dev**, não do código. Divergência é defeito: o cenário fica vermelho e é relatado. Não mude o esperado para passar. Se o negócio decidir conviver com o defeito, as linhas do cenário saem para `cenarios-defeitos.csv` (fora do `Scenario Outline`), com o motivo em `observacao`, e voltam quando a produção for corrigida.
6. **Todo `*Test` tem o `*IT`** de corpo vazio, que herda. Os dois rodam o mesmo número de cenários.
7. **Sistema externo é simulado** por WireMock a partir da massa. O mock server do próprio Karate não funciona com o Quarkus 3.33 (conflito de versão do Netty).

## Antes de terminar

1. `./mvnw verify -Pversao` verde; `target/surefire-reports` e `target/failsafe-reports` com o mesmo número de cenários.
2. Relatório do Karate em `target/karate-reports/` (um HTML por capacidade, com cada passo e requisição).
3. Nenhum arquivo em `aceitacao/` importa classe de produção.
4. Relate defeitos, dúvidas abertas e o resultado da sessão exploratória.

## Sistema existente, sem testes

Antes de mexer num sistema existente, proteja os fluxos principais: gere a linha de base do contrato REST (skill `adequar-ao-padrao`), entreviste o dev sobre os fluxos que mais importam ao negócio e escreva os cenários deles. O esperado de cada cenário é **confirmado com o dev** (e por ele com o negócio), não copiado da resposta atual: comportamento observado não é, sozinho, comportamento correto.

Se um dia os cenários precisarem de mensageria ou o Karate deixar de servir, troque o motor e mantenha as tabelas: [plano B](references/plano-b.md).
