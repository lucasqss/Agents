---
applyTo: "**/db/migration/**"
---
# Migrations Flyway

- **Nunca edite nem apague uma migration existente**: ela já rodou em algum ambiente. Toda mudança é um arquivo novo `V<próximo número>__descricao_em_snake_case.sql`.
- PostgreSQL. Prefira mudanças compatíveis com a versão anterior da aplicação (adicionar coluna nula, depois preencher, depois restringir).
- Toda tabela/coluna nova precisa do campo correspondente na `*Entity` de `shared/persistence` e, se for escrita, do mapper em `command/persistence/mapper`.
