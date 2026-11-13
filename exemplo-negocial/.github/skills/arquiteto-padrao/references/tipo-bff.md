# Tipo BFF (`entrypoint`, `application`, `gateway`)

Um BFF adapta os componentes negociais para **um** canal. Não tem regra de negócio, banco nem `translator`. Os nomes `mobile`, `cota`, `consorcio` e `cobranca` abaixo são ilustrativos.

## Onde cada coisa mora

| Camada | Caminho | O que tem |
|---|---|---|
| Entrada | `entrypoint/<canal>/<recurso>/` | Resource, Request, Response, Assembler |
| Padrão do canal | `entrypoint/<canal>/` e `entrypoint/apresentacao/` | `Padrao<Canal>`, `Formatador`, `Rotulos`, `PadraoApresentacao` |
| Caso de uso | `application/<recurso>/` | um `*UseCase` por classe e os records da composição |
| Negocial chamado | `gateway/<negocial>/` | Client, DTOs do negocial, `*Gateway`, records próprios |

```text
src/main/java/org/acme/consorciomobile/
├── entrypoint/
│   ├── mobile/
│   │   ├── PadraoMobile.java
│   │   └── cota/        PainelCotaMobileResource.java, PainelCotaMobileResponse.java, PainelCotaMobileAssembler.java
│   └── apresentacao/    Formatador.java, Rotulos.java, PadraoApresentacao.java
├── application/cota/    MontarPainelCotaUseCase.java, PainelCota.java
└── gateway/
    ├── consorcio/       ConsorcioClient.java, CotaDto.java, ConsorcioGateway.java, Cota.java
    └── cobranca/        CobrancaClient.java, BoletoDto.java, CobrancaGateway.java, Boleto.java
```

`<recurso>` é o assunto principal da tela (o mesmo nome que ela teria como agregado no negocial). O fluxo é sempre `entrypoint/<canal> → application → gateway/<negocial>`.

## Regras de cada peça

- **Entrada, Assembler e `Padrao<Canal>`:** valem as mesmas regras de [canais](canais.md): contrato congelado, valor bruto e formatado, Assembler puro. A única diferença é a origem do dado: aqui vem do caso de uso de composição, não de uma consulta em `persistence/`.
- **Um canal só (BFF-002)** e só entrada de canal (BFF-004): nada de `entrypoint/rest`, `mensageria` ou `batch`.
- **`XGateway`:** igual a [gateway](gateway.md). Chama a API do negocial e devolve tipo simples ou record próprio. O DTO do negocial não sai de `gateway/<negocial>/`.
- **`XUseCase` de composição:** chama os Gateways necessários e devolve um record de `application/<recurso>/` com tudo que a tela precisa. Sem `@Transactional`. Para cada parte, decida e escreva no código se ela é **essencial** (a falha vira erro da tela) ou **opcional** (a falha vira campo ausente, e o Response tem como dizer isso).
- **Escrita:** o caso de uso repassa a **um único** negocial e devolve o mínimo. Se a operação precisaria gravar em dois negociais, pare e pergunte ao dono: isso é caso de uso de um negocial.
- **Sem regra de negócio:** o BFF soma ou formata o que já veio pronto. Número com significado de negócio (saldo, total pago, pode ou não pode) vem calculado do negocial.
- **Destinos permitidos:** só componentes negociais. BFF não chama ACL, sistema externo nem outro BFF.

## Exemplos

`gateway/consorcio/Cota.java`. Record próprio: o que o BFF precisa da cota, sem o DTO do negocial dentro.

```java
public record Cota(Long id, BigDecimal valorCredito) {
}
```

`application/cota/PainelCota.java`:

```java
/** O boleto é opcional: fica vazio quando a cobrança está fora do ar. */
public record PainelCota(Cota cota, Optional<Boleto> boleto) {
}
```

`application/cota/MontarPainelCotaUseCase.java`:

```java
@ApplicationScoped
public class MontarPainelCotaUseCase {

    private final ConsorcioGateway consorcio;
    private final CobrancaGateway cobranca;

    public MontarPainelCotaUseCase(ConsorcioGateway consorcio, CobrancaGateway cobranca) {
        this.consorcio = consorcio;
        this.cobranca = cobranca;
    }

    public PainelCota executar(Long cotaId) {
        Cota cota = consorcio.buscarCota(cotaId); // essencial: a falha sobe como 503
        return new PainelCota(cota, boletoSeDisponivel(cotaId));
    }

    private Optional<Boleto> boletoSeDisponivel(Long cotaId) { // opcional
        try {
            return cobranca.buscarBoletoAberto(cotaId);
        } catch (SistemaExternoIndisponivelException e) {
            return Optional.empty();
        }
    }
}
```

`entrypoint/mobile/cota/PainelCotaMobileAssembler.java`:

```java
final class PainelCotaMobileAssembler {

    private static final PadraoMobile PADRAO = PadraoMobile.INSTANCIA;

    private PainelCotaMobileAssembler() {
    }

    static PainelCotaMobileResponse montar(PainelCota painel) {
        return new PainelCotaMobileResponse(
                painel.cota().id(),
                painel.cota().valorCredito(),
                PADRAO.moeda(painel.cota().valorCredito()),
                painel.boleto().map(boleto -> PADRAO.moeda(boleto.valor())).orElse(null),
                painel.boleto().isPresent());
    }
}
```

O Resource segue o modelo de `XCanalResource` em [exemplos de código](exemplos-codigo.md): `@Path("/mobile/v1/...")`, chama o caso de uso e devolve o `*Response` montado pelo Assembler.

## Testes

- **Contrato do canal:** `@QuarkusTest` conferindo o conjunto exato de campos e os textos formatados, com Service Stub de cada negocial.
- **Degradação:** um teste por parte opcional, com o stub em falha, conferindo o campo ausente e o indicador; um teste do 503 para cada parte essencial.
- **Assembler e `Padrao<Canal>`:** JUnit puro.

## Receita: nova tela

Entradas necessárias: a tela campo a campo, com o formato esperado; de qual negocial vem cada dado; o que é essencial e o que é opcional. Se faltar, pergunte.

1. Para cada negocial envolvido, `gateway/<negocial>/`: client, DTO, método no `XGateway` e record próprio (receita [nova integração externa](receitas/nova-integracao-externa.md)).
2. `application/<recurso>/`: o record da composição e o `XUseCase`, com o essencial e o opcional explícitos.
3. `entrypoint/<canal>/<recurso>/`: `XCanalResponse`, `XCanalAssembler` e `XCanalResource` (receita [novo endpoint de canal](receitas/novo-endpoint-de-canal.md), trocando a consulta pelo caso de uso de composição).
4. Testes de contrato e de degradação.
5. Rode `./mvnw verify`. Se uma regra falhar (GER, SHR, CAN, BFF), corrija o código, nunca a regra.
