# Consórcio CQRS: exemplo de referência

Projeto didático para validar uma arquitetura CQRS com domínio puro em Quarkus, aplicada a processos de pagamento de consórcio. Ele não implementa o negócio completo.

**Stack:** Java 25, Quarkus 3.33, Hibernate ORM com Jakarta Data (Hibernate Data Repositories) na escrita e na leitura, PostgreSQL (Dev Services), Flyway, JUnit 5, RestAssured e ArchUnit. Build Maven multi-módulo: um núcleo de negócio e **uma aplicação por canal**.

## Arquitetura

Hexagonal/clean **pragmática**, com os padrões do *Patterns of Enterprise Application Architecture* (Fowler). Só existe uma pasta quando o problema pede, e só há interface (porta) onde ela protege alguma coisa.

### Módulos: um núcleo, uma aplicação por canal

Cada canal (web, mobile, TAA) exige **dependências corporativas próprias** (segurança com o token do canal, utilitários, observabilidade), tem **ritmo de mudança e carga diferentes** e uma **rota própria no API gateway**. Por isso cada canal é um módulo Maven e um deployable; o negócio fica num núcleo que nenhum canal contamina.

```
pom.xml            parent + agregador: ./mvnw verify constrói e testa tudo
nucleo/            jar, SEM HTTP: command, query, shared (persistência, gateways, identidade) e as migrations
canal-comum/       jar: base de todos os canais (formato de erro, filtro de token, apresentação comum, @SomenteLeitura)
canal-web/         aplicação do web (backoffice): lê e escreve        rota /web/     porta dev 8081
canal-mobile/      aplicação do app: lê e abre processo de serviço    rota /mobile/  porta dev 8082
canal-taa/         aplicação do terminal: só lê (@SomenteLeitura)     rota /taa/     porta dev 8083
arquitetura/       só testes: regras ArchUnit sobre todos os módulos juntos
```

- **Isolamento garantido pelo Maven.** O `nucleo` não depende de nenhum canal e um canal não depende de outro: um import errado **nem compila**. A dependência corporativa do mobile só existe no `pom.xml` do `canal-mobile`, então nunca chega ao web nem ao núcleo.
- **Um deployable por canal.** Cada canal sobe, escala e é liberado no próprio ritmo. Como o filtro de token de um canal só existe na aplicação dele, os tokens não se misturam.
- **Mesmo time, mesmo repositório, um build.** Mudar uma regra do núcleo e os canais afetados é um PR só, testado de uma vez.
- **Quando NÃO fazer isso:** se os canais não tivessem dependências próprias nem ritmos diferentes, um módulo único com `canal/<canal>` como pacote (e o ArchUnit garantindo a fronteira) bastaria.

Pacote raiz `br.com.claudio.consorcio`:

```
nucleo
  shared/
    persistence/          entidades JPA: modelo do NOSSO banco, só mapeamento, getters e setters
    gateway/              SistemaExternoIndisponivelException (comum a todos os sistemas)
      <sistema>/          *Gateway (o único visível fora) + client/ (REST + resiliência) + dto/ (contrato deles)
    identidade/           Solicitante: quem chamou, na forma comum a todos os canais
  command/
    domain/               Domain Model puro, agrupado por tipo:
      entity/             Cliente, Cota; agregado com várias classes ganha subpacote:
        pagamento/        ProcessoPagamento, ProcessoImobiliario, ProcessoServico
          state/          EstadoProcesso (sealed) + Aberto, EmAnalise, AguardandoVistoria, Aprovado, Pago, Cancelado
      repository/         interfaces de repositório (implementadas em persistence/adapter)
      exception/          DominioException, TransicaoInvalidaException
      enums/              TipoProcesso, CanalDeAtendimento
    application/
      usecase/            use cases (Service Layer): transação e orquestração; chamam repositórios e gateways
      exception/          NaoEncontradoException, ConflitoException
      dto/                dados de entrada dos use cases (DadosCliente, DadosCota)
    persistence/
      repository/         *DataRepository: Jakarta Data (HQL validado na compilação)
      mapper/             *Mapper: domínio <-> JPA, escritos à mão
      adapter/            *RepositoryAdapter: implementa a interface do domínio com o repository e o mapper
  query/
    application/
      usecase/            use cases de leitura (normalização de filtros, paginação)
      dto/                Pagina
    persistence/
      repository/         *QueryRepository: Jakarta Data com @Query HQL
      projection/         records projetados (read models)
canal-comum
  canal/comum/            apresentacao/ (Formatador, Rotulos), erro/ (ErroResponse, ExceptionMappers),
                          seguranca/ (FiltroDeToken, MapeadorDeToken), SomenteLeitura
canal-<canal>
  canal/<canal>/          entrypoint/ (Resource, Request, Response, Assembler), apresentacao/ (Padrao<Canal>),
                          seguranca/ (MapeadorDeToken<Canal>)
```

- **Por tipo dentro de cada camada:** cada camada do núcleo tem subpacotes fixos por tipo de classe (entity, repository, exception, enums, usecase, dto, mapper, adapter, projection), e uma classe solta direto na camada quebra o build (DOM-008, CMD-009/010, QRY-004/005). Mapper e repositório Jakarta Data da escrita são públicos por estarem em pacotes separados, mas só o adapter pode usá-los (CMD-011/012).
- **Escrita:** `canal → application → domain ← persistence`. A persistência implementa as interfaces que o domínio declara (Separated Interface), então o use case não vê JPA. Quando a escrita precisa consultar algo (existe? está ativo?), o método entra nessa interface do domínio; nunca num repositório do query.
- **Leitura:** `canal → application → persistence`. Não há domínio para proteger, então também não há porta. O repositório Jakarta Data já é uma interface, com a implementação gerada, e projeta direto no record.

### Token por canal e identidade comum

Cada canal tem o próprio token, lido pela biblioteca corporativa daquele canal. O núcleo não sabe como era o token: ele recebe sempre o mesmo `Solicitante(id, canal)`.

1. `FiltroDeToken` (`canal-comum`) exige `Authorization: Bearer <token>` nos caminhos do canal (`/web/`, `/mobile/`, `/taa/`) e responde **401** sem token válido.
2. O `MapeadorDeToken<Canal>` do módulo do canal traduz o token para o `Solicitante`. Neste exemplo os tokens são fictícios: `web:<matrícula>`, `mob.<cpf>`, `taa-<terminal>`.
3. O Resource passa `SolicitanteDaRequisicao.atual()` ao use case.

### Regras que mudam por canal

Os canais que escrevem seguem o **mesmo fluxo** (o mesmo use case); o que muda são algumas validações:
- **Formato** (obrigatório, tamanho) fica no `*Request` de cada canal, com Bean Validation. Cada canal tem o seu.
- **Regra de negócio por canal** fica no **domínio**, em `CanalDeAtendimento`: o web abre qualquer processo, o mobile só `SERVICO` (400 para `IMOBILIARIO`), o TAA nenhum. O use case recebe o `Solicitante` e chama a regra. Nada de `if (canal)` no Resource, e a regra é testada sem HTTP (`CanalDeAtendimentoTest`).
- **Escrita bloqueada** é estrutural: o `package-info.java` do canal leva `@SomenteLeitura`, e a regra CAN-010 faz o build falhar se o canal usar qualquer coisa de `command`.

### Um banco, vários canais

Todos os canais usam o mesmo banco. As migrations ficam no `nucleo` e cada aplicação roda o Flyway ao subir; o lock do Flyway serializa canais subindo ao mesmo tempo. Como os canais sobem em **versões diferentes**, toda migration segue **expand/contract**: primeiro acrescenta (coluna nova anulável, tabela nova), os canais migram no ritmo deles, e só depois uma migration remove o que ficou sem uso. Nunca renomeie nem apague numa migration só.

### Persistência com Jakarta Data nos dois lados

Escrita e leitura usam repositórios Jakarta Data: toda consulta é um `@Query` em HQL, **validado na compilação** pelo Hibernate Processor, inclusive as consultas de existência da escrita (`existeNumero`, `existeAtivoPorCota`).

O Jakarta Data do Hibernate usa **stateless session**: não há *dirty checking*. Alterar um objeto carregado não grava nada; gravar é sempre explícito. Por isso o `XRepositoryAdapter` monta a entidade a partir do domínio (`XMapper.paraEntidade`) e chama `inserir` (id nulo) ou `atualizar`. No nosso desenho isso já era assim, porque o domínio nunca toca na entidade JPA.

### Dois tipos de infraestrutura

| | Nosso banco | Sistemas externos (corporativos) |
|---|---|---|
| Modelo | nosso, versionado por migrations do Flyway | deles; muda sem aviso |
| Onde fica | `command.persistence` / `query.persistence` + entidades em `shared.persistence` | `shared.gateway.<sistema>`, atrás de um **Gateway** |
| Acoplamento | a escrita mapeia para o domínio; a leitura projeta direto do banco | o Gateway traduz; client e DTOs externos não saem do pacote |
| Transação | a mesma do use case | fora do nosso controle (veja o trade-off abaixo) |
| Falha | rara, vira 500 | esperada: timeout, retry e circuit breaker no client; vira `SistemaExternoIndisponivelException` (503) |
| Teste | PostgreSQL real (Dev Services) | **Service Stub** (`TesourariaFake`), nunca o sistema real |
| Config | datasource (`DB_*`); permite réplica de leitura no futuro | `quarkus.rest-client.<sistema>.*` (`TESOURARIA_URL`) |

Só os use cases chamam sistemas externos, por isso eles usam o Gateway direto, sem porta. Se um dia uma regra do **domínio** precisar de um sistema externo, aí sim o domínio declara uma interface e a implementação delega ao Gateway.

**Exemplo: a Tesouraria.** Ao pagar, `AvancarProcessoUseCase` faz a transição primeiro (assim uma transição inválida nem chega à tesouraria), depois chama `TesourariaGateway.registrarPagamento` e guarda o protocolo no processo. Se a tesouraria falhar, a exceção desfaz a transação e o processo continua APROVADO. Trade-off: a chamada externa acontece dentro da transação do banco. Em produção, quando isso pesar, use um outbox.

### Padrões (PoEAA)

Service Layer (use cases), Domain Model (`command.domain`), Repository + Data Mapper (`command.persistence.adapter` e `.mapper`), Separated Interface (repositórios do domínio), Gateway e Service Stub (integrações). Num módulo que só orquestra, sem regra de negócio de verdade, um **Transaction Script** no use case basta, sem pasta `domain`.

### Regras de arquitetura (ArchUnit)

Os combinados acima são verificados a cada build pelo módulo `arquitetura`, que depende de todos os outros e analisa o conjunto (núcleo e canais) de uma vez, em `arquitetura/src/test/java/<pacote raiz>/arquitetura/`. Cada grupo é uma classe, e `ArquiteturaTest` só os agrega. O que o Maven já impede (núcleo usar canal, canal usar outro canal) também tem regra, para valer dentro de um mesmo módulo.

**Para usar em outro projeto, é só copiar e colar.** Copie a pasta `arquitetura` para `src/test/java/<pacote raiz do projeto>/arquitetura/` (num projeto multi-módulo, num módulo de testes que dependa de todos, como o `arquitetura` daqui) e deixe a IDE ajustar a linha `package` ao colar. Nenhum arquivo tem o nome do projeto:
- `Raiz` descobre o pacote raiz pelo lugar da pasta (`<raiz>.arquitetura`) e ancora todos os padrões nele. `"query"` casa só com `<raiz>.query`, nunca com pacotes de bibliotecas como `org.hibernate.query`.
- Regras de pastas que o projeto ainda não tem (sem canal, sem gateway, sem `command.gateway`) passam vazias até a primeira classe aparecer.
- `GER-001` nunca passa vazia: se a pasta foi colada no pacote errado e nenhuma classe foi encontrada, o build falha em vez de deixar tudo passar.

Pré-requisitos no projeto de destino: as dependências `archunit-junit5` e `junit-jupiter` (escopo de teste) e o código organizado em `command`, `query`, `shared` e `canal` debaixo do pacote raiz.

Cada regra tem **um motivo só** e um **ID estável** (`DOM-002`). Quando uma falha, o resumo do Maven mostra a regra, o porquê, como corrigir e onde. Quem monta esse texto é `FormatoDaFalha`, ligado pelo `Raiz` através da propriedade `failureDisplayFormat` do ArchUnit, então ele também vem junto ao copiar a pasta:

```
[ERROR] Failures:
[ERROR]   ArquiteturaTest.RegrasDominio > dom002Persistencia VIOLAÇÃO DE ARQUITETURA (1 ocorrência)
  Regra:   DOM-002: DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA
  Por quê: o domínio não sabe que existe banco.
  Corrija: declare uma interface de repositório no domínio; a implementação, a entidade JPA e o Mapper ficam em command.persistence e shared.persistence
  Onde:
    - Method <...command.domain.Cota.paraEntidade()> has return type <...shared.persistence.CotaEntity> in (Cota.java:0)
```

Se os acentos saírem como `?`, o terminal ou o agente de CI não está em UTF-8 (use `LANG=C.UTF-8`).

Convenção de cada regra: `.as("ABC-001: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: como")`, com o campo começando pelo ID (`dom002Persistencia`). O prefixo é o do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM). Um ID nunca é reaproveitado: se uma regra sair, o número dela fica vago. Vagos hoje, desde a separação em módulos: CMD-004, QRY-003, CAN-002, CAN-003, CAN-004 e CAN-006 (cobertos por CAN-001, CAN-011 e GER-003). `CatalogoDeRegrasTest` garante o formato, IDs únicos e um prefixo por grupo.

| ID | Grupo | Regra | Por quê e como corrigir |
|---|---|---|---|
| `GER-001` | `RegrasGerais` | TODO CÓDIGO FICA EM COMMAND, QUERY, SHARED OU CANAL | o núcleo tem command (escrita), query (leitura) e shared; cada canal fica em canal.&lt;canal&gt;. **Corrija:** mova a classe para o lado certo; se nenhuma classe foi analisada, a pasta arquitetura está no pacote errado (deve ser &lt;raiz&gt;.arquitetura) |
| `GER-002` | `RegrasGerais` | SEM CICLOS ENTRE PACOTES | ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra. **Corrija:** inverta a dependência com uma interface ou mova a classe compartilhada |
| `GER-003` | `RegrasGerais` | NÚCLEO NÃO PODE DEPENDER DE CANAL | o mesmo núcleo atende todos os canais; formatação, token e contrato são do canal. **Corrija:** devolva dados crus do use case e trate no módulo do canal; regra de negócio que muda por canal vai para o domínio (CanalDeAtendimento) |
| `DOM-001` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK | o domínio é Java puro, testável sem subir o Quarkus. **Corrija:** tire @Inject/@ApplicationScoped/@Transactional do domínio; quem injeta e abre transação é o use case (command.application) |
| `DOM-002` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA | o domínio não sabe que existe banco. **Corrija:** declare uma interface de repositório no domínio; a implementação, a entidade JPA e o Mapper ficam em command.persistence e shared.persistence |
| `DOM-003` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE APPLICATION (SERVICE LAYER) | o use case usa o domínio, nunca o contrário. **Corrija:** passe o dado como parâmetro do método do domínio ou mova a lógica para o use case |
| `DOM-004` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE GATEWAY EXTERNO | só use cases chamam sistemas externos. **Corrija:** o use case chama o Gateway e passa o resultado para o domínio; se a regra do domínio precisa do sistema, declare a interface no domínio e implemente em command.gateway |
| `DOM-005` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE HTTP | HTTP é detalhe de entrada. **Corrija:** lance uma DominioException; o canal traduz para o status HTTP (canal.comum.erro.ExceptionMappers) |
| `DOM-006` | `RegrasDominio` | DOMÍNIO NÃO PODE USAR BEAN VALIDATION | o domínio protege as próprias invariantes. **Corrija:** valide formato (@NotBlank, @Size) no Request do entrypoint e regra de negócio no construtor/método do domínio lançando DominioException |
| `DOM-007` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE CANAL | formatação, token e contrato mudam por canal; o domínio não. **Corrija:** devolva o valor cru e trate no módulo do canal; regra que muda por canal fica em CanalDeAtendimento, no próprio domínio |
| `DOM-008` | `RegrasDominio` | DOMÍNIO AGRUPADO POR TIPO (ENTITY, REPOSITORY, EXCEPTION, ENUMS) | achar uma classe do domínio não pode depender de abrir o código. **Corrija:** agregado e estados em entity (agregado grande ganha subpacote, ex.: entity.pagamento), interface de repositório em repository, exceção em exception, enum em enums |
| `CMD-001` | `RegrasCommand` | COMMAND NÃO PODE DEPENDER DE QUERY | a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica. **Corrija:** busque pelo repositório do domínio (command.domain) e, se faltar método, crie-o lá |
| `CMD-002` | `RegrasCommand` | USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO | o use case vê o banco só pela interface do domínio (Separated Interface). **Corrija:** injete a interface de command.domain (ex.: CotaRepository), não o *RepositoryAdapter |
| `CMD-003` | `RegrasCommand` | USE CASE NÃO PODE USAR ENTIDADE JPA NEM JAKARTA DATA | o use case trabalha com objetos do domínio. **Corrija:** use o objeto do domínio; a conversão entidade &lt;-&gt; domínio é do Mapper em command.persistence |
| `CMD-005` | `RegrasCommand` | COMMAND.PERSISTENCE SÓ É USADO PELA INTERFACE DO DOMÍNIO | a implementação dos repositórios é encontrada via CDI pela interface do domínio. **Corrija:** dependa da interface em command.domain, nunca da classe de command.persistence |
| `CMD-006` | `RegrasCommand` | IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO FICA EM COMMAND.GATEWAY | cada pasta tem um papel só. **Corrija:** mova a classe para command.gateway |
| `CMD-007` | `RegrasCommand` | COMMAND.GATEWAY SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO | ele não orquestra nem acessa banco. **Corrija:** deixe só a tradução domínio &lt;-&gt; Gateway; orquestração vai para o use case |
| `CMD-008` | `RegrasCommand` | COMMAND.GATEWAY SÓ É USADO PELA INTERFACE DO DOMÍNIO | quem precisa usa a interface, nunca a implementação. **Corrija:** injete a interface declarada em command.domain |
| `CMD-009` | `RegrasCommand` | COMMAND.APPLICATION AGRUPADO POR TIPO (USECASE, EXCEPTION, DTO) | cada tipo de classe tem lugar fixo. **Corrija:** *UseCase em usecase, exceção em exception, dados de entrada do use case em dto |
| `CMD-010` | `RegrasCommand` | COMMAND.PERSISTENCE AGRUPADO POR TIPO (REPOSITORY, MAPPER, ADAPTER) | cada tipo de classe tem lugar fixo. **Corrija:** *DataRepository em repository, *Mapper em mapper, *RepositoryAdapter em adapter |
| `CMD-011` | `RegrasCommand` | MAPPER SÓ É USADO PELO ADAPTER | o mapper é detalhe de como o adapter grava; ele só é público por estar em outro pacote. **Corrija:** chame a interface de repositório do domínio; a conversão fica no *RepositoryAdapter |
| `CMD-012` | `RegrasCommand` | REPOSITÓRIO JAKARTA DATA DA ESCRITA SÓ É USADO PELO ADAPTER | o *DataRepository trabalha com *Entity; o resto da escrita trabalha com o domínio. **Corrija:** chame a interface de repositório do domínio; o *RepositoryAdapter usa o *DataRepository |
| `QRY-001` | `RegrasQuery` | QUERY NÃO PODE DEPENDER DE COMMAND | leitura e escrita evoluem separadas (CQRS). **Corrija:** crie um record de projeção e a consulta em query.persistence; não reuse o domínio |
| `QRY-002` | `RegrasQuery` | QUERY.PERSISTENCE NÃO PODE DEPENDER DE QUERY.APPLICATION | a direção é canal -&gt; application -&gt; persistence. **Corrija:** o repositório devolve records próprios; quem adapta é o use case ou o canal |
| `QRY-004` | `RegrasQuery` | QUERY.APPLICATION AGRUPADO POR TIPO (USECASE, DTO) | cada tipo de classe tem lugar fixo. **Corrija:** *UseCase em usecase, tipos de saída comuns (ex.: Pagina) em dto |
| `QRY-005` | `RegrasQuery` | QUERY.PERSISTENCE AGRUPADO POR TIPO (REPOSITORY, PROJECTION) | cada tipo de classe tem lugar fixo. **Corrija:** *QueryRepository em repository, record projetado em projection |
| `SHR-001` | `RegrasShared` | SHARED NÃO PODE DEPENDER DE COMMAND NEM QUERY | shared é usado pelos dois lados, nunca o contrário; por isso um gateway nunca importa o domínio. **Corrija:** use tipos próprios do shared (ex.: DTO do gateway) e converta no use case |
| `SHR-002` | `RegrasShared` | ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO | entidade é só o formato da tabela. **Corrija:** mova o método para o objeto do domínio em command.domain; na entidade, só get/set/is |
| `SHR-003` | `RegrasShared` | CLIENT E DTO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY | se o sistema externo mudar o contrato, só o gateway muda. **Corrija:** fora de shared.gateway use só a classe *Gateway e as *Exception dele |
| `SHR-004` | `RegrasShared` | SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO | chamada externa é orquestração, papel do Service Layer; o canal só traduz a exceção para HTTP. **Corrija:** injete o *Gateway no *UseCase e passe o resultado adiante |
| `SHR-005` | `RegrasShared` | UM GATEWAY NÃO PODE DEPENDER DE OUTRO | cada sistema externo tem o próprio gateway. **Corrija:** quem combina dois sistemas é o use case |
| `CAN-001` | `RegrasCanais` | UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL | cada canal tem o próprio contrato, padrão e dependências corporativas, e sobe sozinho. **Corrija:** copie o que precisa para o seu canal; o que é comum a todos vai para canal.comum |
| `CAN-005` | `RegrasCanais` | CANAL.COMUM NÃO PODE DEPENDER DE UM CANAL | canal.comum é a base que todos os canais usam. **Corrija:** mova o que é específico para o módulo do canal |
| `CAN-007` | `RegrasCanais` | ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE | mudar uma projeção não pode mudar o JSON do front. **Corrija:** devolva o *Response do canal, montado pelo Assembler |
| `CAN-008` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK | são funções puras, testáveis sem Quarkus. **Corrija:** tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA |
| `CAN-009` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO | montar a resposta não busca dados. **Corrija:** o use case busca tudo que a tela precisa; o Assembler só transforma |
| `CAN-010` | `RegrasCanais` | CANAL @SOMENTELEITURA NÃO PODE USAR O LADO DE ESCRITA | a escrita está bloqueada neste canal (package-info do canal). **Corrija:** use só query.application; se o canal passou a escrever, isso é decisão de negócio: tire o @SomenteLeitura no PR e libere o tipo em CanalDeAtendimento |
| `CAN-011` | `RegrasCanais` | CANAL NÃO PODE ACESSAR O BANCO | o canal só traduz HTTP e chama use case; dos repositórios ele só vê os records projetados. **Corrija:** crie ou use um *UseCase em command.application ou query.application |
| `TEC-001` | `RegrasTecnologia` | @ENTITY SÓ EM SHARED.PERSISTENCE | as entidades são o modelo do nosso banco, compartilhado por escrita e leitura. **Corrija:** mova a classe para shared.persistence |
| `TEC-002` | `RegrasTecnologia` | PANACHE NÃO É USADO | o padrão do projeto é Jakarta Data, com HQL validado na compilação. **Corrija:** crie uma interface @Repository com @Find/@Insert/@Update/@Query |
| `TEC-003` | `RegrasTecnologia` | @REPOSITORY (JAKARTA DATA) SÓ EM COMMAND.PERSISTENCE OU QUERY.PERSISTENCE | repositório Jakarta Data é detalhe de persistência de cada lado. **Corrija:** mova a interface para o persistence do lado que a usa |
| `TEC-004` | `RegrasTecnologia` | REST CLIENT SÓ EM SHARED.GATEWAY | chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway. **Corrija:** mova o client para shared.gateway.&lt;sistema&gt; e exponha um *Gateway |
| `TEC-005` | `RegrasTecnologia` | TIMEOUT, RETRY E CIRCUIT BREAKER SÓ EM SHARED.GATEWAY | resiliência protege chamadas a sistemas externos. **Corrija:** coloque @Timeout/@Retry/@CircuitBreaker no client do gateway |
| `TEC-006` | `RegrasTecnologia` | @PATH SÓ NOS CANAIS | só o módulo de um canal expõe HTTP; o núcleo é biblioteca. **Corrija:** mova o *Resource para canal.&lt;canal&gt;.entrypoint, no módulo do canal |
| `TEC-007` | `RegrasTecnologia` | BEAN VALIDATION SÓ NOS CANAIS | Bean Validation é o porteiro do formato da entrada de cada canal; regra de negócio fica no domínio. **Corrija:** anote o *Request do canal; no domínio, lance DominioException |
| `TEC-008` | `RegrasTecnologia` | @SERVEREXCEPTIONMAPPER SÓ NOS CANAIS | traduzir exceção para HTTP é papel do canal, igual para todos. **Corrija:** adicione o mapeamento em canal.comum.erro.ExceptionMappers |
| `TEC-009` | `RegrasTecnologia` | @TRANSACTIONAL EM MÉTODO SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação daqui e coloque no método do *UseCase que chama este código |
| `TEC-010` | `RegrasTecnologia` | @TRANSACTIONAL EM CLASSE SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação da classe e coloque no *UseCase |
| `NOM-001` | `RegrasNomes` | *USECASE FICA EM APPLICATION.USECASE | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-002` | `RegrasNomes` | *RESOURCE FICA EM ENTRYPOINT | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-003` | `RegrasNomes` | *ASSEMBLER FICA EM ENTRYPOINT | montar a resposta do canal é papel do entrypoint. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-004` | `RegrasNomes` | *GATEWAY FICA EM SHARED.GATEWAY OU COMMAND.GATEWAY | Gateway é o acesso a sistema externo. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-005` | `RegrasNomes` | *ENTITY FICA EM SHARED.PERSISTENCE | Entity é o formato da tabela. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-006` | `RegrasNomes` | *MAPPER FICA EM COMMAND.PERSISTENCE.MAPPER | o mapper converte entre domínio e banco do lado de escrita. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-007` | `RegrasNomes` | PADRAO* FICA EM CANAL.&lt;CANAL&gt;.APRESENTACAO | o padrão de apresentação é de um canal (a interface comum fica em canal.comum.apresentacao). **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-008` | `RegrasNomes` | MAPEADORDETOKEN* FICA EM CANAL.&lt;CANAL&gt;.SEGURANCA | o token é de um canal, com a biblioteca corporativa dele. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-009` | `RegrasNomes` | *REPOSITORYADAPTER FICA EM COMMAND.PERSISTENCE.ADAPTER | o adapter implementa a interface de repositório do domínio. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-010` | `RegrasNomes` | *DATAREPOSITORY FICA EM COMMAND.PERSISTENCE.REPOSITORY | é o repositório Jakarta Data da escrita. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-011` | `RegrasNomes` | *EXCEPTION DO NÚCLEO FICA NUM PACOTE EXCEPTION | exceção é agrupada por tipo (command.domain.exception, command.application.exception). A de sistema externo fica na raiz de shared.gateway. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-012` | `RegrasNomes` | ENUM DO DOMÍNIO FICA EM COMMAND.DOMAIN.ENUMS | enum é agrupado por tipo. **Corrija:** mova o enum para command.domain.enums |

O HQL dos repositórios Jakarta Data também é validado na compilação, pelo Hibernate Processor.

### Canais e apresentação

Os fronts são gerados por low-code e não formatam nada, então o backend entrega tudo pronto para exibir, e cada canal (web, mobile, TAA) segue um padrão próprio da empresa. Os padrões deste exemplo são fictícios.

- **O use case não formata nem decide por canal.** Ele devolve o read model em termos de negócio; regra que muda por canal é do domínio (veja acima).
- **Cada canal é um BFF na própria aplicação:** `canal.<canal>.entrypoint` tem o Resource, o Response (o contrato do front) e o Assembler (DTO Assembler do PoEAA), uma função pura que decide **o que** o canal recebe.
- **O padrão do canal decide _como_ exibir:** `PadraoWeb`, `PadraoMobile` e `PadraoTaa` (em `canal.<canal>.apresentacao`) implementam `PadraoApresentacao` (`canal-comum`). Um formato novo na interface só compila quando todos os canais decidirem como exibi-lo. Todos usam a base comum `Formatador` (locale explícito, sem o espaço U+00A0 do `NumberFormat`) e `Rotulos` (catálogo por extenso que cada canal usa, abrevia ou transforma).
- **Somar ou formatar o que já veio da consulta** é papel do Assembler (ex.: `totalCotas` no mobile). Um número com **significado de negócio** (total pago, saldo devedor) nasce no lado de consulta e serve todos os canais, senão os canais divergem.
- **O response traz o valor bruto e o formatado** (`valorCredito` e `valorCreditoFormatado`, `estado` e `situacao`): o low-code usa o bruto para ordenar, filtrar e escolher cor, e o formatado para exibir.
- **O contrato é congelado:** só mudanças aditivas; uma quebra vira `/v2`. Cada canal tem o `Contrato<Canal>Test` (nomes exatos dos campos, textos, token) e o `Padrao<Canal>Test` (tabela viva do padrão). `RotulosTest` falha se um estado novo do domínio não tiver rótulo.
- **Quando houver vários serviços,** `canal-comum` é o primeiro candidato a virar uma biblioteca interna versionada, para que todos sigam o mesmo padrão.

| Entrada | Web | Mobile | TAA |
|---|---|---|---|
| `250000.00` | `R$ 250.000,00` | `R$ 250 mil` | `R$250.000,00` |
| `12345678909` | `123.456.789-09` | `***.456.789-**` | `***.***.789-09` |
| `Júlia Araújo Lima` | `Júlia Araújo Lima` | `Júlia Lima` | `JULIA ARAUJO LIMA` |
| `AGUARDANDO_VISTORIA` | `Aguardando vistoria` | `Aguard. vistoria` | `AGUARDANDO VISTORIA` |

### Processo de pagamento

`ProcessoPagamento` é abstrata, com `ProcessoImobiliario` e `ProcessoServico`. Os estados seguem o padrão State (`command.domain.entity.pagamento.state`):

```
Imobiliário: ABERTO → EM_ANALISE → AGUARDANDO_VISTORIA → APROVADO → PAGO
Serviço:     ABERTO → EM_ANALISE → APROVADO → PAGO
Qualquer estado não final → CANCELADO
```

Transições inválidas lançam `TransicaoInvalidaException` (HTTP 409). Cada cota tem no máximo um processo em andamento. No banco existe uma única tabela, `processo_pagamento`, com `tipo` e `estado` em texto. O mapper reconstrói a subclasse e o estado, sem herança JPA.

## Guardrails para IA e revisão

O ArchUnit detecta o erro no build. Estas peças evitam que ele aconteça e impedem que ele entre:

| Camada | Peça | Onde |
|---|---|---|
| Orientar | Instruções gerais do Copilot | `.github/copilot-instructions.md` |
| Orientar | Instruções por pasta (carregadas só quando o Copilot mexe ali) | `.github/instructions/*.instructions.md` |
| Orientar | Receitas prontas: `/novo-caso-de-uso-escrita`, `/nova-consulta`, `/nova-integracao-externa`, `/novo-endpoint-de-canal`, `/novo-canal`, `/separar-entidade-jpa` (legado: entidade JPA que também é domínio) | `.github/prompts/*.prompt.md` |
| Orientar | O papel e as proibições de cada pacote, lidos pela IA ao abrir a pasta | `package-info.java` |
| Bloquear | Dependências entre módulos: núcleo sem canal, canal sem outro canal (nem compila) | `pom.xml` de cada módulo |
| Detectar | Regras ArchUnit, testes de contrato por canal, HQL validado na compilação | `arquitetura/`, `*/src/test/...` |
| Bloquear | CI (`./mvnw verify`), donos das partes sensíveis e checklist de PR | `.github/workflows/ci.yml`, `.github/CODEOWNERS`, `.github/pull_request_template.md` |

**Neste repositório, a pasta `.github` é um modelo:** o GitHub só lê `.github` na raiz do repositório. Ao criar um projeto a partir deste exemplo, ela vai para a raiz.

O CODEOWNERS sozinho só **pede** revisão. Para que ele e o CI **bloqueiem** o merge, configure no GitHub, em *Settings → Rules → Rulesets* (ou *Branches → Branch protection*), para a `main`:
1. *Require a pull request before merging*, com *Require review from Code Owners*.
2. *Require status checks to pass*, marcando o job `build` do CI.
3. *Block force pushes* e, se quiser, *Require linear history*.

## API

Todo caminho de canal exige o token daquele canal (`Authorization: Bearer <token>`); sem ele, 401.

| Canal (aplicação) | Método e caminho | Descrição |
|---|---|---|
| web | `GET /web/v1/clientes` · `POST /web/v1/clientes` · `PUT/DELETE /web/v1/clientes/{id}` | Clientes com a quantidade de cotas; CRUD (CPF único) |
| web | `GET /web/v1/cotas/{id}` · `POST /web/v1/cotas` · `PUT/DELETE /web/v1/cotas/{id}` | Cota no padrão web, com o processo atual; CRUD |
| web | `GET /web/v1/processos?tipo=&estado=&pagina=1&tamanho=20` | Processos filtrados e paginados, com o protocolo de pagamento |
| web | `POST /web/v1/processos` `{cotaId, tipo}` | Cria processo (`IMOBILIARIO` ou `SERVICO`) |
| web | `POST /web/v1/processos/{id}/iniciar-analise` · `concluir-analise` · `concluir-vistoria` · `pagar` · `cancelar` | Transições (`pagar` registra a ordem na Tesouraria) |
| mobile | `GET /mobile/v1/cotas/{id}` · `GET /mobile/v1/clientes` | Card da cota; clientes com o total de cotas somado |
| mobile | `POST /mobile/v1/processos` `{cotaId, tipo}` | Abre processo; o app só abre `SERVICO` (regra do domínio) |
| TAA | `GET /taa/v1/cotas/{id}` | Cota no padrão do terminal; sem escrita |

Validação em duas camadas: o **formato** da entrada (obrigatório, tamanho, faixa) é checado com Bean Validation nos requests de cada canal (`ClienteRequest`, `CotaRequest`, `NovoProcessoMobileRequest`), antes do use case, e as restrições aparecem no OpenAPI; a **regra de negócio** continua no domínio, que vale para qualquer chamador. Todo erro sai no mesmo formato, e `campos` só vem preenchido em erro de formato:

```json
{ "mensagem": "Dados inválidos", "campos": [ { "campo": "cpf", "erro": "não deve estar em branco" } ] }
```

Erros: 400 para entrada fora do formato ou violação de regra de domínio (inclusive a regra do canal), 401 sem o token do canal, 404 para recurso inexistente, 409 para conflito ou transição inválida, 503 para sistema externo indisponível. Excluir cliente com cotas, ou cota com processos, retorna 409.

## Como rodar

Requisitos: JDK 25 (`sdk env install` com SDKMAN) e Docker. O Dev Services sobe o PostgreSQL em dev e nos testes.

```bash
./mvnw verify                          # todos os módulos: compila, testa (domínio, canais, ArchUnit) e empacota + *IT
./mvnw install -DskipTests             # uma vez, para o quarkus:dev de um canal achar o núcleo
./mvnw -pl canal-web quarkus:dev       # web em http://localhost:8081 (mobile 8082, taa 8083), Swagger em /q/swagger-ui
./mvnw -pl canal-mobile -am verify     # só o mobile e o que ele usa
docker compose up --build              # PostgreSQL + uma aplicação por canal (rode ./mvnw package antes)
```

```bash
curl -H 'Authorization: Bearer web:M123' localhost:8081/web/v1/clientes
curl -H 'Authorization: Bearer mob.12345678909' localhost:8082/mobile/v1/clientes
curl -H 'Authorization: Bearer taa-T0042' localhost:8083/taa/v1/cotas/1
```

Em produção, informe o banco com `DB_URL`, `DB_USER` e `DB_PASSWORD`, e a tesouraria com `TESOURARIA_URL`. Em `quarkus:dev`, sem uma tesouraria em `http://localhost:8089`, o `pagar` retorna 503.

Cópia do exemplo mantido em um repositório privado (branch `exemplo-cqrs`). Rode os comandos acima de dentro desta pasta.
