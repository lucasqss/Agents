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

## Value Object e Money

Em `domain/` (raiz, porque vários agregados usam). Regras em [valores](valores.md).

```java
/** Padrão: Money [PoEAA], como Value Object. Valor monetário com duas casas. */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    private static final int CASAS = 2;

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    public Dinheiro {
        if (valor == null) {
            throw new DominioException("Valor monetário é obrigatório");
        }
        if (valor.stripTrailingZeros().scale() > CASAS) {
            throw new DominioException("Valor monetário tem no máximo " + CASAS + " casas decimais");
        }
        valor = valor.setScale(CASAS);
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    /** Multiplica por um fator (ex.: taxa) e arredonda o centavo para o par mais próximo. */
    public Dinheiro multiplicar(BigDecimal fator) {
        return new Dinheiro(valor.multiply(fator).setScale(CASAS, RoundingMode.HALF_EVEN));
    }

    /** Partes que somam exatamente o total: os centavos que sobram vão para as primeiras. */
    public List<Dinheiro> ratear(int partes) {
        if (partes <= 0) {
            throw new DominioException("Rateio precisa de ao menos uma parte");
        }
        BigInteger[] divisao = valor.unscaledValue().divideAndRemainder(BigInteger.valueOf(partes));
        int sobra = divisao[1].abs().intValue();
        BigInteger centavo = BigInteger.valueOf(divisao[1].signum());
        List<Dinheiro> resultado = new ArrayList<>(partes);
        for (int i = 0; i < partes; i++) {
            BigInteger centavos = i < sobra ? divisao[0].add(centavo) : divisao[0];
            resultado.add(new Dinheiro(new BigDecimal(centavos, CASAS)));
        }
        return List.copyOf(resultado);
    }

    public boolean positivo() {
        return valor.signum() > 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }
}
```

A entidade recebe o valor pronto e o mapper converte para a coluna:

```java
// domínio
if (valorCredito == null || !valorCredito.positivo()) {
    throw new DominioException("Valor do crédito precisa ser positivo");
}

// caso de uso: o DadosX traz BigDecimal
Cota cota = Cota.nova(dados.numero(), Dinheiro.de(dados.valorCredito()), dados.clienteId());

// mapper
Cota.reconstituir(entity.getId(), entity.getNumero(), Dinheiro.de(entity.getValorCredito()), entity.getClienteId());
entity.setValorCredito(cota.valorCredito().valor());
```

## Ciclo de vida (State dirigido por tabela)

Estados e ações são enums; a tabela diz de qual estado se vai para qual. Ficam em `domain/processo/fluxo/`.

`domain/processo/fluxo/EstadoProcesso.java`. O nome da constante é o código gravado no banco.

```java
public enum EstadoProcesso {

    ABERTO(false),
    EM_ANALISE(false),
    AGUARDANDO_VISTORIA(false),
    APROVADO(false),
    PAGO(true),
    CANCELADO(true);

    private final boolean finalizado;

    EstadoProcesso(boolean finalizado) {
        this.finalizado = finalizado;
    }

    /** Estado final não aceita nenhuma ação. */
    public boolean finalizado() {
        return finalizado;
    }
}
```

`domain/processo/fluxo/AcaoProcesso.java`:

```java
public enum AcaoProcesso {
    INICIAR_ANALISE, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, DEVOLVER_PARA_ANALISE, PAGAR, CANCELAR
}
```

`domain/processo/fluxo/FluxoProcesso.java`. Tabela imutável. `comum()` é o caminho de todo tipo; cada tipo troca só as linhas em que difere.

```java
/** Padrão: State dirigido por tabela [GoF, State, "table-driven approach"]. */
public final class FluxoProcesso {

    private final Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes;

    private FluxoProcesso(Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes) {
        this.transicoes = transicoes;
    }

    /** Caminho que todo tipo segue, salvo as linhas que ele trocar. */
    public static Builder comum() {
        return new Builder()
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.PAGAR, EstadoProcesso.PAGO);
    }

    /** Próximo estado, ou TransicaoInvalidaException se a ação não vale no estado atual. */
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao) {
        EstadoProcesso destino = transicoes.get(atual).get(acao);
        if (destino == null) {
            throw new TransicaoInvalidaException(acao, atual);
        }
        return destino;
    }

    public Set<AcaoProcesso> acoesDisponiveis(EstadoProcesso atual) {
        return transicoes.get(atual).keySet();
    }

    public static final class Builder {

        private final Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes =
                new EnumMap<>(EstadoProcesso.class);

        private Builder() {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                transicoes.put(estado, new EnumMap<>(AcaoProcesso.class));
            }
        }

        /** Acrescenta a linha; se já existe uma para o mesmo estado e ação, troca o destino. */
        public Builder transicao(EstadoProcesso de, AcaoProcesso acao, EstadoProcesso para) {
            if (de.finalizado()) {
                throw new IllegalArgumentException("Estado final não tem saída: " + de);
            }
            transicoes.get(de).put(acao, para);
            return this;
        }

        /** Fecha a tabela. Regra de todos os tipos: qualquer estado não final pode ser cancelado. */
        public FluxoProcesso construir() {
            Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> tabela = new EnumMap<>(EstadoProcesso.class);
            transicoes.forEach((estado, saidas) -> {
                Map<AcaoProcesso, EstadoProcesso> linhas = new EnumMap<>(saidas);
                if (!estado.finalizado()) {
                    linhas.put(AcaoProcesso.CANCELAR, EstadoProcesso.CANCELADO);
                }
                tabela.put(estado, Collections.unmodifiableMap(linhas));
            });
            return new FluxoProcesso(Collections.unmodifiableMap(tabela));
        }
    }
}
```

`domain/processo/fluxo/Transicao.java`. O registro de cada mudança de estado, que vira o histórico.

```java
public record Transicao(EstadoProcesso de, EstadoProcesso para, AcaoProcesso acao, Instant quando, String observacao) {
}
```

## Fluxo que muda por tipo

Em `domain/processo/tipo/`. Tipo novo é uma constante no enum e uma classe com os desvios dele, sem subclasse do agregado e sem tocar na entidade.

`domain/processo/tipo/FluxoImobiliario.java`:

```java
/** Imobiliário: depois da análise, passa pela vistoria antes de ser aprovado. */
final class FluxoImobiliario {

    private FluxoImobiliario() {
    }

    static FluxoProcesso definir() {
        return FluxoProcesso.comum()
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE, EstadoProcesso.AGUARDANDO_VISTORIA)
                .transicao(EstadoProcesso.AGUARDANDO_VISTORIA, AcaoProcesso.CONCLUIR_VISTORIA, EstadoProcesso.APROVADO)
                .construir();
    }
}
```

`domain/processo/tipo/TipoProcesso.java`. O nome da constante é o código gravado no banco.

```java
public enum TipoProcesso {
    IMOBILIARIO(FluxoImobiliario.definir()),
    SERVICO(FluxoProcesso.comum().construir());

    private final FluxoProcesso fluxo;

    TipoProcesso(FluxoProcesso fluxo) {
        this.fluxo = fluxo;
    }

    public FluxoProcesso fluxo() {
        return fluxo;
    }
}
```

## Entidade que usa o fluxo

`domain/processo/Processo.java` (só a parte do ciclo de vida). A entidade não conhece nenhum tipo: pergunta à tabela do tipo.

```java
/** Aplica a ação; se o fluxo do tipo não a aceita no estado atual, lança TransicaoInvalidaException. */
public void executar(AcaoProcesso acao, Instant quando, String observacao) {
    Transicao transicao = new Transicao(estado, tipo.fluxo().proximo(estado, acao), acao, quando, observacao);
    estado = transicao.para();
    novasTransicoes.add(transicao);
}

public Set<AcaoProcesso> acoesDisponiveis() {
    return tipo.fluxo().acoesDisponiveis(estado);
}

/** Transições feitas desde que o processo foi criado ou lido; o repositório as grava no histórico ao salvar. */
public List<Transicao> novasTransicoes() {
    return List.copyOf(novasTransicoes);
}
```

Um único caso de uso atende todas as ações que só mudam o estado: `application/processo/ExecutarAcaoProcessoUseCase` recebe o id e a `AcaoProcesso`, chama `processo.executar(...)` e salva. Ação com efeito fora do agregado (pagar, que chama a tesouraria) tem o próprio caso de uso.

## Mapper

`persistence/processo/ProcessoMapper.java`. Tipo e estado são reconstruídos pelo `valueOf` do enum, sem `switch`. Valor desconhecido vindo do banco vira `IllegalStateException` com o valor na mensagem.

```java
/** Padrão: Data Mapper [PoEAA]. */
final class ProcessoMapper {

    private ProcessoMapper() {
    }

    static Processo paraDominio(ProcessoEntity entity) {
        try {
            return Processo.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), EstadoProcesso.valueOf(entity.getEstado()));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
    }

    static ProcessoEntity paraEntidade(Processo processo) {
        ProcessoEntity entity = new ProcessoEntity();
        entity.setId(processo.id());
        entity.setCotaId(processo.cotaId());
        entity.setTipo(processo.tipo().name());
        entity.setEstado(processo.estado().name());
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

## Gateway sobre o catálogo corporativo

O client e os DTOs vêm prontos de uma dependência Maven (aqui, `org.acme.sistema`). Eles ficam atrás do Gateway, que devolve um record próprio.

`gateway/tesouraria/OrdemRegistrada.java`. Record nosso, no nosso vocabulário, sem nenhuma classe do catálogo. O caso de uso pode importá-lo.

```java
public record OrdemRegistrada(String protocolo, LocalDate previsaoPagamento) {
}
```

`gateway/tesouraria/TesourariaGateway.java`:

```java
/** Padrão: Gateway [PoEAA]. */
@ApplicationScoped
public class TesourariaGateway {

    private final org.acme.sistema.TesourariaApi api; // client gerado pelo catálogo

    public TesourariaGateway(@RestClient org.acme.sistema.TesourariaApi api) {
        this.api = api;
    }

    public OrdemRegistrada registrarPagamento(Long processoId, Long cotaId) {
        org.acme.sistema.PaymentOrderOutput saida;
        try {
            saida = api.createOrder(new org.acme.sistema.PaymentOrderInput("PROCESSO-" + processoId, cotaId));
        } catch (RuntimeException e) {
            throw new SistemaExternoIndisponivelException("Tesouraria", e);
        }
        return new OrdemRegistrada(saida.getProtocolNumber(), saida.getDueDate());
    }
}
```

No caso de uso: `OrdemRegistrada ordem = tesouraria.registrarPagamento(...)`, e os dados seguem para o domínio como parâmetros (`processo.registrarPagamento(ordem.protocolo(), ordem.previsaoPagamento())`). O domínio não importa `OrdemRegistrada`.

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
