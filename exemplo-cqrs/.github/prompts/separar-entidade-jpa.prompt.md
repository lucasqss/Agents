---
mode: agent
description: Quebra uma entidade que hoje é JPA e domínio ao mesmo tempo em entidade de domínio pura, entidade JPA, mapper, repositório do domínio e adapter, sem mudar o banco.
---
Refatore a entidade abaixo seguindo `.github/copilot-instructions.md` e o exemplo completo da `Cota` no `nucleo`:
`command/domain/Cota.java`, `command/domain/CotaRepository.java`, `shared/persistence/CotaEntity.java`,
`command/persistence/CotaDataRepository.java`, `command/persistence/CotaMapper.java` e `command/persistence/CotaRepositoryAdapter.java`.
Para subtipos e estados, o exemplo é o `ProcessoPagamento` e o `ProcessoPagamentoMapper`.

Entidade atual: ${file}
Observações: ${input:observacoes:Opcional. Ex.: "o campo situacao é calculado", "não mexer no método X ainda"}

Isto é uma **refatoração**: o comportamento e o **banco não mudam**. Nenhuma migration, nenhuma coluna renomeada, nenhum tipo de coluna alterado.

## Passo 0: analise e me mostre o plano ANTES de alterar qualquer arquivo

Leia a entidade e procure todos os usos dela no projeto. Depois me mostre uma tabela com cada membro da classe atual e para onde ele vai:

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

## Passo 1: entidade de domínio (`nucleo`, `command/domain/<Nome>.java`)

- **Java puro:** sem JPA, Jakarta Data, CDI, Bean Validation, Lombok, Quarkus nem Hibernate. Ela não importa nada de `shared`.
- **Construção:**
  - construtor privado;
  - fábrica `novo(...)` (ou `nova(...)`), sem id, para criar;
  - `reconstituir(id, ...)`, usada só pelo mapper.
- **Invariantes:**
  - toda regra que antes estava na entidade, em `@NotNull`/`@Size` ou em callbacks vai para o construtor ou para métodos de negócio (`alterar(...)`, verbos do negócio), lançando `DominioException`;
  - nada de setter público;
  - acessores no estilo `nome()`, sem `get`.
- **Referência a outro agregado** (`@ManyToOne Cliente cliente`) vira só o id (`Long clienteId`). A existência do outro agregado é verificada no use case, pelo repositório dele.
- **Objeto que pertence ao agregado** (`@Embedded`, ou um filho `@OneToMany` com cascade/orphanRemoval):
  - um `@Embedded` vira um `record` no domínio (Value Object);
  - os filhos viram uma lista dentro do agregado, alterada só por métodos dele.
- **Enum:** fica um enum de domínio, em `command/domain`.
- **Estados ou subtipos fechados:** `sealed`, como em `ProcessoPagamento`.
- **Versão:** se havia `@Version`, o domínio carrega `versao` (Long) só para devolvê-la ao mapper.

## Passo 2: interface de repositório no domínio (`command/domain/<Nome>Repository.java`)

- Só os métodos que os **use cases de escrita** precisam: `salvar`, `buscarPorId`, `excluir` e consultas de decisão (`existeX(...)`).
- Usa só tipos do domínio.
- Buscas que só alimentam **telas** não entram aqui: elas vão para o `query` (record + `@Query` em `query/persistence`).

## Passo 3: entidade JPA (`nucleo`, `shared/persistence/<Nome>Entity.java`)

- **O mapeamento é exatamente o atual:** os mesmos `@Table`, `@Column`, `@Id`/geração e `@Version`.
- **Só campos, getters e setters.** Sem regra, sem construtor com lógica, sem Bean Validation e sem Lombok.
- **Sem associações JPA para outros agregados:** a coluna de FK vira um campo simples (`@Column(name = "cliente_id") Long clienteId`).
- **Filhos do agregado:** cada um vira a própria `*Entity` com a coluna de FK.
- **`@Embedded`:** as colunas ficam achatadas na entidade, ou num `@Embeddable` em `shared/persistence`.
- **Enum:** `String` (ou `@Enumerated(EnumType.STRING)` se a coluna já guarda o nome); a conversão é do mapper.
- **Callbacks:** nenhum. O que eles faziam passa a ser explícito (domínio ou adapter).

## Passo 4: repositório Jakarta Data (`command/persistence/<Nome>DataRepository.java`)

- Interface **package-private** com `@Repository`, `@Find`, `@Insert`, `@Update` e `@Query` em HQL para deletes e contagens. Sem SQL nativo nem Panache.
- **Stateless session:** não há dirty checking, cascade nem lazy loading. Gravar é sempre `@Insert`/`@Update` explícito, e os filhos são gravados e carregados explicitamente pelo adapter.

## Passo 5: mapper (`command/persistence/<Nome>Mapper.java`)

- Classe `final`, package-private, métodos estáticos `paraDominio(entity)` e `paraEntidade(dominio)`, escrita à mão, sem MapStruct.
- `paraDominio` usa `reconstituir(...)`. Valor inválido vindo do banco (enum desconhecido) lança `IllegalStateException` com a mensagem do valor.
- Subtipos e estados são reconstruídos com `switch`, como em `ProcessoPagamentoMapper`.

## Passo 6: adapter (`command/persistence/<Nome>RepositoryAdapter.java`)

- `@ApplicationScoped`, `implements <Nome>Repository`, injeção pelo construtor do `<Nome>DataRepository`.
- `salvar`: `paraEntidade`, depois `inserir` se o id for nulo ou `atualizar` se não, e devolve `paraDominio(entity)`. Com filhos: grave o agregado, depois os filhos (apague os removidos), tudo na transação do use case.

## Passo 7: atualize quem usava a entidade

- **Use cases de escrita** passam a usar `<Nome>Repository` e a entidade de domínio. Nada de `*Entity` fora de `command/persistence` e `shared/persistence`.
- **Consultas de tela** que usavam a entidade migram para `query/persistence` (record projetado + `@Query` HQL) e `query/application`.
- **Canais:** os Resources que recebiam ou devolviam a entidade passam a usar `*Request`/`*Response` próprios. O formato da entrada (as antigas `@NotNull`/`@Size` da entidade) vai para o `*Request` do canal, com `@Size` igual ao tamanho da coluna.

## Passo 8: testes

- **Domínio:** JUnit puro em `nucleo/src/test/java/**/command/domain/<Nome>Test.java`, um teste por regra movida, cobrindo o caminho feliz e a violação.
- **Mapper:** `nucleo/src/test/java/**/command/persistence/<Nome>MapperTest.java`, no mesmo pacote. A ida e volta `paraEntidade(paraDominio(entity))` preserva todos os campos, inclusive enum, subtipo e versão.
- **Fluxo:** os testes de API que já existem precisam continuar passando sem alteração de contrato.

## Passo 9: verificação

1. Rode `./mvnw verify` na raiz. O HQL é validado na compilação.
2. Se uma regra ArchUnit falhar, corrija o código, nunca a regra. As mais prováveis aqui:
   - DOM-001/002/006: domínio importando framework, persistência ou validação;
   - SHR-002: método de negócio na `*Entity`;
   - CMD-002/003: use case usando adapter ou `*Entity`;
   - TEC-001: `@Entity` fora de `shared.persistence`;
   - TEC-002: Panache;
   - NOM-005/006: nomes e lugares.
3. No final, me entregue:
   - a tabela do Passo 0 atualizada com o que foi feito;
   - a lista de arquivos criados, alterados e removidos, por camada;
   - qualquer comportamento que **mudou sem querer** (ex.: algo que dependia de lazy loading, de cascade ou de callback) e como ficou explícito.
