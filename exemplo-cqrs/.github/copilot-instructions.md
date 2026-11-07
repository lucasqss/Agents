# Instruções para o Copilot

Serviço Java 25 + Quarkus 3.33 com **CQRS**, **domínio puro** e os padrões do *Patterns of Enterprise Application Architecture* (Fowler). A arquitetura é verificada a cada build por testes ArchUnit em `src/test/java/**/arquitetura/`. Código fora destes combinados **não compila nos testes**.

## Antes de terminar qualquer tarefa
1. Rode `./mvnw verify`. Ele compila (o HQL das consultas é validado aqui), roda os testes de domínio, ArchUnit, contrato e integração.
2. **Se uma regra ArchUnit falhar, corrija o código, nunca a regra.** Não edite, apague nem afrouxe nada em `src/test/java/**/arquitetura/`; essa pasta tem dono (CODEOWNERS). Se achar que a regra está errada, diga isso na resposta e pare.
3. Não edite migrations já existentes em `src/main/resources/db/migration/`. Mudança de schema é sempre um arquivo novo `V<n+1>__descricao.sql`.

## Onde cada coisa mora

```
command/                     ESCRITA
  entrypoint/                REST de escrita + ExceptionMappers (exceção -> HTTP)
  application/               use cases (*UseCase): transação (@Transactional) e orquestração
  domain/                    regras de negócio em Java puro + interfaces de repositório
  persistence/               Panache + Data Mappers escritos à mão (implementam as interfaces do domínio)
query/                       LEITURA (nunca importa command)
  entrypoint/                REST neutro (sem formatação)
    <canal>/                 web/, mobile/, taa/: Resource + Response (contrato) + Assembler
  application/               use cases de leitura (filtros, paginação)
  persistence/               repositórios Jakarta Data com @Query HQL + records projetados
shared/
  persistence/               entidades JPA (*Entity): só campos, getters e setters
  gateway/<sistema>/         sistemas externos: *Gateway + REST client + DTOs externos + resiliência
  apresentacao/              Formatador, Rotulos e um Padrao<Canal> por canal
```

## Perguntas frequentes: "onde eu coloco...?"
| Preciso de... | Coloque em |
|---|---|
| Uma regra de negócio, validação ou transição de estado | `command/domain`, na entidade de domínio. Nunca no use case, no resource nem na entidade JPA. |
| Consultar algo para validar ou decidir uma escrita (existe? está ativo?) | método novo na interface de repositório do domínio (`command/domain`), implementado em `command/persistence`. Nunca um repositório do query. |
| Gravar algo | um use case em `command/application` que carrega o objeto de domínio pelo repositório, chama o método de negócio e salva |
| Uma tela/listagem nova | um record + um método `@Query` HQL em `query/persistence`, um use case em `query/application` e um resource |
| Chamar um sistema de outra área | um `*Gateway` em `shared/gateway/<sistema>/`. Só **use cases** chamam gateways. |
| Formatar para uma tela (moeda, máscara, rótulo) | o Assembler do canal em `query/entrypoint/<canal>/`, usando `Padrao<Canal>` |
| Um número com significado de negócio (total pago, saldo) | uma consulta em `query/persistence`, não o Assembler |
| Mudar o banco | nova migration Flyway + campo na `*Entity` + mapper (escrita) e/ou HQL (leitura) |

## Proibido
- `command.domain` importar JPA, Jakarta Data, CDI (`@Inject`, `@ApplicationScoped`), JAX-RS, Quarkus, Hibernate, MicroProfile ou `shared`.
- `query` importar qualquer coisa de `command`, e `command` importar qualquer coisa de `query` (CQRS nos dois sentidos).
- Use case importar Panache, JPA, `*Entity` ou `command/persistence`.
- Entidade JPA (`*Entity`) com qualquer método além de getter/setter.
- Lombok, MapStruct, `@Query(nativeQuery)` ou SQL nativo. Mappers são escritos à mão; consultas são HQL validado na compilação.
- Herança JPA. Uma tabela por agregado; subtipos são reconstruídos pelo mapper.
- DTO ou client de sistema externo fora de `shared/gateway/<sistema>/`. Gateway nunca importa o domínio.
- `@RegisterRestClient`, `@Timeout`, `@Retry`, `@CircuitBreaker` fora de `shared/gateway`.
- Resource de canal devolver record de `persistence`; ele devolve o próprio `*Response`.
- Canal usar o `Padrao` de outro canal; formatação fora do entrypoint.
- Renomear ou remover campo de um `*Response` de canal: o front low-code quebra. Só mudanças aditivas; quebra vira `/v2`.

## Estilo
- Java moderno: `record` para dados imutáveis, `sealed` + `switch` com pattern matching onde há estados fechados.
- Injeção por construtor. Assemblers e `Padrao*` são classes puras, sem CDI.
- Exceções: `DominioException` (400), `TransicaoInvalidaException`/`ConflitoException` (409), `NaoEncontradoException` (404), `SistemaExternoIndisponivelException` (503).
- Nomes em português no domínio e nos use cases, seguindo o vocabulário já existente.

## Testes esperados em cada mudança
- Regra de negócio: teste JUnit puro em `src/test/java/**/command/domain/`, sem Quarkus.
- Endpoint novo: `@QuarkusTest` + RestAssured. Em canal, conferir os **nomes exatos dos campos** e os textos formatados (veja `CanaisContratoTest`).
- Formato novo de canal: linha nova em `PadroesDeCanalTest`.
- Sistema externo: nunca chamar o real nos testes; use um Service Stub (veja `TesourariaFake`).
