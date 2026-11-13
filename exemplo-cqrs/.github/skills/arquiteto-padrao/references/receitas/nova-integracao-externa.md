# Receita: nova integração externa

Entradas necessárias: o nome do sistema (ex.: "cadastro-corporativo"), a operação (ex.: "consultar restrições de um CPF") e o contrato da API (endpoint e JSON, ou link da documentação). Se faltar, pergunte. Veja [gateway](../gateway.md) e os modelos de Client, Gateway e Service Stub em [exemplos de código](../exemplos-codigo.md).

0. **O sistema está no catálogo corporativo?** Se há dependência Maven gerada, use o client e os DTOs dela em vez de escrever os passos 2 e 3, e confira se o `groupId` dela está na lista de `ClassesExternas.java` (se não estiver, peça ao dono). Essas classes não saem de `gateway/<sistema>/`.
1. Pacote `gateway/<sistema>/`, pelo nome do sistema externo, não pelo agregado.
2. DTOs (records) com os nomes de campo **exatos da API externa**.
3. `XClient` com `@RegisterRestClient(configKey = "<sistema>")`, `@Timeout`, `@Retry` e `@CircuitBreaker`.
4. `XGateway` com uma API simples no nosso vocabulário, devolvendo tipo simples ou record próprio (sem classe do catálogo nem DTO externo dentro); traduz e converte qualquer falha em `SistemaExternoIndisponivelException("<Sistema>", causa)`. Não importa nada de `application/` nem de `domain/`.
5. Configuração: `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:http://localhost:...}` e `%test.quarkus.rest-client.<sistema>.url` apontando para o stub.
6. Service Stub de teste em `src/test/java/**/integracao/` (recurso JAX-RS) com chave para simular falha, e um teste do 503.
7. Só um caso de uso chama o Gateway. Se a operação tem efeito no sistema externo e o caso de uso também grava, escolha a forma em [transação e sistema externo](../transacao-e-sistema-externo.md) e envie chave de idempotência.
8. Rode `./mvnw verify`.
