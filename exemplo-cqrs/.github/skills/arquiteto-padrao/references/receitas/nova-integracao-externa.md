# Receita: nova integração externa

Entradas necessárias: o nome do sistema (ex.: "cadastro-corporativo"), a operação (ex.: "consultar restrições de um CPF") e o contrato da API (endpoint e JSON, ou link da documentação). Se faltar, pergunte. Use `gateway/tesouraria` como exemplo e veja [gateway](../gateway.md).

1. Pacote `gateway/<sistema>/`.
2. DTOs (records) com os nomes de campo **exatos da API externa**.
3. `XClient` com `@RegisterRestClient(configKey = "<sistema>")`, `@Timeout`, `@Retry` e `@CircuitBreaker`.
4. `XGateway` com uma API simples no nosso vocabulário; traduz e converte qualquer falha em `SistemaExternoIndisponivelException("<Sistema>", causa)`. Não importa nada de `command` nem de `query`.
5. Configuração: `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:http://localhost:...}` e `%test.quarkus.rest-client.<sistema>.url` apontando para o stub.
6. Service Stub de teste em `src/test/java/**/integracao/` (recurso JAX-RS, como `TesourariaFake`) com chave para simular falha, e um teste do 503.
7. Só um use case chama o Gateway. Rode `./mvnw verify`.
