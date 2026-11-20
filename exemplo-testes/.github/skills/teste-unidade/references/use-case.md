# Caso de uso, Mapper e Assembler

## Caso de uso: `@QuarkusTest` com `@InjectMock`

O caso de uso orquestra: busca, chama o domínio, grava. A regra está no domínio e é testada lá; aqui se prova a **orquestração**: o que é gravado, com que dados, e o que acontece quando uma parte falha.

- **`@QuarkusTest` + `@Inject` do caso de uso**: o Quarkus monta o bean com a configuração de verdade (`application.properties`). Assim o teste prova também que o parâmetro de negócio (ex.: `processo.alcada`) chega ao domínio.
- **`@InjectMock` em cada porta** (interface do repositório do domínio, `Clock`, gateway): sem banco e sem sistema externo. O mock vale só para a classe de teste.
- **Confira o que foi gravado** com `ArgumentCaptor` e o que **não** foi com `verify(..., never())`: o objeto do domínio muda mesmo sem gravar; o banco não. Sem essa conferência, "esqueceu de gravar" passa.
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

Casos típicos: inexistente dá `NaoEncontradoException` sem gravar; a configuração chega ao domínio (no limite e um centavo acima); a gravação leva a transição com autor, motivo e hora; recusa do domínio não grava; conflito de versão do repositório sobe para quem chamou.

Modelo no exemplo: `application/processo/ExecutarAcaoProcessoUseCaseTest.java` e `AbrirProcessoUseCaseTest.java`.

O que **não** se prova aqui: transação de verdade, SQL, bloqueio e concorrência no banco. Isso é da `teste-componente`.

O PITest (`-Pauditoria`) roda só os testes puros do domínio: os de use case sobem o Quarkus e ficam fora, para a auditoria caber em minutos.

## Mapper e Assembler

- **Mapper** (persistência): a ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, campos nulos permitidos e versão.
- **Assembler e padrão de canal** (apresentação): uma linha por formato, em um teste parametrizado. Um formato novo é uma linha nova.
