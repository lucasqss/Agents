# Testes esperados em cada mudança

- **Regra de negócio:** teste JUnit puro, sem Quarkus, no pacote de teste do `domain`.
- **Endpoint novo:** `@QuarkusTest` + RestAssured, cobrindo o caminho feliz e o erro principal. Em canal, conferir os **nomes exatos dos campos** e os textos formatados (veja `CanaisContratoTest`).
- **Formato novo de canal:** linha nova em `PadroesDeCanalTest`.
- **Mapper:** a ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo e versão.
- **Sistema externo:** nunca chamar o real nos testes. Use um Service Stub (veja `TesourariaFake`) com chave para simular falha, e teste o 503.

Antes de terminar qualquer tarefa, rode `./mvnw verify`.
