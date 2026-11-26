# Identidade nos testes

Os testes geram tokens JWT assinados com uma **chave de teste** (par em `src/test/resources`, que não vale em nenhum outro lugar). O emissor e a chave pública de teste chegam à aplicação pelo recurso de teste, para valerem também no `*IT` da aceitação, que roda no perfil `prod`. Modelo no exemplo: `aceitacao/IdentidadeTeste.java` (gera o token com usuário, papéis e o identificador do cliente) e o mapa de configuração devolvido por `TesourariaStubResource.start()`.

## Matriz papel × endpoint

Um teste parametrizado, uma linha por endpoint protegido:

| Endpoint | Sem token | Papel errado | Papel certo |
|---|---|---|---|
| `POST /pedidos` | `401` | `403` (gerente) | `201` (cliente) |
| `POST /pedidos/{id}/aprovar` | `401` | `403` (cliente) | `200` (gerente) |

- `401` e `403` saem no formato de erro padrão.
- Token expirado, de outro emissor ou com assinatura inválida: `401` (um caso de cada, num endpoint qualquer).
- Endpoint novo entra na matriz no mesmo commit.

## O que fica na unidade

Regra de permissão que depende do registro ("só o dono da conta", "quem aprova não pode ser quem analisou") é do domínio e é provada na `teste-unidade`. Aqui basta um caso que prove que o endpoint entrega a identidade ao domínio e traduz a recusa em `403`.
