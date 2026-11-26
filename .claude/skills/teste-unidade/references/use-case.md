# Caso de uso, Mapper e Assembler

## Caso de uso

O caso de uso orquestra: busca, chama o domínio, grava, chama o gateway. A regra está no domínio e é testada lá; aqui se prova a **orquestração**: o que é gravado, com que dados, o que é chamado e o que acontece quando uma parte falha.

Dois jeitos. **Escolha pelo que o teste precisa provar:**

| Use | Quando |
|---|---|
| **Sem Quarkus** (padrão) | Sempre que possível: roda em milissegundos e entra no teste de mutação (PITest) |
| **`@QuarkusTest` + `@InjectMock`** | Quando o caso de uso depende de configuração (`@ConfigProperty`, ex.: a alçada) ou de um bean (`Clock`) e o teste precisa provar que ela chega ao domínio |

### Sem Quarkus: repositório em memória

- **Repositório em memória**, um por agregado, em `src/test/java/.../application/<agregado>/`, implementando a interface de repositório do domínio. Conte as chamadas a `salvar`: o objeto em memória muda mesmo sem gravar, o banco não. Sem o contador, "esqueceu de gravar" passa.
- **Gateway falso** por lambda ou classe pequena, devolvendo a resposta do cenário (aceito, recusado, sem resposta).
- **Construa o caso de uso com `new`**, passando as peças. Nada de CDI.

```java
class AplicarResultadoUseCaseTest {

    private final PedidosEmMemoria repositorio = new PedidosEmMemoria();
    private final AplicarResultadoUseCase aplicar = new AplicarResultadoUseCase(repositorio);

    @Test
    void resultadoDeOutraSolicitacaoEhIgnoradoSemGravar() {
        repositorio.aguardandoResultado(1);

        assertFalse(aplicar.executar(1L, solicitacaoAntiga(), confirmado()));

        assertEquals(AGUARDANDO, repositorio.gravados.get(1L).situacao());
        assertEquals(0, repositorio.salvamentos);
    }
}
```

Modelo: `exemplo-negocial`, `application/processo/ProcessosEmMemoria.java`, `AplicarResultadoPagamentoUseCaseTest.java` e `ExecutarRotinaDePagamentoUseCaseTest.java`.

### Com a configuração de verdade: `@QuarkusTest` + `@InjectMock`

- **`@QuarkusTest` + `@Inject` do caso de uso**: o Quarkus monta o bean com a configuração de verdade (`application.properties`). Assim o teste prova também que o parâmetro de negócio (ex.: `processo.alcada`) chega ao domínio.
- **`@InjectMock` em cada porta** (interface do repositório do domínio, `Clock`, gateway): sem banco e sem sistema externo. O mock vale só para a classe de teste.
- **Confira o que foi gravado** com `ArgumentCaptor`, e o que **não** foi com `verify(..., never())`: o objeto do domínio muda mesmo sem gravar; o banco não.
- **Relógio é um bean** (`Clock`, produzido em `application/<agregado>/Relogio.java`): o teste fixa a hora e confere a data do histórico.

```java
@QuarkusTest
class ExecutarAcaoProcessoUseCaseTest {

    @Inject ExecutarAcaoProcessoUseCase useCase;
    @InjectMock ProcessoPagamentoRepository processos;
    @InjectMock Clock relogio;

    @Test
    void recusaDoDominioNaoGrava() {
        when(processos.buscarPorId(1L)).thenReturn(Optional.of(emAnalise("10.00")));
        assertThrows(TransicaoInvalidaException.class, () -> useCase.executar(1L, PAGAR, "ana", null));
        verify(processos, never()).salvar(any());
    }
}
```

Modelo: `exemplo-testes`, `application/processo/ExecutarAcaoProcessoUseCaseTest.java` e `AbrirProcessoUseCaseTest.java`. Esses testes sobem o Quarkus e ficam fora do PITest (`-Pauditoria` roda só os testes puros), para a auditoria caber em minutos.

### Casos típicos (os dois jeitos)

- O efeito foi gravado uma vez, com os dados certos (transição com autor, motivo e hora).
- Nada foi gravado quando a regra recusou; inexistente dá `NaoEncontradoException` sem gravar.
- A configuração chega ao domínio: no limite e um centavo acima (só com `@QuarkusTest`).
- Falha em um item não impede os demais; a chamada externa leva a chave de idempotência certa.
- Conflito de versão do repositório sobe para quem chamou.

O que **não** se prova aqui: transação de verdade, SQL, bloqueio e concorrência no banco. Isso é da `teste-componente`.

## Mapper e Assembler

- **Mapper** (persistência): a ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo, campos nulos permitidos e versão.
- **Assembler e padrão de canal** (apresentação): uma linha por formato (moeda, CPF mascarado, rótulo de situação), em um teste parametrizado. Um formato novo é uma linha nova.
