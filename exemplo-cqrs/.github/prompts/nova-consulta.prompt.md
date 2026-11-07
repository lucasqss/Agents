---
mode: agent
description: Cria uma consulta de leitura (HQL projetado em record, use case e endpoint neutro) sem tocar no lado de escrita.
---
Crie a consulta descrita abaixo seguindo `.github/copilot-instructions.md`.

Consulta: ${input:consulta:Descreva o que a tela precisa ver, ex.: "processos de uma cota com o total pago"}

1. **Record** em `query/persistence` com exatamente os campos que a consulta devolve.
2. **Repositório Jakarta Data** em `query/persistence` (`@Repository`), método com `@Query` em HQL usando `select new <pacote>.<Record>(...)` sobre as entidades de `shared/persistence`. Sem SQL nativo. Se for lista grande, use `PageRequest` e devolva `Page<Record>`.
3. **Use case** em `query/application`: normalize filtros e paginação; injete o repositório direto.
4. **Endpoint neutro** em `query/entrypoint` (sem formatação). Se a tela for de um canal específico, use o prompt `novo-endpoint-de-canal`.
5. **Teste** `@QuarkusTest` criando os dados pela API de escrita e conferindo o resultado.
6. Nada aqui importa `command`. Rode `./mvnw verify` (o HQL é validado na compilação).
