# Massa, chave natural e IDs reprodutíveis

## Como funciona

`BancoDaAceitacao` (um `QuarkusTestResourceLifecycleManager`) roda na JVM do teste, **antes** de a aplicação subir:

1. sobe um PostgreSQL novo (Testcontainers);
2. aplica `classpath:db/migration` com o histórico padrão do Flyway, o mesmo que a aplicação usa. Ao subir, ela confere e não tem nada a migrar;
3. chama `MassaDeProcessos.carregar`, que:
   - lê e valida o arquivo inteiro `cucumber/massa/processos.md` (`TabelaMarkdown`) e para, com a linha, se algo está errado;
   - grava a **sentinela** (`COTA-SENTINELA`, id 1) e as linhas do arquivo com id 2, 3... na ordem do arquivo;
   - faz as sequências recomeçarem em **1000**: o que os cenários criam ganha 1000, 1001... na ordem dos cenários;
4. devolve só `quarkus.datasource.jdbc.url`, `username` e `password`.

O cenário cita a **cota**. `MassaDeProcessos.idDaCota` relê o mesmo arquivo e calcula o mesmo id: carregador e passos não dividem estado (o recurso de teste e o Quarkus podem estar em classloaders diferentes). Linha nova em qualquer posição muda os ids, e nenhum cenário percebe.

Toda tabela com `identity` (ou sequência) que os cenários alimentam ganha o seu `restart with 1000` no carregador. Massa de outra entidade é outro arquivo `.md` e outro carregador, no mesmo formato.

## Por que não outra pasta no Flyway da aplicação

- `quarkus.flyway.locations` é fixado no build: o jar não aceita outra pasta só no teste.
- O jar do `*IT` não leva o classpath de teste.
- `migrate-at-start=false` devolvido pelo recurso **vaza** para os jars dos `*IT` seguintes (que sobem com o banco do Dev Services, sem tabelas, e respondem 500). Por isso o recurso não muda nada além do endereço do banco.

## Sandbox: o que garante que não conflita com os outros testes

1. **Banco próprio.** O `@WithTestResource` vale só para a classe anotada: o Quarkus reinicia a aplicação para ela, com o banco novo, e volta ao Dev Services depois.
2. **JVM própria.** No `*IT`, a configuração que um recurso de teste devolve vale para **todos os jars que sobem depois na mesma JVM** do failsafe. Por isso a aceitação tem execuções próprias no `pom.xml` (`cucumber` no surefire, `cucumber-it` no failsafe), depois das demais. Cada execução é uma JVM.
3. **Sentinela.** O carregador grava `COTA-SENTINELA` (id 1), que não pode estar no arquivo do QA. O runner confere que ela está lá antes do primeiro cenário: aplicação sobre o banco errado para ali, com a mensagem certa.
4. **Em sequência.** O runner chama o Cucumber com `--order lexical` (arquivos em ordem alfabética, cenários na ordem do arquivo, um de cada vez), e o `AceitacaoSequencialTest` falha se `junit.jupiter.execution.parallel.enabled` for ligado.

Para conferir: `./mvnw verify -Dsurefire.runOrder=reversealphabetical -Dfailsafe.runOrder=reversealphabetical` precisa dar os mesmos números.

## Por que um banco novo e não `clean-at-start`

Banco novo não depende do que outro teste fez e vale igual no `*Test` e no `*IT`. O `@WithTestResource` também isola a classe de aceitação: ela ganha aplicação e banco próprios, e os testes de componente (que usam o banco do Dev Services) não consomem IDs dela.

## Armadilhas

| Sintoma | Causa |
|---|---|
| `Massa inválida: processos.md, linha N: ...` antes de qualquer cenário | O arquivo da massa tem termo, valor ou cota inválidos. A mensagem lista todas as linhas com erro |
| Cenário passa sozinho e falha no build | Dois cenários sobre o mesmo processo. O passo `Dado o processo da cota` barra; crie outra linha na massa |
| Número do processo aberto errado depois de acrescentar um cenário | O cenário novo cria registro antes; confira o número só onde ele é o ponto do cenário |
| `duplicate key` ao criar | Faltou o `restart with 1000` de uma tabela no carregador |
| `*IT` de outra classe responde 500 depois da aceitação | A aceitação rodou na mesma JVM do failsafe que os outros `*IT` e a configuração do recurso vazou. Mantenha a execução `cucumber-it` separada |
| A aceitação para antes do primeiro cenário: "não está sobre o banco da aceitação" | O runner perdeu o `@WithTestResource(BancoDaAceitacao.class)` (ou o `*IT` não herda do `*Test`) |
