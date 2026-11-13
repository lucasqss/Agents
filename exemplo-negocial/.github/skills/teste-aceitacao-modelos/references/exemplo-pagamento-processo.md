# Exemplo completo: pagamento de processo

Exemplo ilustrativo de como aplicar a skill a uma capacidade de negócio. As classes e os endpoints daqui não precisam existir no seu repositório, e nada deste exemplo é fonte de regra para outra capacidade.

Os arquivos que uma capacidade gera, a partir de `src/test/`:

| Conteúdo | Arquivo |
|---|---|
| Caso de uso, modelo, rastreabilidade, achados | `resources/aceitacao/<capacidade>/caso-de-uso.md` |
| Modelo executável | `java/<pacote-base>/aceitacao/<capacidade>/Modelo<Capacidade>.java` |
| Testes | `java/<pacote-base>/aceitacao/<capacidade>/<Capacidade>AceitacaoTest.java` |
| Par de integração | `java/<pacote-base>/aceitacao/<capacidade>/<Capacidade>AceitacaoIT.java` |
| Atalhos HTTP | `java/<pacote-base>/aceitacao/ApiAceitacao.java` |
| Sistema externo simulado | `java/<pacote-base>/aceitacao/<Sistema>Stub.java`, `<Sistema>StubResource.java` |

## 1. Do requisito às regras

O requisito diz, em linguagem de negócio, que um processo de serviço passa por aberto, em análise, aprovado e pago. O imobiliário ainda aguarda vistoria. O que não é final pode ser cancelado. Viram as regras R1 a R9 do `caso-de-uso.md`.

## 2. Do contrato aos terminais

| Terminal | Onde é coberto |
|---|---|
| `tipo`: `IMOBILIARIO`, `SERVICO` | `tipoImobiliarioAbreProcesso`, `escolhaBaseAbreProcesso` |
| 5 ações do caminho | `fluxoChegaASituacaoFinal`, `acaoEmProcessoInexistente` |
| Filtros `tipo`, `estado`, `pagina`, `tamanho` | `listagemFiltraPorTipoESituacao`, `listagemPaginada` |
| Status 400, 404, 409, 503 | `cotaAusente`, `cotaInexistente`, `acaoForaDaOrdemERecusada`, `tesourariaIndisponivel` |

## 3. Modelo: máquina de estados

`ModeloPagamentoProcesso` escreve a tabela de transições a partir do requisito e gera os casos:

- 9 caminhos, do início a cada situação final.
- 41 ações inválidas: 19 em serviço e 22 em imobiliário.

## 4. Particionamento com Base Choice

Base: cota existente e tipo serviço. Muda uma coisa por vez:

| Caso | Cota | Tipo | Esperado |
|---|---|---|---|
| Base | existente | `SERVICO` | 201 |
| Variação | existente | `IMOBILIARIO` | 201 |
| Variação | inexistente | `SERVICO` | 404 |
| Variação | ausente | `SERVICO` | 400, indicando `cotaId` |
| Variação | existente | `CONSTRUCAO` | 400 |
| Variação | existente | ausente | 400, indicando `tipo` |

## 5. Tabela de decisão: um processo em andamento por cota

| Processo anterior da mesma cota | Esperado |
|---|---|
| Em andamento, em cada situação não final | 409 |
| Finalizado, pago ou cancelado | 201 |
| Em andamento, porém de outra cota | 201 |
| Vários pedidos simultâneos na mesma cota | um 201 e os demais 409 |

## 6. O caso, do jeito certo

```java
@ParameterizedTest(name = "{0}")
@MethodSource("acoesInvalidas")
@DisplayName("Ação fora da ordem é recusada e a situação não muda")
void acaoForaDaOrdemERecusada(AcaoInvalida invalida) {
    // Pré-condição: criada pelos endpoints, com dados próprios.
    long processo = abrirProcesso(novaCota(), invalida.ate().tipo());
    invalida.ate().passos().forEach(p -> executar(processo, p.acao()).statusCode(200));

    // Entrada e resultado esperado.
    executar(processo, invalida.acao()).statusCode(409).body("mensagem", notNullValue());

    // Verificação do efeito, por outro endpoint.
    assertThat(consultarProcesso(processo).get("estado"), equalTo(invalida.ate().situacaoFinal()));
}
```

## 7. O que esse tipo de teste costuma encontrar

- **Erro fora do formato do requisito:** por exemplo, 400 sem corpo quando o requisito diz que todo erro tem o mesmo formato. O caso mantém a expectativa do requisito e o defeito segue a regra 4 da skill.
- **Concorrência:** pedidos simultâneos que furam uma regra de unicidade. Depois de corrigido, um caso como `pedidosSimultaneosAbremUmUnicoProcesso` protege a regra.
- **Perguntas ao negócio:** paginação com valores inválidos, filtros desconhecidos e campos extras. O comportamento observado não é fixado em teste.

## 8. Modelo executável

Versão reduzida, com um tipo de processo. A tabela de transições vem do requisito, e os casos são gerados a partir dela.

```java
final class ModeloPagamentoProcesso {

    private static final String TIPO = "SERVICO";
    private static final List<String> ACOES = List.of("iniciar-analise", "concluir-analise", "pagar", "cancelar");

    private static final Map<String, Map<String, String>> TRANSICOES = Map.of(
            "ABERTO", Map.of("iniciar-analise", "EM_ANALISE", "cancelar", "CANCELADO"),
            "EM_ANALISE", Map.of("concluir-analise", "APROVADO", "cancelar", "CANCELADO"),
            "APROVADO", Map.of("pagar", "PAGO", "cancelar", "CANCELADO"),
            "PAGO", Map.of(),
            "CANCELADO", Map.of());

    record Passo(String acao, String situacao) {
    }

    record Caminho(String tipo, List<Passo> passos) {
        String situacaoFinal() {
            return passos.isEmpty() ? "ABERTO" : passos.getLast().situacao();
        }
    }

    record AcaoInvalida(Caminho ate, String acao) {
    }

    private ModeloPagamentoProcesso() {
    }

    /** Todos os caminhos do início até uma situação final. */
    static Stream<Caminho> caminhosCompletos() {
        return percorrer("ABERTO", List.of()).stream()
                .filter(caminho -> TRANSICOES.get(caminho.situacaoFinal()).isEmpty());
    }

    /** Cada par situação × ação que o requisito manda recusar. */
    static Stream<AcaoInvalida> acoesInvalidas() {
        return percorrer("ABERTO", List.of()).stream()
                .collect(Collectors.toMap(Caminho::situacaoFinal, caminho -> caminho, (primeiro, outro) -> primeiro))
                .values().stream()
                .flatMap(caminho -> ACOES.stream()
                        .filter(acao -> !TRANSICOES.get(caminho.situacaoFinal()).containsKey(acao))
                        .map(acao -> new AcaoInvalida(caminho, acao)));
    }

    private static List<Caminho> percorrer(String situacao, List<Passo> passos) {
        List<Caminho> caminhos = new ArrayList<>();
        caminhos.add(new Caminho(TIPO, passos));
        for (String acao : ACOES) {
            String destino = TRANSICOES.get(situacao).get(acao);
            if (destino != null) {
                List<Passo> seguintes = new ArrayList<>(passos);
                seguintes.add(new Passo(acao, destino));
                caminhos.addAll(percorrer(destino, List.copyOf(seguintes)));
            }
        }
        return caminhos;
    }
}
```

## 9. Atalhos HTTP

Só HTTP, com os caminhos e campos do contrato. Valores únicos evitam que um caso interfira no outro.

```java
public final class ApiAceitacao {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    private ApiAceitacao() {
    }

    public static long novaCota() {
        long cliente = enviar("/clientes", Map.of("nome", "Cliente de Aceitação", "cpf", "%011d".formatted(unico())))
                .statusCode(201).extract().jsonPath().getLong("id");
        return enviar("/cotas", Map.of("numero", "COTA-" + unico(), "valorCredito", 250000, "clienteId", cliente))
                .statusCode(201).extract().jsonPath().getLong("id");
    }

    public static long abrirProcesso(long cota, String tipo) {
        return enviar("/processos", Map.of("cotaId", cota, "tipo", tipo))
                .statusCode(201).extract().jsonPath().getLong("id");
    }

    public static ValidatableResponse enviar(String caminho, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(caminho).then();
    }

    public static ValidatableResponse executar(long processo, String acao) {
        return given().when().post("/processos/%d/%s".formatted(processo, acao)).then();
    }

    /** O contrato deste exemplo não tem consulta por id: procura na listagem, página a página. */
    public static Map<String, Object> consultarProcesso(long processo) {
        for (int pagina = 1;; pagina++) {
            List<Map<String, Object>> itens = given().queryParam("pagina", pagina).queryParam("tamanho", 100)
                    .when().get("/processos").then().statusCode(200).extract().jsonPath().getList("itens");
            if (itens.isEmpty()) {
                throw new AssertionError("Processo %d não aparece na listagem".formatted(processo));
            }
            for (Map<String, Object> item : itens) {
                if (((Number) item.get("id")).longValue() == processo) {
                    return item;
                }
            }
        }
    }

    private static long unico() {
        return SEQUENCIA.incrementAndGet();
    }
}
```

## 10. Modelo de `caso-de-uso.md`

````markdown
# Caso de uso: <capacidade>

Os testes conhecem apenas estas regras e o contrato HTTP.

## Regras de negócio

| Regra | Descrição |
|---|---|
| R1 | ... |

## Contrato usado

| Operação | Entrada | Resposta |
|---|---|---|
| `POST /...` | ... | ... |

## Modelo

Tabela de transições, partições ou tabela de decisão.

## Rastreabilidade

| Regra | Modelo e critério | Teste | Casos |
|---|---|---|---|
| R1 | ... | `nomeDoTeste` | 1 |

## Sessão exploratória

Missão: ...

### Defeitos

| Regra | Tentativa | Observado |
|---|---|---|

### Perguntas

| Tentativa | Observado |
|---|---|

### Confirmado

...
````
