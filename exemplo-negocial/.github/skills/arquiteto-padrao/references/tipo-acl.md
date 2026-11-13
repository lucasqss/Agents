# Tipo ACL (`entrypoint`, `application`, `translator`, `gateway`)

Um ACL traduz entre um sistema externo e o nosso vocabulário. Não tem regra de negócio nem dado de negócio. Um ACL por sistema externo. Os nomes `tesouraria` e `ordem` abaixo são ilustrativos.

## Onde cada coisa mora

| Camada | Caminho | O que tem |
|---|---|---|
| Entrada | `entrypoint/<via>/<conceito>/` | Resource (ou consumidor), Request |
| Caso de uso | `application/<conceito>/` | um `*UseCase` por classe |
| Tradução | `translator/<sistema>/` | `*Translator` e os records do nosso vocabulário |
| Sistema do outro lado | `gateway/<sistema>/` | Client, modelo externo, `*Gateway` |

```text
src/main/java/org/acme/tesourariaacl/
├── entrypoint/rest/ordem/     OrdemResource.java
├── application/ordem/         ConsultarOrdemUseCase.java
├── translator/tesouraria/     OrdemPagamentoTranslator.java, OrdemPagamento.java
└── gateway/tesouraria/        TesourariaClient.java, PaymentOrderOutput.java, TesourariaGateway.java
```

O fluxo é sempre `entrypoint → application → gateway e translator`:

1. o Resource chama o caso de uso;
2. o caso de uso chama o `*Gateway`, que devolve o **modelo externo**;
3. o caso de uso passa o modelo externo ao `*Translator`, que devolve o **nosso record**;
4. o caso de uso devolve o nosso record; o Resource o devolve.

## Os dois sentidos

| | Saída (nós chamamos o sistema externo) | Entrada (o sistema externo nos chama) |
|---|---|---|
| Quem chama o ACL | um componente negocial, no nosso vocabulário | o sistema externo, no vocabulário dele |
| `entrypoint` | contrato nosso, versionado (`/v1`) | contrato dele (classe do catálogo ou Request com os nomes dele) |
| `gateway/<sistema>/` | o sistema externo | o componente negocial que recebe |
| `*Translator` | externo → nosso | externo → nosso, antes de chamar o negocial |

## Regras de cada peça

- **`XGateway`:** só transporte, resiliência e erro. Chama o client (escrito à mão ou do catálogo corporativo), converte qualquer falha em `SistemaExternoIndisponivelException` e devolve o modelo externo. Não traduz e não importa `translator/`.
- **`XTranslator`:** classe `final`, construtor privado, métodos estáticos, sem CDI nem framework. Recebe o modelo de um lado e devolve o do outro. Não chama Gateway, Client nem Resource. Valor externo desconhecido (código de situação novo, por exemplo) lança exceção com o valor na mensagem; nunca vira um padrão silencioso.
- **Records do nosso vocabulário:** moram em `translator/<sistema>/`, com nomes e tipos nossos. São o que o caso de uso devolve e o que a API neutra expõe.
- **`XUseCase`:** um método público, sem `@Transactional`. É o único que chama o `*Translator`. Nunca devolve modelo externo (ACL-006).
- **Sem banco:** nada de JPA, Jakarta Data ou transação (ACL-001). Cache, controle de mensagem já processada ou fila de reenvio: pare e peça ao dono; se aprovado, fica atrás de um `*Gateway`.
- **Sem regra de negócio:** se a tradução precisa decidir algo que depende do estado do nosso negócio, essa decisão é do componente negocial.

## Exemplos

`translator/tesouraria/OrdemPagamento.java`:

```java
public record OrdemPagamento(String protocolo, SituacaoOrdem situacao) {
}
```

`translator/tesouraria/OrdemPagamentoTranslator.java`:

```java
/** Padrão: Mapper [PoEAA], tradução pura entre o modelo da tesouraria e o nosso. */
public final class OrdemPagamentoTranslator {

    private OrdemPagamentoTranslator() {
    }

    public static OrdemPagamento paraNosso(PaymentOrderOutput externo) {
        return new OrdemPagamento(externo.protocolNumber(), situacao(externo.sts()));
    }

    private static SituacaoOrdem situacao(String codigo) {
        return switch (codigo) {
            case "P" -> SituacaoOrdem.PAGA;
            case "W" -> SituacaoOrdem.PENDENTE;
            default -> throw new IllegalStateException("Situação desconhecida na tesouraria: " + codigo);
        };
    }
}
```

`gateway/tesouraria/TesourariaGateway.java`:

```java
/** Padrão: Gateway [PoEAA]. */
@ApplicationScoped
public class TesourariaGateway {

    private final TesourariaClient client;

    public TesourariaGateway(@RestClient TesourariaClient client) {
        this.client = client;
    }

    public PaymentOrderOutput buscarOrdem(Long id) {
        try {
            return client.buscar(id);
        } catch (RuntimeException e) {
            throw new SistemaExternoIndisponivelException("Tesouraria", e);
        }
    }
}
```

`application/ordem/ConsultarOrdemUseCase.java`:

```java
@ApplicationScoped
public class ConsultarOrdemUseCase {

    private final TesourariaGateway tesouraria;

    public ConsultarOrdemUseCase(TesourariaGateway tesouraria) {
        this.tesouraria = tesouraria;
    }

    public OrdemPagamento executar(Long id) {
        return OrdemPagamentoTranslator.paraNosso(tesouraria.buscarOrdem(id));
    }
}
```

`entrypoint/rest/ordem/OrdemResource.java`:

```java
@Path("/v1/ordens")
public class OrdemResource {

    private final ConsultarOrdemUseCase consultar;

    public OrdemResource(ConsultarOrdemUseCase consultar) {
        this.consultar = consultar;
    }

    @GET
    @Path("/{id}")
    public OrdemPagamento buscar(Long id) {
        return consultar.executar(id);
    }
}
```

## Testes

- **Translator:** JUnit puro. Guarde respostas reais do sistema externo em `src/test/resources/<sistema>/*.json` e confira o record traduzido, inclusive o caso de valor desconhecido.
- **Contrato publicado:** `@QuarkusTest` conferindo os nomes exatos dos campos da resposta. Só mudanças aditivas; quebra vira `/v2`.
- **Falha do sistema externo:** Service Stub com chave de falha e teste do 503 ([exemplo](exemplos-codigo.md)).

## Receita: nova operação de tradução

Entradas necessárias: o sistema, a operação, o contrato externo (ou a dependência do catálogo) e os campos que o nosso lado precisa, com os nomes do nosso vocabulário. Se faltar, pergunte.

1. `gateway/<sistema>/`: client e modelo externo (ou a dependência do catálogo; confira o `groupId` em `ClassesExternas.java`) e o método no `XGateway`, devolvendo o modelo externo.
2. `translator/<sistema>/`: o record do nosso vocabulário e o método no `XTranslator`. Escreva antes o teste, com um JSON real.
3. `application/<conceito>/XUseCase`: chama o Gateway, passa pelo Translator, devolve o nosso record.
4. `entrypoint/<via>/<conceito>/`: endpoint ou consumidor que chama o caso de uso.
5. Testes de contrato e do 503.
6. Rode `./mvnw verify`. Se uma regra falhar (GER, SHR, TEC, ACL), corrija o código, nunca a regra.
