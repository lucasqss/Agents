# Massa com IDs fixos

## Como funciona

`BancoDaAceitacao` (um `QuarkusTestResourceLifecycleManager`, em `assets/java/`) roda na JVM do teste, **antes** de a aplicação subir:

1. sobe um PostgreSQL novo (Testcontainers);
2. aplica `classpath:db/migration` com o histórico padrão do Flyway, o mesmo que a aplicação usa. Ao subir, ela confere e não tem nada a migrar;
3. aplica `classpath:db/massa` com um histórico próprio (`flyway_historico_massa`). A aplicação não conhece essa migration e não estranha;
4. devolve só `quarkus.datasource.jdbc.url`, `username` e `password`.

A massa é uma migration **repeatable** (`R__`): roda depois de todas as `V*`. Migration nova de produção não obriga a renumerar nada.

```sql
insert into processo_pagamento (id, tipo, numero_cota, valor, estado) values
    (1, 'SERVICO', 'COTA-001', 30000.00, 'ABERTO'),
    (2, 'SERVICO', 'COTA-002', 30000.00, 'EM_ANALISE');
-- Tudo o que os cenários criarem começa em 1000, na ordem em que os cenários rodam.
alter table processo_pagamento alter column id restart with 1000;
alter table processo_transicao alter column id restart with 1000;
```

Toda tabela com `identity` (ou sequência) que os cenários alimentam ganha o seu `restart with 1000`. Tabela nova esquecida aqui não quebra o teste: os IDs dela começam em 1 e continuam reprodutíveis, mas podem colidir com uma massa futura.

## Por que não outra pasta no Flyway da aplicação

- `quarkus.flyway.locations` é fixado no build: o jar não aceita outra pasta só no teste.
- O jar do `*IT` não leva o classpath de teste.
- `migrate-at-start=false` devolvido pelo recurso **vaza** para os jars dos `*IT` seguintes (que sobem com o banco do Dev Services, sem tabelas, e respondem 500). Por isso o recurso não muda nada além do endereço do banco.

## Sandbox: o que garante que não conflita com os outros testes

1. **Banco próprio.** O `@WithTestResource` vale só para a classe anotada: o Quarkus reinicia a aplicação para ela, com o banco novo, e volta ao Dev Services depois.
2. **JVM própria.** No `*IT`, a configuração que um recurso de teste devolve vale para **todos os jars que sobem depois na mesma JVM** do failsafe. Por isso a aceitação tem execuções próprias no `pom.xml` (`aceitacao` no surefire, `aceitacao-it` no failsafe), depois das demais. Cada execução é uma JVM.
3. **Sentinela.** O processo 1 da massa (`COTA-SENTINELA`) não é usado por cenário nenhum (`Cenarios.validar(..., [1])` barra). O `@setup` confere que ele está lá antes do primeiro cenário: aplicação sobre o banco errado para ali, com a mensagem certa.
4. **Em sequência.** `Karate.run` roda na ordem do arquivo, e o `AceitacaoSequencialTest` falha se `junit.jupiter.execution.parallel.enabled` for ligado.

Para conferir: `./mvnw verify -Dsurefire.runOrder=reversealphabetical -Dfailsafe.runOrder=reversealphabetical` precisa dar os mesmos IDs.

## Por que um banco novo e não `clean-at-start`

Banco novo não depende do que outro teste fez e vale igual no `*Test` e no `*IT`. O `@WithTestResource` também isola a classe de aceitação: ela ganha aplicação e banco próprios, e os testes de componente (que usam o banco do Dev Services) não consomem IDs dela.

## Armadilhas

| Sintoma | Causa |
|---|---|
| Cenário passa sozinho e falha no build | Dois cenários sobre o mesmo registro da massa. `Cenarios.validar` barra; se escapou, crie outro registro |
| `espera.id` errado depois de acrescentar um cenário | O cenário novo cria registro antes; confira o ID só onde ele é o ponto do cenário |
| `duplicate key` ao criar | Faltou o `restart with 1000` da tabela, e a massa usou um ID que a sequência também gera |
| `*IT` de outra classe responde 500 depois da aceitação | A aceitação rodou na mesma JVM do failsafe que os outros `*IT` e a configuração do recurso vazou (ex.: Flyway desligado). Mantenha a execução `aceitacao-it` separada |
| Karate para no `@setup`: "não está sobre o banco da aceitação" | O runner perdeu o `@WithTestResource(BancoDaAceitacao.class)` (ou o `*IT` não herda do `*Test`) |
| Massa nova não aparece | O arquivo não começa com `R__` (ou `V`) e o Flyway o ignora |
