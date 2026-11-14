# Caso de uso, Mapper e Assembler sem Quarkus

## Caso de uso

O caso de uso orquestra: busca, chama o domínio, grava, chama o gateway. A regra está no domínio e é testada lá; aqui se prova a **orquestração**: o que é gravado, o que é chamado, o que acontece quando uma parte falha.

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

Casos típicos: o efeito foi gravado uma vez; nada foi gravado quando a regra recusou; falha em um item não impede os demais; a chamada externa leva a chave de idempotência certa.

Modelo no exemplo: `application/processo/ProcessosEmMemoria.java`, `AplicarResultadoPagamentoUseCaseTest.java` e `ExecutarRotinaDePagamentoUseCaseTest.java`.

O que **não** se prova aqui: transação de verdade, bloqueio no banco e concorrência. Isso é da `teste-componente`.

## Mapper e Assembler

- **Mapper** (persistência): a ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo, campos nulos permitidos e versão.
- **Assembler e padrão de canal** (apresentação): uma linha por formato (moeda, CPF mascarado, rótulo de situação), em um teste parametrizado. Um formato novo é uma linha nova.
