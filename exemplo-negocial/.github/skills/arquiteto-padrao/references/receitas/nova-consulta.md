# Receita: nova consulta de leitura

Entrada necessária: o que a tela precisa ver (ex.: "processos de uma cota com o total pago"). Se faltar, pergunte.

A pasta é a do agregado principal da consulta.

1. **Record** em `persistence/<agregado>/` com exatamente os campos que a consulta devolve.
2. **`XQueryRepository`** em `persistence/<agregado>/` (`@Repository`), método com `@Query` em HQL usando `select new <pacote>.<Record>(...)`. Sem SQL nativo. Se for lista grande, use `PageRequest` e devolva `Page<Record>`.
3. **Caso de uso** em `application/<agregado>/`: um método público, normalize filtros e paginação, injete o `XQueryRepository` direto. Não use nada de `domain/`.
4. **Endpoint neutro** em `entrypoint/rest/<agregado>/` (sem formatação). Se a tela for de um canal específico, use a receita [novo endpoint de canal](novo-endpoint-de-canal.md).
5. **Teste** `@QuarkusTest` criando os dados pela API de escrita e conferindo o resultado.
6. Rode `./mvnw verify` (o HQL é validado na compilação).
