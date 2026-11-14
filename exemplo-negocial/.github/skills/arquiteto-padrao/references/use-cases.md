# Casos de uso (`application/<agregado>/`)

O caso de uso é o Service Layer: orquestra e define a transação. Ele **não sabe qual canal o chamou** e não formata nada.

**Uma classe, uma operação, um único método público.** `CriarCotaUseCase`, `AtualizarCotaUseCase` e `ExcluirCotaUseCase` são três classes.

A pasta é a do agregado principal da operação. Excluir uma cota verifica processos, mas é operação de cota: fica em `application/cota/`.

## Escrita

- Classe `*UseCase`, `@ApplicationScoped`, injeção por construtor, `@Transactional` no método público.
- Roteiro: buscar o agregado pelo repositório do domínio (`orElseThrow(() -> new NaoEncontradoException(...))`), chamar o método de negócio, salvar.
- A regra fica no domínio. Se o caso de uso começar a ter `if` de negócio, mova para a entidade.
- Pode usar repositórios de vários agregados: é aqui que dois agregados se encontram.
- **Regra que depende de outro agregado ou de um total:** o caso de uso só orquestra. Bloqueia a raiz dona da regra ([concorrência](concorrencia.md)), lê o total pelo repositório do domínio **dentro da mesma transação** (`processos.somarValorPagoPorCota(cotaId)`) e pede a decisão ao domínio (`cota.exigirCreditoPara(valor, total)`). Nenhum `if` da regra fica aqui.
- **Parâmetro de negócio configurável** (limite, alçada, prazo): o valor fica na configuração da aplicação. O caso de uso o lê (`@ConfigProperty`) e entrega ao domínio como Value Object (dinheiro é `Dinheiro`); o domínio não lê configuração.
  - Limite fixo no código (`if (valor > 50000)`) não entra: se é parâmetro de negócio, é configuração.
  - Se a condição de uma transição precisa do parâmetro e ele não veio, falha cedo com `IllegalStateException`, em vez de escolher um destino por padrão. Teste: sem o parâmetro, a operação falha com `IllegalStateException`, e não avança o estado. Veja `processo.alcada` no exemplo.
- Pode chamar `*Gateway`. Confira a regra do domínio **antes** da chamada externa, para que um pedido inválido nem chegue ao sistema externo. Se a chamada tem efeito lá (cria, paga, envia), siga [transação e sistema externo](transacao-e-sistema-externo.md).
- Diga o que acontece com duas requisições simultâneas e com a mesma requisição repetida: [concorrência](concorrencia.md).
- Proibido importar `*Entity`, Jakarta Data, JPA, as implementações de `persistence/` ou qualquer `*QueryRepository`.

## Rotina (sem transação)

O caso de uso que orquestra uma rotina ([saga](transacao-e-sistema-externo.md)) **não** é `@Transactional`, de propósito: a chamada ao sistema externo fica fora de transação. Ele delega a gravação a outro caso de uso transacional, de outra classe (um método público por classe; autochamada não abre transação). Falha em um item é capturada e registrada em log por item, e não impede os demais.

Precisa consultar algo para validar ou decidir a escrita?

1. Acrescente o método na interface de repositório do domínio, no vocabulário do negócio: `boolean existeProcessoAtivo(Long cotaId)`, `List<Cota> buscarPorCliente(Long clienteId)`.
2. Implemente no `XRepositoryAdapter` (método `@Query` no `XDataRepository`), devolvendo objetos de domínio.
3. Chame pela interface injetada no construtor; o CDI entrega a implementação.

Dados para **tela** (listas, detalhes, totais) não são da escrita. O endpoint de escrita devolve só o mínimo (`id`, `estado`).

## Leitura

- Injeta o `*QueryRepository` de `persistence/<agregado>/` direto, sem porta. Normaliza filtros e paginação e devolve os records projetados.
- **Não usa nada de `domain/`.**
- **Valor derivado** (disponível = limite − soma) pode ser calculado na própria query de leitura. A conta passa a existir em dois lugares **de propósito**: a leitura mostra, o método do domínio decide a escrita. Um teste de aceitação confere que os dois concordam (o disponível mostrado é o aceito pela escrita, e um centavo a mais é recusado).
