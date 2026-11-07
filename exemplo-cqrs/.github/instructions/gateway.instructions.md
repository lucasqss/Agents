---
applyTo: "**/shared/gateway/**"
---
# Sistemas externos (shared/gateway/<sistema>)

Cada sistema externo tem seu pacote, com o padrão Gateway (PoEAA):
- `XClient`: interface `@RegisterRestClient(configKey = "<sistema>")` com `@Timeout`, `@Retry` e `@CircuitBreaker` nos métodos. URL em `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:...}`.
- DTOs no **vocabulário do sistema externo** (records). Eles nunca saem deste pacote (o ArchUnit falha).
- `XGateway` (`@ApplicationScoped`): API simples no nosso vocabulário (tipos simples ou records próprios), traduz request/response e converte qualquer falha em `SistemaExternoIndisponivelException` (vira 503).

Regras:
- O Gateway **não importa nada de `command` nem de `query`**, nem do domínio. Quem converte o retorno para o domínio é o use case.
- Um sistema externo não depende de outro; quem combina dois sistemas é o use case.
- Só use cases chamam gateways.
- Nos testes, nunca chame o sistema real: crie um Service Stub em `src/test/java/**/integracao/` (recurso JAX-RS fake, como `TesourariaFake`) e aponte `%test.quarkus.rest-client.<sistema>.url` para ele.
