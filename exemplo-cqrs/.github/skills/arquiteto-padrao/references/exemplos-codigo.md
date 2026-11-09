# Exemplos de código

Modelos para copiar e adaptar. `cota`, `processo` e `tesouraria` são nomes ilustrativos: troque pelos agregados e sistemas do seu componente, e `org.acme.consorcio` pelo pacote raiz dele. Estas classes não precisam existir no repositório.

## Entidade de domínio

`domain/cota/Cota.java`. Java puro. Construtor privado, fábricas estáticas, regra lançando `DominioException`.

```java
public class Cota {

    private final Long id;
    private String numero;
    private Long clienteId; // outro agregado: só o id

    private Cota(Long id, String numero, Long clienteId) {
        this.id = id;
        alterar(numero, clienteId);
    }

    public static Cota nova(String numero, Long clienteId) {
        return new Cota(null, numero, clienteId);
    }

    /** Usada só pelo mapper. */
    public static Cota reconstituir(Long id, String numero, Long clienteId) {
        return new Cota(id, numero, clienteId);
    }

    public void alterar(String numero, Long clienteId) {
        if (numero == null || numero.isBlank()) {
            throw new DominioException("Número da cota é obrigatório");
        }
        if (clienteId == null) {
            throw new DominioException("Cota precisa de um cliente");
        }
        this.numero = numero.strip();
        this.clienteId = clienteId;
    }

    public Long id() { return id; }
    public String numero() { return numero; }
    public Long clienteId() { return clienteId; }
}
```

## Estados (State)

`domain/processo/estado/EstadoProcesso.java`. Cada ação devolve o próximo estado. Por padrão ela é inválida, e só os estados que a permitem sobrescrevem.

```java
/** Padrão: State, interface do estado [GoF]. */
public sealed interface EstadoProcesso permits Aberto, Pago {

    /** Estado novo: crie o record, inclua em permits e aqui. */
    static List<EstadoProcesso> todos() {
        return List.of(new Aberto(), new Pago());
    }

    /** Reconstrói o estado pelo código gravado no banco. */
    static EstadoProcesso porNome(String nome) {
        return todos().stream()
                .filter(estado -> estado.nome().equals(nome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado desconhecido: " + nome));
    }

    String nome();

    default EstadoProcesso pagar() {
        throw new TransicaoInvalidaException("pagar", nome());
    }
}
```

`domain/processo/estado/Aberto.java`:

```java
/** Padrão: State, estado concreto [GoF]. */
public record Aberto() implements EstadoProcesso {

    @Override
    public String nome() {
        return "ABERTO";
    }

    @Override
    public EstadoProcesso pagar() {
        return new Pago();
    }
}
```

## Variação por tipo (Strategy)

`domain/processo/FluxoProcesso.java` e `domain/processo/TipoProcesso.java`. Tipo novo é uma constante no enum e uma implementação da interface, sem subclasse do agregado.

```java
/** Padrão: Strategy, interface da estratégia [GoF]. */
public interface FluxoProcesso {
    EstadoProcesso aposAnalise();
}

/** Padrão: Strategy, escolhe a estratégia de cada tipo [GoF]. */
public enum TipoProcesso {
    IMOBILIARIO(new FluxoImobiliario()),
    SERVICO(new FluxoServico());

    private final FluxoProcesso fluxo;

    TipoProcesso(FluxoProcesso fluxo) {
        this.fluxo = fluxo;
    }

    public FluxoProcesso fluxo() {
        return fluxo;
    }
}
```

## Mapper

`persistence/processo/ProcessoMapper.java`. Tipo e estado são reconstruídos sem `switch`. Valor desconhecido vindo do banco vira `IllegalStateException` com o valor na mensagem.

```java
/** Padrão: Data Mapper [PoEAA]. */
final class ProcessoMapper {

    private ProcessoMapper() {
    }

    static Processo paraDominio(ProcessoEntity entity) {
        try {
            return Processo.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), EstadoProcesso.porNome(entity.getEstado()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
    }

    static ProcessoEntity paraEntidade(Processo processo) {
        ProcessoEntity entity = new ProcessoEntity();
        entity.setId(processo.id());
        entity.setCotaId(processo.cotaId());
        entity.setTipo(processo.tipo().name());
        entity.setEstado(processo.estado().nome());
        return entity;
    }
}
```

## Adapter

`persistence/processo/ProcessoRepositoryAdapter.java`. Implementa a interface do domínio. O `ProcessoDataRepository` segue o modelo de `XDataRepository` em [persistência](persistencia-jakarta-data.md).

```java
@ApplicationScoped
public class ProcessoRepositoryAdapter implements ProcessoRepository {

    private final ProcessoDataRepository dados;

    public ProcessoRepositoryAdapter(ProcessoDataRepository dados) {
        this.dados = dados;
    }

    @Override
    public Processo salvar(Processo processo) {
        ProcessoEntity entity = ProcessoMapper.paraEntidade(processo);
        if (processo.id() == null) {
            dados.inserir(entity);
        } else {
            dados.atualizar(entity);
        }
        return ProcessoMapper.paraDominio(entity);
    }

    @Override
    public Optional<Processo> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(ProcessoMapper::paraDominio);
    }
}
```

## Bloqueio pessimista

No `XDataRepository`, para o que o Jakarta Data não expressa:

```java
@Repository
interface XDataRepository {

    /** Sessão do repositório, usada apenas onde o Jakarta Data não expressa a operação. */
    StatelessSession sessao();

    default Optional<XEntity> buscarPorIdComBloqueio(Long id) {
        return Optional.ofNullable(sessao().get(XEntity.class, id, LockMode.PESSIMISTIC_WRITE));
    }
}
```

## Comando de escrita (`DadosX`)

O `XRequest` valida o formato e se converte no comando. O caso de uso recebe só o comando.

`application/cota/DadosCota.java`:

```java
public record DadosCota(String numero, Long clienteId) {
}
```

`entrypoint/rest/cota/CotaRequest.java`:

```java
public record CotaRequest(
        @NotBlank @Size(max = 30) String numero,
        @NotNull Long clienteId) {

    DadosCota paraDados() {
        return new DadosCota(numero, clienteId);
    }
}
```

## Client e Gateway

`gateway/tesouraria/TesourariaClient.java`:

```java
@RegisterRestClient(configKey = "tesouraria")
@Path("/ordens-pagamento")
public interface TesourariaClient {

    @POST
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    OrdemPagamentoResponse criarOrdem(OrdemPagamentoRequest request);
}
```

`gateway/tesouraria/TesourariaGateway.java`. API no nosso vocabulário. Qualquer falha vira `SistemaExternoIndisponivelException`.

```java
/** Padrão: Gateway [PoEAA]. */
@ApplicationScoped
public class TesourariaGateway {

    private static final String SISTEMA = "Tesouraria";

    private final TesourariaClient client;

    public TesourariaGateway(@RestClient TesourariaClient client) {
        this.client = client;
    }

    public String registrarPagamento(Long processoId, Long cotaId) {
        OrdemPagamentoResponse resposta;
        try {
            resposta = client.criarOrdem(new OrdemPagamentoRequest("PROCESSO-" + processoId, cotaId));
        } catch (RuntimeException e) {
            throw new SistemaExternoIndisponivelException(SISTEMA, e);
        }
        if (resposta == null || resposta.protocolNumber() == null) {
            throw new SistemaExternoIndisponivelException(SISTEMA,
                    new IllegalStateException("Resposta sem protocolo: " + resposta));
        }
        return resposta.protocolNumber();
    }
}
```

## Service Stub para testes

`src/test/java/<pacote raiz>/integracao/<Sistema>Fake.java`. A própria aplicação serve o stub nos testes com `@QuarkusTest`. Para testes de aceitação ele não serve, porque não entra no artefato empacotado: siga a skill `teste-aceitacao-modelos`.

```java
/** Padrão: Service Stub [PoEAA]. */
@Path("/fake-sistema/ordens-pagamento")
public class SistemaExternoFake {

    /** Ligue no teste do 503 e desligue em um @AfterEach. */
    static volatile boolean falhar;

    @POST
    public Map<String, Object> criarOrdem(Map<String, Object> ordem) {
        if (falhar) {
            throw new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE);
        }
        return Map.of("protocolNumber", "PROTOCOLO-1");
    }
}
```

Em `src/main/resources/application.properties`:

```properties
%test.quarkus.rest-client.<sistema>.url=http://localhost:${quarkus.http.test-port:8081}/fake-sistema
```

## Apresentação do canal

Se `entrypoint/apresentacao/` ainda não existir, crie-a na primeira tela de canal.

`entrypoint/apresentacao/PadraoApresentacao.java`. Um formato novo aqui só compila depois que todos os canais decidirem como exibi-lo.

```java
public interface PadraoApresentacao {
    String moeda(BigDecimal valor);
}
```

`entrypoint/web/PadraoWeb.java`. Classe pura, sem CDI.

```java
public final class PadraoWeb implements PadraoApresentacao {

    public static final PadraoWeb INSTANCIA = new PadraoWeb();

    private PadraoWeb() {
    }

    @Override
    public String moeda(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor).replace(' ', ' ');
    }
}
```

`entrypoint/web/cota/CotaWebAssembler.java`. Recebe o record da consulta e devolve o contrato do front, com o valor bruto e o formatado.

```java
final class CotaWebAssembler {

    private static final PadraoWeb PADRAO = PadraoWeb.INSTANCIA;

    private CotaWebAssembler() {
    }

    static CotaWebResponse montar(CotaDetalhe cota) {
        return new CotaWebResponse(cota.id(), cota.valorCredito(), PADRAO.moeda(cota.valorCredito()));
    }
}
```

`entrypoint/web/cota/CotaWebResource.java`:

```java
@Tag(name = "Canal web")
@Path("/web/v1/cotas")
public class CotaWebResource {

    private final DetalharCotaUseCase detalhar;

    public CotaWebResource(DetalharCotaUseCase detalhar) {
        this.detalhar = detalhar;
    }

    @GET
    @Path("/{id}")
    public CotaWebResponse detalhar(Long id) {
        return detalhar.executar(id).map(CotaWebAssembler::montar)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }
}
```
