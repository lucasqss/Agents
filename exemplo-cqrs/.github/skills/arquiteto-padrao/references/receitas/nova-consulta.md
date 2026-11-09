# Receita: nova consulta de leitura

Entrada necessária: o que a tela precisa ver (ex.: "processos de uma cota com o total pago"). Se faltar, pergunte.

1. **Record** em `query/persistence` com exatamente os campos que a consulta devolve.
2. **Repositório Jakarta Data** em `query/persistence` (`@Repository`), método com `@Query` em HQL usando `select new <pacote>.<Record>(...)` sobre as `*Entity` do módulo. Sem SQL nativo. Se for lista grande, use `PageRequest` e devolva `Page<Record>`.
3. **Use case** em `query/application`: normalize filtros e paginação; injete o repositório direto.
4. **Endpoint neutro** em `query/entrypoint` (sem formatação). Se a tela for de um canal específico, use a receita [novo endpoint de canal](novo-endpoint-de-canal.md).
5. **Teste** `@QuarkusTest` criando os dados pela API de escrita e conferindo o resultado.
6. Nada aqui importa `command`. Rode `./mvnw verify` (o HQL é validado na compilação).
