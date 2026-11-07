---
mode: agent
description: Cria o Gateway de um sistema externo (REST client, DTOs, resiliência, erro 503 e Service Stub para testes).
---
Crie a integração com o sistema externo descrito abaixo seguindo `.github/copilot-instructions.md` e o exemplo `shared/gateway/tesouraria`.

Sistema: ${input:sistema:Nome do sistema, ex.: "cadastro-corporativo"}
Operação: ${input:operacao:O que precisamos dele, ex.: "consultar restrições de um CPF"}
Contrato da API externa: ${input:contrato:Cole aqui o endpoint e o JSON de request/response, ou o link da documentação}

1. Pacote `shared/gateway/<sistema>/` no `nucleo`.
2. DTOs (records) com os nomes de campo **exatos da API externa**.
3. `XClient` com `@RegisterRestClient(configKey = "<sistema>")`, `@Timeout`, `@Retry` e `@CircuitBreaker`.
4. `XGateway` com uma API simples no nosso vocabulário; traduz e converte qualquer falha em `SistemaExternoIndisponivelException("<Sistema>", causa)`. Não importa nada de `command` nem de `query`.
5. Configuração: `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:http://localhost:...}` em `nucleo/src/main/resources/META-INF/microprofile-config.properties` e `%test.quarkus.rest-client.<sistema>.url` apontando para o stub no `application.properties` do canal que testa a chamada.
6. Service Stub de teste no canal que exercita a chamada (ex.: `canal-web/src/test/java/**`) (recurso JAX-RS, como `TesourariaFake`) com chave para simular falha, e um teste do 503.
7. Só um use case chama o Gateway. Rode `./mvnw verify`.
