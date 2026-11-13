# Remover o que é proibido

| Encontrado | O que fazer |
|---|---|
| Lombok em `*Entity` | getters e setters escritos |
| Lombok em domínio | construtor privado + fábricas + acessores `nome()`; nunca gere setter |
| Lombok em DTO, Request, Response | `record` |
| `@Slf4j` e afins | logger explícito ou `io.quarkus.logging.Log`, seguindo o que o projeto já usa |
| MapStruct | mapper `final` à mão, com testes de ida e volta. Confira os campos ignorados e as conversões implícitas: precisam continuar iguais |
| SQL nativo traduzível | `@Query` em HQL, com teste comparando o resultado antes e depois |
| SQL nativo não traduzível (função específica do banco, CTE recursiva) | **pare e pergunte ao dono** |
| Herança JPA | **pare e pergunte ao dono:** separar exigiria mudar o schema |

Depois de remover, tire a dependência do `pom.xml` e o processador de anotações correspondente.
