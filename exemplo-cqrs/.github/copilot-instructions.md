# Instruções para o Copilot

Serviço Java 25 + Quarkus 3.33 com **CQRS**, **domínio puro** e os padrões do *Patterns of Enterprise Application Architecture* (Fowler), em **módulos Maven**: um núcleo de negócio e **uma aplicação por canal** (web, mobile, TAA). A arquitetura é verificada a cada build pelo Maven (um módulo só enxerga o que declara no pom) e por testes ArchUnit no módulo `arquitetura`. Código fora destes combinados **não compila ou não passa nos testes**.

## Antes de terminar qualquer tarefa
1. Rode `./mvnw verify` na raiz. Ele compila todos os módulos (o HQL das consultas é validado aqui), roda os testes de domínio, de canal, ArchUnit e integração.
2. **Se uma regra ArchUnit falhar, corrija o código, nunca a regra.** Não edite, apague nem afrouxe nada em `arquitetura/`; essa pasta tem dono (CODEOWNERS). Se achar que a regra está errada, diga isso na resposta e pare. A falha traz um ID (ex.: `DOM-002`): procure-o em `arquitetura/src/test/java/**/arquitetura/` e na tabela de regras do README e siga o "Corrija:" da mensagem.
3. Não edite migrations já existentes em `nucleo/src/main/resources/db/migration/`. Mudança de schema é sempre um arquivo novo `V<n+1>__descricao.sql`, compatível com a versão anterior dos canais (expand/contract).
4. **Nunca** adicione dependência de canal (`canal-*`) no `nucleo`, nem dependência entre dois canais. Dependência corporativa de um canal entra só no `pom.xml` daquele canal.

## Módulos e onde cada coisa mora

```
nucleo/                         NÚCLEO (jar, sem HTTP) - nunca conhece canal; cada camada é agrupada por TIPO
  command/                      ESCRITA
    application/
      usecase/                  *UseCase: transação (@Transactional) e orquestração
      exception/                NaoEncontradoException (404), ConflitoException (409)
      dto/                      dados de entrada dos use cases (records sem anotação)
    domain/                     regras de negócio em Java puro
      entity/                   agregados (Cliente, Cota); agregado com várias classes ganha subpacote:
        pagamento/              ProcessoPagamento e subtipos
          state/                estados (padrão State, sealed)
      repository/               interfaces de repositório (XRepository)
      exception/                DominioException (400), TransicaoInvalidaException (409)
      enums/                    TipoProcesso, CanalDeAtendimento (regra que muda por canal)
    persistence/
      repository/               *DataRepository: Jakarta Data (@Find/@Insert/@Update/@Query HQL)
      mapper/                   *Mapper: domínio <-> *Entity, escritos à mão
      adapter/                  *RepositoryAdapter: implementa XRepository usando repository + mapper
  query/                        LEITURA (nunca importa command)
    application/
      usecase/                  use cases de leitura (filtros, paginação)
      dto/                      Pagina
    persistence/
      repository/               *QueryRepository: Jakarta Data com @Query HQL
      projection/               records projetados (read models)
  shared/
    persistence/                entidades JPA (*Entity): só campos, getters e setters
    gateway/                    SistemaExternoIndisponivelException
      <sistema>/                *Gateway + client/ (REST + resiliência) + dto/ (contrato do sistema externo)
    identidade/                 Solicitante: quem chamou, igual para todos os canais
  resources/db/migration/       migrations Flyway (um banco para todos os canais)
canal-comum/                    BASE DOS CANAIS (jar)
  canal/comum/apresentacao/     Formatador, Rotulos, PadraoApresentacao
  canal/comum/erro/             ErroResponse, ExceptionMappers (exceção -> HTTP), IdCriado
  canal/comum/seguranca/        FiltroDeToken, MapeadorDeToken, SolicitanteDaRequisicao
canal-<canal>/                  UMA APLICAÇÃO POR CANAL (web, mobile, taa): deps corporativas do canal no pom
  canal/<canal>/entrypoint/     *Resource (/<canal>/v1), *Request, *Response (contrato), *Assembler
  canal/<canal>/apresentacao/   Padrao<Canal>
  canal/<canal>/seguranca/      MapeadorDeToken<Canal>: token do canal -> Solicitante
arquitetura/                    só testes: regras ArchUnit sobre todos os módulos
```

## Perguntas frequentes: "onde eu coloco...?"
| Preciso de... | Coloque em |
|---|---|
| Uma regra de negócio, validação ou transição de estado | `nucleo`, `command/domain/entity`, na entidade de domínio. Nunca no use case, no resource nem na entidade JPA. |
| Uma regra de negócio que **muda por canal** (o app não abre X, o terminal não escreve) | `CanalDeAtendimento` (`command/domain/enums`); o use case recebe o `Solicitante` e chama a regra. Nunca um `if (canal)` no Resource. |
| Consultar algo para validar ou decidir uma escrita (existe? está ativo?) | método novo na interface de repositório do domínio (`command/domain/repository`), implementado no `*RepositoryAdapter` (`command/persistence/adapter`) com um `@Query` no `*DataRepository` (`command/persistence/repository`). Nunca um repositório do query. |
| Validar o formato da entrada (obrigatório, tamanho, faixa) | `XRequest` (record) no `entrypoint` do canal, com Bean Validation (`@NotBlank`, `@Size` igual à coluna, `@Positive`) e `@Valid @NotNull` no parâmetro do resource. Cada canal tem o seu Request. |
| Gravar algo | um use case em `command/application/usecase` (núcleo) e o endpoint no canal que escreve. Canal `@SomenteLeitura` não grava. |
| Uma tela/listagem nova | record em `query/persistence/projection` + `@Query` HQL em `query/persistence/repository`, use case em `query/application/usecase` (núcleo) e Resource/Response/Assembler no canal |
| Chamar um sistema de outra área | um `*Gateway` em `shared/gateway/<sistema>/`. Só **use cases** chamam gateways. |
| Formatar para uma tela (moeda, máscara, rótulo) | o Assembler do canal, usando `Padrao<Canal>` |
| Um número com significado de negócio (total pago, saldo) | uma consulta em `query/persistence/repository`, não o Assembler |
| Saber quem chamou | `SolicitanteDaRequisicao.atual()` no Resource, passado ao use case |
| Uma dependência corporativa de canal (segurança, log, utilitário) | `pom.xml` do `canal-<canal>`. Nunca no `nucleo` nem no `canal-comum`. |
| Mudar o banco | nova migration Flyway (expand/contract) + campo na `*Entity` + mapper (escrita) e/ou HQL (leitura) |

## Proibido
- O `nucleo` depender de `canal-*`, e um canal depender de outro (o pom nem permite; não "conserte" adicionando a dependência).
- `jakarta.validation` (Bean Validation), `@Path` e `@ServerExceptionMapper` fora dos módulos de canal.
- `command.domain` importar JPA, Jakarta Data, CDI (`@Inject`, `@ApplicationScoped`), JAX-RS, Bean Validation, Quarkus, Hibernate, MicroProfile, `shared` ou `canal`.
- `query` importar qualquer coisa de `command`, e `command` importar qualquer coisa de `query` (CQRS nos dois sentidos).
- Use case importar JPA, Jakarta Data, `*Entity` ou `command/persistence`.
- Classe solta direto numa camada do núcleo (ex.: em `command/domain`): ela vai para o subpacote do tipo dela.
- Usar `*Mapper` ou `*DataRepository` fora do `*RepositoryAdapter`.
- Canal acessar banco (repositório, `*Entity`, JPA) ou devolver record de `persistence`; ele devolve o próprio `*Response`.
- Canal marcado com `@SomenteLeitura` usar `command`.
- Panache. A persistência é só Jakarta Data, com HQL validado na compilação; gravar é sempre explícito (`@Insert`/`@Update`), sem dirty checking.
- Entidade JPA (`*Entity`) com qualquer método além de getter/setter.
- Lombok, MapStruct, `@Query(nativeQuery)` ou SQL nativo. Mappers são escritos à mão; consultas são HQL validado na compilação.
- Herança JPA. Uma tabela por agregado; subtipos são reconstruídos pelo mapper.
- DTO ou client de sistema externo fora de `shared/gateway/<sistema>/`. Gateway nunca importa o domínio.
- `@RegisterRestClient`, `@Timeout`, `@Retry`, `@CircuitBreaker` fora de `shared/gateway`.
- Renomear ou remover campo de um `*Response` de canal: o front low-code quebra. Só mudanças aditivas; quebra vira `/v2`.

## Estilo
- Java moderno: `record` para dados imutáveis, `sealed` + `switch` com pattern matching onde há estados fechados.
- Injeção por construtor. Assemblers e `Padrao*` são classes puras, sem CDI.
- Exceções: `DominioException` (400), `TransicaoInvalidaException`/`ConflitoException` (409), `NaoEncontradoException` (404), `SistemaExternoIndisponivelException` (503). Token ausente ou inválido: 401.
- Nomes em português no domínio e nos use cases, seguindo o vocabulário já existente.

## Testes esperados em cada mudança
- Regra de negócio: teste JUnit puro em `nucleo/src/test/java/**/command/domain/...`, no mesmo subpacote da classe (ex.: `entity`), sem Quarkus.
- Endpoint novo: `@QuarkusTest` + RestAssured no módulo do canal, com o token do canal. Conferir os **nomes exatos dos campos** e os textos formatados (veja `ContratoWebTest`, `ContratoMobileTest`, `ContratoTaaTest`).
- Formato novo de canal: linha nova no `Padrao<Canal>Test` de cada canal.
- Sistema externo: nunca chamar o real nos testes; use um Service Stub (veja `TesourariaFake` no canal web).
