# Contrato REST

Duas peças seguram o contrato publicado contra a linha de base versionada em `docs/api/openapi-base.json`:

| Peça | Fase | Barra |
|---|---|---|
| `ContratoRestTest` | `test` | campo de resposta removido, renomeado ou com tipo trocado |
| openapi-diff | `verify` | endpoint removido, campo novo obrigatório na requisição, enum reduzido, tipo trocado |

O openapi-diff sozinho **não** trata como quebra a remoção de campo de resposta não obrigatório, e o Quarkus não marca os campos dos records como obrigatórios. Por isso as duas peças existem.

## Adotar

1. Copie `.github/skills/arquiteto-padrao/assets/contrato/ContratoRestTest.java` para `src/test/java/<pacote raiz>/` e troque `PACOTE_RAIZ`.
2. Configure o openapi-diff e a gravação do OpenAPI no build ([pom obrigatório](../../arquiteto-padrao/references/pom-obrigatorio.md), seção "Barreiras de qualidade").
3. Gere a linha de base: `./mvnw package -DskipTests && mkdir -p docs/api && cp target/openapi/openapi.json docs/api/openapi-base.json`.

## Mudar a API

- **Aditiva** (campo novo, endpoint novo, `/v2`): os dois passam. Atualize a linha de base no mesmo commit, com o comando acima.
- **Quebra** (campo removido ou renomeado): os dois falham. Não atualize a linha de base para o build passar: publique em `/v2` e mantenha a versão anterior.
