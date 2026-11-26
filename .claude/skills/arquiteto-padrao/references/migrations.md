# Migrations Flyway (`src/main/resources/db/migration`)

- **Nunca edite nem apague uma migration existente**: ela já rodou em algum ambiente. Toda mudança é um arquivo novo `V<próximo número>__descricao_em_snake_case.sql`.
- PostgreSQL. Prefira mudanças compatíveis com a versão anterior da aplicação (adicionar coluna nula, depois preencher, depois restringir).
- **Coluna obrigatória em tabela que já tem dados**, na mesma migration e em três passos: `add column ... numeric(15,2)` anulável; `update` que preenche as linhas existentes com um valor justificável (ex.: copiado da tabela pai); `alter column ... set not null`. Dizer na descrição qual foi o valor de preenchimento.
- **Tabela filha com regra nova de "ao menos um"**: a migration cria o filho padrão para cada raiz existente e diz na descrição de onde veio o valor (ex.: o titular copiado da tabela pai), para a regra valer também para os dados antigos.
- **Coluna de autoria em histórico que já tem linhas:** as linhas antigas recebem um valor que diz a verdade (`'desconhecido'`), nunca um usuário inventado.
- Toda tabela ou coluna nova precisa do campo correspondente na `*Entity` e, se for escrita, do ajuste no mapper.
