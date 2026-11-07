---
mode: agent
description: Cria uma consulta de leitura no núcleo (HQL projetado em record e use case) sem tocar no lado de escrita.
---
Crie a consulta descrita abaixo seguindo `.github/copilot-instructions.md`.

Consulta: ${input:consulta:Descreva o que a tela precisa ver, ex.: "processos de uma cota com o total pago"}

1. **Record** em `nucleo`, `query/persistence/projection` com exatamente os campos que a consulta devolve.
2. **Repositório Jakarta Data** em `query/persistence/repository` (`@Repository`), método com `@Query` em HQL usando `select new <pacote>.<Record>(...)` sobre as entidades de `shared/persistence`. Sem SQL nativo. Se for lista grande, use `PageRequest` e devolva `Page<Record>`.
3. **Use case** em `query/application/usecase`: normalize filtros e paginação; injete o repositório direto.
4. **Endpoint**: o núcleo não tem HTTP. Exponha a consulta no canal que precisa dela com o prompt `novo-endpoint-de-canal`.
5. **Teste**: o contrato da tela é testado no módulo do canal (`Contrato<Canal>Test`).
6. Nada aqui importa `command`. Rode `./mvnw verify` (o HQL é validado na compilação).
