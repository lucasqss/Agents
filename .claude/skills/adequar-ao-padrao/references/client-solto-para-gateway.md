# REST client solto → Gateway

O destino está em `references/gateway.md` da skill `arquiteto-padrao`.

1. Mova o client e seus DTOs para `gateway/<sistema>/`, pelo nome do sistema externo, não pelo agregado. Os DTOs mantêm os nomes de campo **exatos da API externa** e não saem desse pacote.
2. Crie o `XGateway` com uma API no nosso vocabulário e troque, em cada chamador, o uso direto do client pelo Gateway.
3. **Quem chamava o client passa a ser um use case.** Se era um Resource ou uma entidade, a chamada sobe ou desce para o use case.
4. Junte num só lugar o tratamento de erro que estava espalhado pelos chamadores: qualquer falha vira `SistemaExternoIndisponivelException`.
5. **Resiliência:** coloque `@Timeout`, `@Retry` e `@CircuitBreaker` no client. Se antes não havia, valores e retentativas são mudança de comportamento: confirme com o dono e nunca use `@Retry` em operação não idempotente sem essa confirmação.
6. Mantenha a chave de configuração da URL, ou migre para `quarkus.rest-client.<sistema>.url` preservando o valor em cada ambiente.
7. Crie o Service Stub de teste, conforme `references/exemplos-codigo.md` da skill `arquiteto-padrao`, e o teste do 503.
8. **Classe do catálogo corporativo** (dependência Maven gerada) usada em service, use case ou domínio: troque por tipo simples ou por um record próprio em `gateway/<sistema>/`, devolvido pelo Gateway (TEC-011).
9. Se o chamador tratava erros específicos (404 do sistema externo significa "não existe", por exemplo), preserve isso no retorno do Gateway, sem expor o DTO.
