# Entidade que é JPA e domínio ao mesmo tempo

Quebra a entidade em domínio puro, entidade JPA, mapper, repositório do domínio e adapter. O comportamento e o **banco não mudam**: nenhuma migration, nenhuma coluna renomeada, nenhum tipo de coluna alterado.

Exemplo completo: `Cota`, `CotaRepository`, `CotaEntity`, `CotaDataRepository`, `CotaMapper` e `CotaRepositoryAdapter`. Para subtipos e estados: `ProcessoPagamento` e `ProcessoPagamentoMapper`.

## Passo 0: analise e mostre o plano ANTES de alterar qualquer arquivo

Leia a entidade e procure todos os usos dela no projeto. Mostre uma tabela com cada membro da classe atual e para onde ele vai:

| Membro atual | Tipo/anotação | Vai para | Observação |
|---|---|---|---|

Classifique tudo:

- **Campos persistidos:** tabela, coluna (`@Column`, `@JoinColumn`), tipo, nulidade, `@Id`, geração do id e `@Version`.
- **Métodos com regra de negócio:** validações, cálculos, mudanças de estado.
- **O que é só infraestrutura:** callbacks (`@PrePersist`, `@PreUpdate`), `@Transient`, `equals`/`hashCode` por id e anotações de Bean Validation.
- **Associações JPA:** `@ManyToOne`, `@OneToMany`, `@OneToOne`, `@ManyToMany`, `@Embedded` e `@ElementCollection`.
- **Lombok, herança JPA (`@Inheritance`, `@MappedSuperclass`) e enums.**
- **Usos:** quem usa a entidade hoje (use cases, resources, consultas, testes) e quais métodos de busca são chamados.

Se encontrar um dos casos abaixo, **pare e pergunte** antes de seguir:

- **Herança JPA:** separar exigiria mudar o schema.
- **Coleção grande carregada sob demanda (lazy) da qual o código depende.**
- **Callback que faz mais do que preencher data.**
- **Associação que forma ciclo entre agregados.**

## Passo 1: entidade de domínio (`domain/<Nome>.java`)

- **Java puro:** sem JPA, Jakarta Data, CDI, Bean Validation, Lombok, Quarkus nem Hibernate.
- **Construção:** construtor privado; fábrica `novo(...)` (ou `nova(...)`), sem id, para criar; `reconstituir(id, ...)`, usada só pelo mapper.
- **Invariantes:** toda regra que antes estava na entidade, em `@NotNull`/`@Size` ou em callbacks vai para o construtor ou para métodos de negócio (`alterar(...)`, verbos do negócio), lançando `DominioException`. Nada de setter público. Acessores no estilo `nome()`, sem `get`.
- **Referência a outro agregado** (`@ManyToOne Cliente cliente`) vira só o id (`Long clienteId`). A existência do outro agregado é verificada no use case, pelo repositório dele.
- **Objeto que pertence ao agregado:** um `@Embedded` vira um `record` no domínio (Value Object); filhos `@OneToMany` com cascade/orphanRemoval viram uma lista dentro do agregado, alterada só por métodos dele.
- **Enum:** fica um enum de domínio.
- **Estados fechados:** `sealed` + `record` (State), como em `ProcessoPagamento`. **Variação por tipo:** enum com Strategy (`TipoProcesso` + `FluxoProcesso`), sem subclasse por tipo.
- **Versão:** se havia `@Version`, o domínio carrega `versao` (Long) só para devolvê-la ao mapper.

## Passo 2: interface de repositório no domínio (`domain/<Nome>Repository.java`)

- Só os métodos que os **use cases de escrita** precisam: `salvar`, `buscarPorId`, `excluir` e consultas de decisão (`existeX(...)`).
- Usa só tipos do domínio.
- Buscas que só alimentam **telas** não entram aqui: vão para o `query` (record + `@Query` em `query/persistence`).

## Passo 3: entidade JPA (`<modulo>/persistence/<Nome>Entity.java`)

- **O mapeamento é exatamente o atual:** os mesmos `@Table`, `@Column`, `@Id`/geração e `@Version`.
- **Só campos, getters e setters.** Sem regra, sem construtor com lógica, sem Bean Validation e sem Lombok.
- **Sem associações JPA para outros agregados:** a coluna de FK vira um campo simples (`@Column(name = "cliente_id") Long clienteId`).
- **Filhos do agregado:** cada um vira a própria `*Entity` com a coluna de FK.
- **`@Embedded`:** as colunas ficam achatadas na entidade, ou num `@Embeddable` no mesmo pacote.
- **Enum:** `String` (ou `@Enumerated(EnumType.STRING)` se a coluna já guarda o nome); a conversão é do mapper.
- **Callbacks:** nenhum. O que eles faziam passa a ser explícito (domínio ou adapter).

## Passo 4: repositório Jakarta Data (`persistence/<Nome>DataRepository.java`)

- Interface **package-private** com `@Repository`, `@Find`, `@Insert`, `@Update` e `@Query` em HQL para deletes e contagens. Sem SQL nativo nem Panache.
- **Stateless session:** não há dirty checking, cascade nem lazy loading. Gravar é sempre explícito, e os filhos são gravados e carregados explicitamente pelo adapter.

## Passo 5: mapper (`persistence/<Nome>Mapper.java`)

- Classe `final`, package-private, métodos estáticos `paraDominio(entity)` e `paraEntidade(dominio)`, escrita à mão, sem MapStruct.
- `paraDominio` usa `reconstituir(...)`. Valor inválido vindo do banco (enum desconhecido) lança `IllegalStateException` com a mensagem do valor.
- Tipo e estado são reconstruídos por `TipoProcesso.valueOf` e `EstadoProcesso.porNome`, como em `ProcessoPagamentoMapper`, sem `switch` no mapper.

## Passo 6: adapter (`persistence/<Nome>RepositoryAdapter.java`)

- `@ApplicationScoped`, `implements <Nome>Repository`, injeção pelo construtor do `<Nome>DataRepository`.
- `salvar`: `paraEntidade`, depois `inserir` se o id for nulo ou `atualizar` se não, e devolve `paraDominio(entity)`. Com filhos: grave o agregado, depois os filhos (apague os removidos), tudo na transação do use case.

## Passo 7: atualize quem usava a entidade

- **Use cases de escrita** passam a usar `<Nome>Repository` e a entidade de domínio. Nada de `*Entity` fora de `persistence`.
- **Consultas de tela** que usavam a entidade migram para `query/persistence` (record projetado + `@Query` HQL) e `query/application`.
- **Entrypoints:** veja [entidade-na-api](entidade-na-api.md).

## Passo 8: testes

- **Domínio:** JUnit puro, `<Nome>Test`, um teste por regra movida, cobrindo o caminho feliz e a violação.
- **Mapper:** `<Nome>MapperTest`, no mesmo pacote. A ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo e versão.
- **Fluxo:** os testes de API que já existem precisam continuar passando sem alteração de contrato.

## Passo 9: verificação

Rode `./mvnw verify`. As regras mais prováveis de falhar aqui:

- DOM-001/002/006: domínio importando framework, persistência ou validação;
- SHR-002: método de negócio na `*Entity`;
- CMD-002/003: use case usando adapter ou `*Entity`;
- TEC-001: `@Entity` fora do lugar;
- TEC-002: Panache;
- NOM-005/006: nomes e lugares.

No final, entregue a tabela do Passo 0 atualizada, a lista de arquivos por camada e qualquer comportamento que **mudou sem querer** (ex.: algo que dependia de lazy loading, cascade ou callback) e como ficou explícito.
