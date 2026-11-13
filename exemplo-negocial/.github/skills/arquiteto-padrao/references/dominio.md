# Domínio (`domain/<agregado>/`)

Aqui mora a regra de negócio. É Java puro: o ArchUnit falha se aparecer import de JPA, Jakarta Data, CDI, JAX-RS, Quarkus, Hibernate, MicroProfile ou de `application/`, `persistence/`, `entrypoint/` ou `gateway/`.

- A entidade de domínio protege as próprias invariantes: construtor privado e fábricas estáticas (`novo`, `reconstituir`) que validam e lançam `DominioException`. Sem setters públicos. Acessores no estilo `nome()`, sem `get`.
- **Ciclo de vida** usa State dirigido por tabela [GoF] ([exemplo](exemplos-codigo.md)), em `domain/<agregado>/fluxo/`: os estados são um `enum`, as ações são outro `enum`, e uma classe imutável guarda a tabela `(estado, ação) → próximo estado`. A entidade não decide transição: pergunta à tabela (`fluxo.proximo(estado, acao)`), que lança `TransicaoInvalidaException` quando a ação não vale no estado atual. Não crie uma classe por estado nem um método por ação na entidade.
- "Quais ações valem agora?" sai da mesma tabela (`acoesDisponiveis(estado)`); não escreva essa lista em outro lugar.
- Ação que precisa de um dado ou tem efeito fora do agregado (pagar exige o protocolo da tesouraria) ganha método próprio na entidade, que confere o dado e usa a mesma tabela.
- Toda mudança de estado gera um registro (`Transicao`: de, para, ação, quando, observação), guardado pela entidade até o repositório gravar o histórico.
- **Fluxo que muda por tipo:** cada tipo tem a sua tabela, que parte da comum e acrescenta ou troca só as linhas em que difere (`FluxoProcesso.comum().transicao(...).construir()`), em `domain/<agregado>/tipo/`. O enum do tipo guarda a tabela dele. A entidade não conhece nenhum tipo em particular: nada de `if (tipo == ...)` nem de subclasse do agregado por tipo.
- Subpastas do agregado levam o nome do conceito (`fluxo/`, `tipo/`) e dependem num sentido só: `fluxo` ← `tipo` ← raiz do agregado.
- Toda classe de padrão traz no Javadoc uma linha curta com o padrão, o papel e a fonte. Ex.: `Padrão: State dirigido por tabela [GoF].`
- Sem Bean Validation (`@NotNull`, `@NotBlank`...) aqui: o formato é checado no entrypoint; o domínio valida a regra de negócio no código e lança `DominioException`.
- Repositório é uma **interface** aqui (`domain/cota/CotaRepository`), no vocabulário do negócio. A implementação fica em `persistence/cota/`.
- **Um agregado não importa outro.** A referência é só o id (`Long clienteId` em `domain/cota/Cota`). Verificar se o outro existe é papel do caso de uso. Objeto que pertence ao agregado vira `record` (Value Object) ou lista alterada só por métodos dele.
- **Valor com regra própria** (dinheiro, CPF, período) é Value Object, e dinheiro é sempre `Dinheiro` ([valores](valores.md)).
- Se uma regra precisar de um sistema externo, declare uma interface aqui. A implementação é um `gateway/<sistema>/XGatewayAdapter.java`, que delega ao `XGateway`. O domínio nunca importa o Gateway.
- Todo comportamento novo ganha teste JUnit puro, sem Quarkus.
