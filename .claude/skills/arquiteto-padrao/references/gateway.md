# Sistemas externos (`gateway/<sistema>/`)

Esta camada se organiza por **sistema externo**, e não por agregado: o mesmo sistema pode servir a vários agregados. Exemplo: `gateway/tesouraria/`.

Cada sistema tem seu pacote, com o padrão Gateway [PoEAA] ([exemplo](exemplos-codigo.md)):

- `XClient`: interface `@RegisterRestClient(configKey = "<sistema>")` com `@Timeout`, `@Retry` e `@CircuitBreaker` nos métodos. URL em `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:...}`.
- DTOs no **vocabulário do sistema externo** (records), com os nomes de campo exatos da API. Eles nunca saem deste pacote (o ArchUnit falha).
- `XGateway` (`@ApplicationScoped`): API simples no nosso vocabulário, traduz request/response e converte qualquer falha em `SistemaExternoIndisponivelException` (vira 503). Essa exceção fica na raiz de `gateway/`.
- **Records próprios** (opcional): quando o retorno não cabe num tipo simples, o `XGateway` devolve um record nosso, declarado neste pacote, com nomes do nosso vocabulário. O caso de uso pode importá-lo; o domínio, não.

## Gateway que não lança

Quando quem chama é uma rotina que tenta de novo ([saga por rotina](transacao-e-sistema-externo.md)), a exceção não ajuda: o `XGateway` devolve um record próprio de resposta, selado (confirmada, recusada, sem resposta, incoerente), e traduz erro, timeout e circuito aberto em "sem resposta".

- Sem `@Retry`: a repetição é da rotina. `@Timeout` sempre; `@CircuitBreaker` é opcional e, com ele, nem toda tentativa chega ao sistema externo.
- O Gateway não importa o domínio. O caso de uso traduz a resposta para o tipo de resultado do domínio, e é o domínio que decide o que é incoerente. Veja `TesourariaGateway` e `ResultadoPagamento` no exemplo.

## Catálogo corporativo

O client e os DTOs de muitos sistemas chegam prontos, como dependência Maven gerada pelo catálogo corporativo. Nesse caso não se escreve `XClient` nem DTO à mão: o `XGateway` usa as classes do catálogo e devolve tipo simples ou record próprio.

- **Classe do catálogo só aparece em `gateway/` (ao consumir) e em `entrypoint/` (ao prover).** Nunca em `application/`, `domain/` nem `persistence/` (TEC-011).
- Uma classe é externa quando vem de um jar cujo `groupId` está listado em `src/test/java/<pacote raiz>/arquitetura/ClassesExternas.java`. O `groupId` do próprio componente já vem na lista (as dependências do catálogo usam o mesmo). Sistema externo com outro `groupId`: peça ao dono para acrescentá-lo à lista.
- **Record próprio é só nosso:** não tem campo, parâmetro nem método com classe do catálogo, e não aparece na assinatura de um `XClient`. Se tiver, ele conta como modelo externo e não sai do gateway (SHR-003).
- Ao prover para outro sistema, veja "Contrato do catálogo" em [canais](canais.md).

Em um componente **ACL**, o Gateway não traduz: devolve o modelo externo, e o caso de uso o leva ao `*Translator` ([tipo ACL](tipo-acl.md)). Nos tipos Negocial e BFF vale o que está abaixo.

Regras:

- O Gateway **não importa nada de `application/` nem de `domain/`.** Quem converte o retorno (tipo simples ou record próprio) para o domínio é o caso de uso.
- **A única exceção é o `XGatewayAdapter`**, usado quando uma regra do domínio precisa de um sistema externo: a interface fica em `domain/<agregado>/` e a implementação em `gateway/<sistema>/XGatewayAdapter.java`. Ele só traduz entre domínio e Gateway, não orquestra nem acessa banco, e ninguém o referencia pelo nome, só pela interface (CMD-006, CMD-007, CMD-008).
- Um sistema externo não depende de outro; quem combina dois sistemas é o caso de uso.
- Só casos de uso chamam gateways.
- Operação com efeito no sistema externo envia uma chave de idempotência estável, montada no Gateway a partir do que identifica a operação no nosso lado ([concorrência](concorrencia.md)). `@Retry` em operação com efeito só com essa chave.
- `@RegisterRestClient`, `@Timeout`, `@Retry` e `@CircuitBreaker` só existem em `gateway/`.
- Nos testes, nunca chame o sistema real: crie um Service Stub (recurso JAX-RS fake, [exemplo](exemplos-codigo.md)) e aponte `%test.quarkus.rest-client.<sistema>.url` para ele.
