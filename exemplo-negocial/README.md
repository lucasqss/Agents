# Consórcio: exemplo de referência

Projeto didático para validar uma arquitetura em camadas com domínio puro em Quarkus, aplicada a processos de pagamento de consórcio. Ele não implementa o negócio completo.

**Stack:** Java 25, Quarkus 3.33, Hibernate ORM com Jakarta Data (Hibernate Data Repositories) na escrita e na leitura, PostgreSQL (Dev Services), Flyway, JUnit 5, RestAssured e ArchUnit.

## Arquitetura

Hexagonal/clean **pragmática**, com os padrões do *Patterns of Enterprise Application Architecture* (Fowler). Só existe uma pasta quando o problema pede, e só há interface (porta) onde ela protege alguma coisa.

Pacote raiz `br.com.claudio.consorcio`:

```
entrypoint/
  rest/<agregado>/            API neutra, sem formatação: Resource, QueryResource, Request
  web/ mobile/ taa/           um BFF por canal: Padrao<Canal> na raiz e, em <agregado>/, Resource + Response (contrato) + Assembler
  apresentacao/               Formatador, Rotulos e PadraoApresentacao (base comum dos canais)
  ExceptionMappers, ErroResponse, IdCriado
application/
  <agregado>/                 um *UseCase por operação (Service Layer): transação e orquestração
  NaoEncontradoException, ConflitoException, Pagina
domain/
  <agregado>/                 Domain Model puro: Cliente, Cota, ProcessoPagamento (+ estados) e interfaces de repositório
  DominioException
persistence/
  <agregado>/                 *Entity, *DataRepository, *Mapper, *RepositoryAdapter, *QueryRepository e records projetados
gateway/
  <sistema>/                  Gateway de cada sistema externo + client REST, DTOs externos e resiliência
  SistemaExternoIndisponivelException
```

Os agregados são `cliente`, `cota` e `processo`. Nos entrypoints, primeiro vem a via e depois o agregado: `entrypoint/mobile/cota/CotaMobileResource.java`.

- **Escrita:** `entrypoint → application → domain ← persistence`. A persistência implementa as interfaces que o domínio declara (Separated Interface), então o use case não vê JPA. Quando a escrita precisa consultar algo (existe? está ativo?), o método entra nessa interface do domínio; nunca num `*QueryRepository`.
- **Leitura:** `entrypoint → application → persistence`. Não há domínio para proteger, então também não há porta. O repositório Jakarta Data já é uma interface, com a implementação gerada, e projeta direto no record.

### Persistência com Jakarta Data nos dois lados

Escrita e leitura usam repositórios Jakarta Data: toda consulta é um `@Query` em HQL, **validado na compilação** pelo Hibernate Processor, inclusive as consultas de existência da escrita (`existeNumero`, `existeAtivoPorCota`).

O Jakarta Data do Hibernate usa **stateless session**: não há *dirty checking*. Alterar um objeto carregado não grava nada; gravar é sempre explícito. Por isso o `XRepositoryAdapter` monta a entidade a partir do domínio (`XMapper.paraEntidade`) e chama `inserir` (id nulo) ou `atualizar`. No nosso desenho isso já era assim, porque o domínio nunca toca na entidade JPA.

### Dois tipos de infraestrutura

| | Nosso banco | Sistemas externos (corporativos) |
|---|---|---|
| Modelo | nosso, versionado por migrations do Flyway | deles; muda sem aviso |
| Onde fica | `persistence.<agregado>` | `gateway.<sistema>`, atrás de um **Gateway** |
| Acoplamento | a escrita mapeia para o domínio; a leitura projeta direto do banco | o Gateway traduz; client e DTOs externos não saem do pacote |
| Transação | a mesma do use case | fora do nosso controle (veja o trade-off abaixo) |
| Falha | rara, vira 500 | esperada: timeout, retry e circuit breaker no client; vira `SistemaExternoIndisponivelException` (503) |
| Teste | PostgreSQL real (Dev Services) | **Service Stub** (`TesourariaFake`; WireMock na aceitação), nunca o sistema real |
| Config | datasource (`DB_*`); permite réplica de leitura no futuro | `quarkus.rest-client.<sistema>.*` (`TESOURARIA_URL`) |

Só os use cases chamam sistemas externos, por isso eles usam o Gateway direto, sem porta. Se um dia uma regra do **domínio** precisar de um sistema externo, aí sim o domínio declara uma interface e um `*GatewayAdapter` em `gateway/<sistema>/` delega ao Gateway.

**Exemplo: a Tesouraria.** Pagar é uma saga orquestrada ([ADR 002](docs/adr/ADR_002_pagamento_como_saga.md)). `SolicitarPagamentoProcessoUseCase` só grava `PAGAMENTO_SOLICITADO`, com a identificação da solicitação; nada vai à tesouraria. Uma rotina agendada (`entrypoint/batch/processo/RotinaDePagamentoJob` → `ExecutarRotinaDePagamentoUseCase`) chama `TesourariaGateway.solicitarPagamento` **fora de transação**, com a identificação como chave de idempotência, e `AplicarResultadoPagamentoUseCase` grava o resultado em transação curta, com o processo bloqueado. O gateway traduz tudo (inclusive 5xx, timeout de 2 s e circuito aberto) para `RespostaDaTesouraria`, sem lançar exceção; quem repete é a rotina. Consistência eventual: ver o ADR para o que acontece em cada falha.

### Padrões (PoEAA)

Service Layer (use cases), Domain Model (`domain`), Repository + Data Mapper (`persistence`), Separated Interface (repositórios do domínio), Gateway e Service Stub (integrações). Num módulo que só orquestra, sem regra de negócio de verdade, um **Transaction Script** no use case basta, sem pasta `domain`.

### Regras de arquitetura (ArchUnit)

Os combinados acima são verificados a cada build, em `src/test/java/<pacote raiz>/arquitetura/`. Cada grupo é uma classe, e `ArquiteturaTest` só os agrega.

**Tipo Negocial** porque o componente tem as regras e é dono dos dados de cliente, cota e processo de pagamento (veja o ADR 001 na skill `arquiteto-padrao`). O tipo está declarado em `Tipo.java`, que define as pastas permitidas.

**Para usar em outro projeto, é só copiar e colar.** A origem são as pastas `comum/` e a do tipo (`negocial/`, `acl/` ou `bff/`) em `.github/skills/arquiteto-padrao/assets/arquitetura/`. Copie os arquivos das duas para `src/test/java/<pacote raiz do projeto>/arquitetura/` e troque `PACOTE_RAIZ` pelo pacote raiz. Fora o `Tipo` e o `ClassesExternas`, nenhum arquivo muda de projeto para projeto:
- `Raiz` descobre o pacote raiz pelo lugar da pasta (`<raiz>.arquitetura`) e ancora todos os padrões nele. `"persistence"` casa só com `<raiz>.persistence`, nunca com pacotes de bibliotecas como `jakarta.persistence`.
- Regras de pastas que o projeto ainda não tem (sem canal, sem gateway, sem `*GatewayAdapter`) passam vazias até a primeira classe aparecer.
- `ClassesExternas` é o único arquivo que se edita por projeto: a lista de `CONFIGURACAO` tem um `groupId` Maven por sistema externo (o exemplo é `org.acme.sistema`), além do `groupId` do próprio componente, lido do `pom.xml`. Classe que vem de jar desses `groupId`s só pode aparecer em `gateway` e `entrypoint`.
- `GER-001` nunca passa vazia: se a pasta foi colada no pacote errado e nenhuma classe foi encontrada, o build falha em vez de deixar tudo passar.

Pré-requisitos no projeto de destino: a dependência `archunit-junit5` (escopo de teste) e o código organizado nas pastas do tipo do componente debaixo do pacote raiz (no Negocial: `entrypoint`, `application`, `domain`, `persistence` e `gateway`).

Cada regra tem **um motivo só** e um **ID estável** (`DOM-002`). Quando uma falha, o resumo do Maven mostra a regra, o porquê, como corrigir e onde. Quem monta esse texto é `FormatoDaFalha`, ligado pelo `Raiz` através da propriedade `failureDisplayFormat` do ArchUnit, então ele também vem junto ao copiar a pasta:

```
[ERROR] Failures:
[ERROR]   ArquiteturaTest.RegrasDominio > dom002Persistencia VIOLAÇÃO DE ARQUITETURA (1 ocorrência)
  Regra:   DOM-002: DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA
  Por quê: o domínio não sabe que existe banco.
  Corrija: declare uma interface de repositório no domínio; a implementação, a entidade JPA e o Mapper ficam em persistence/<agregado>
  Onde:
    - Method <...domain.cota.Cota.paraEntidade()> has return type <...persistence.cota.CotaEntity> in (Cota.java:0)
```

Se os acentos saírem como `?`, o terminal ou o agente de CI não está em UTF-8 (use `LANG=C.UTF-8`).

Convenção de cada regra: `.as("ABC-001: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: como")`, com o campo começando pelo ID (`dom002Persistencia`). O prefixo é o do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM; os tipos ACL e BFF têm também os grupos ACL e BFF, que não se aplicam a este componente). Um ID nunca é reaproveitado: se uma regra sair, o número dela fica vago (como `CAN-002` e `CAN-003`), e o prefixo `SHR` ficou por histórico. `CatalogoDeRegrasTest` garante o formato, IDs únicos e um prefixo por grupo.

| ID | Grupo | Regra | Por quê e como corrigir |
|---|---|---|---|
| `GER-001` | `RegrasGerais` | TODO CÓDIGO FICA EM UMA DAS CAMADAS DO TIPO DO COMPONENTE | um componente Negocial só tem entrypoint, application, domain, persistence, gateway. **Corrija:** mova a classe para a camada certa; se a camada não existe neste tipo, o código pertence a outro componente; se nenhuma classe foi analisada, a pasta arquitetura está no pacote errado (deve ser <raiz>.arquitetura) |
| `GER-002` | `RegrasGerais` | SEM CICLOS ENTRE PACOTES | ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra. **Corrija:** inverta a dependência com uma interface ou mova a classe compartilhada |
| `GER-003` | `RegrasGerais` | RAIZ DA CAMADA SÓ ACEITA OS TIPOS LISTADOS | sem limite, a raiz da camada vira um novo shared. **Corrija:** mova a classe para <camada>/<agregado>; a raiz só aceita exceções comuns, ExceptionMappers, ErroResponse, IdCriado, Pagina, Dinheiro e package-info |
| `GER-004` | `RegrasGerais` | NÃO EXISTEM PASTAS SHARED, COMMON, UTIL, COMMAND NEM QUERY | essas pastas viram depósito ou separam por técnica o que é do mesmo agregado. **Corrija:** mova a classe para <camada>/<agregado> |
| `GER-005` | `RegrasGerais` | ENDPOINT FICA EM ENTRYPOINT.<VIA>.<AGREGADO> | primeiro a via (rest, web, mobile, taa), depois o agregado, sempre nessa ordem. **Corrija:** mova o *Resource; se ele atende vários agregados, faça um Resource por agregado |
| `DOM-001` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK | o domínio é Java puro, testável sem subir o Quarkus. **Corrija:** tire @Inject/@ApplicationScoped/@Transactional do domínio; quem injeta e abre transação é o use case (application/<agregado>) |
| `DOM-002` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA | o domínio não sabe que existe banco. **Corrija:** declare uma interface de repositório no domínio; a implementação, a entidade JPA e o Mapper ficam em persistence/<agregado> |
| `DOM-003` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE APPLICATION (SERVICE LAYER) | o use case usa o domínio, nunca o contrário. **Corrija:** passe o dado como parâmetro do método do domínio ou mova a lógica para o use case |
| `DOM-004` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE GATEWAY EXTERNO | só use cases chamam sistemas externos. **Corrija:** o use case chama o Gateway e passa o resultado para o domínio; se a regra do domínio precisa do sistema, declare a interface no domínio e implemente em um *GatewayAdapter em gateway/<sistema> |
| `DOM-005` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE HTTP NEM DO ENTRYPOINT | HTTP é detalhe de entrada. **Corrija:** lance uma DominioException; o entrypoint traduz para o status HTTP no ExceptionMappers |
| `DOM-006` | `RegrasDominio` | DOMÍNIO NÃO PODE USAR BEAN VALIDATION | o domínio protege as próprias invariantes. **Corrija:** valide formato (@NotBlank, @Size) no Request do entrypoint e regra de negócio no construtor/método do domínio lançando DominioException |
| `DOM-007` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE APRESENTAÇÃO DE CANAL | formatação muda por canal; regra de negócio não. **Corrija:** devolva o valor cru e formate no entrypoint do canal com o Padrao do canal |
| `DOM-008` | `RegrasDominio` | UM AGREGADO NÃO PODE DEPENDER DE OUTRO NO DOMÍNIO | o agregado guarda só o id do outro; assim cada um muda sem arrastar o resto. **Corrija:** troque a referência pelo id e coordene os dois agregados no use case |
| `CMD-001` | `RegrasEscrita` | CASO DE USO QUE USA O DOMÍNIO NÃO PODE USAR *QUERYREPOSITORY | a escrita decide com o domínio; consultas de tela mudam conforme as telas. **Corrija:** busque pelo repositório em domain/<agregado> e, se faltar método, crie-o lá; se é uma leitura, tire o domínio do caso de uso |
| `CMD-002` | `RegrasEscrita` | USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO | o use case vê o banco só pela interface do domínio (Separated Interface). **Corrija:** injete a interface de domain/<agregado> (ex.: CotaRepository), não o *RepositoryAdapter |
| `CMD-003` | `RegrasEscrita` | USE CASE NÃO PODE USAR ENTIDADE JPA | o use case trabalha com objetos do domínio ou records de consulta. **Corrija:** use o objeto do domínio; a conversão entidade <-> domínio é do Mapper em persistence/<agregado> |
| `CMD-004` | `RegrasEscrita` | ENTRYPOINT NÃO PODE ACESSAR O BANCO | o entrypoint só traduz HTTP e chama use case. **Corrija:** crie ou use um use case em application/<agregado> |
| `CMD-005` | `RegrasEscrita` | A IMPLEMENTAÇÃO DA ESCRITA SÓ É USADA PELA INTERFACE DO DOMÍNIO | a implementação dos repositórios é encontrada via CDI pela interface do domínio. **Corrija:** dependa da interface em domain/<agregado>, nunca da classe de persistence/<agregado> |
| `CMD-006` | `RegrasEscrita` | IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO É UM *GATEWAYADAPTER EM GATEWAY | cada pasta tem um papel só. **Corrija:** mova a classe para gateway/<sistema> com o sufixo GatewayAdapter |
| `CMD-007` | `RegrasEscrita` | *GATEWAYADAPTER SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO | ele não orquestra nem acessa banco. **Corrija:** deixe só a tradução domínio <-> Gateway; orquestração vai para o use case |
| `CMD-008` | `RegrasEscrita` | *GATEWAYADAPTER SÓ É USADO PELA INTERFACE DO DOMÍNIO | quem precisa usa a interface, nunca a implementação. **Corrija:** injete a interface declarada em domain/<agregado> |
| `CMD-009` | `RegrasEscrita` | *USECASE TEM UM ÚNICO MÉTODO PÚBLICO | uma classe, uma operação: é o limite que impede classes gigantes. **Corrija:** separe cada operação em seu próprio *UseCase em application/<agregado> |
| `CMD-010` | `RegrasEscrita` | CASO DE USO QUE USA O DOMÍNIO NÃO PODE USAR JAKARTA DATA | na escrita o banco fica atrás da interface do domínio. **Corrija:** declare o método na interface de domain/<agregado> e implemente em persistence/<agregado> |
| `QRY-001` | `RegrasLeitura` | *QUERYREPOSITORY NÃO PODE DEPENDER DO DOMÍNIO | consulta de tela não passa pelo domínio. **Corrija:** crie um record de projeção e a consulta em persistence/<agregado>; não reuse o domínio |
| `QRY-002` | `RegrasLeitura` | PERSISTENCE NÃO PODE DEPENDER DE APPLICATION NEM ENTRYPOINT | a direção é entrypoint -> application -> persistence. **Corrija:** o repositório devolve records próprios; quem adapta é o use case ou o entrypoint |
| `QRY-003` | `RegrasLeitura` | ENTRYPOINT NÃO PODE CHAMAR REPOSITÓRIO | o entrypoint chama use cases; dos repositórios ele só vê os records projetados. **Corrija:** crie ou use um *UseCase em application/<agregado> |
| `SHR-001` | `RegrasPersistenciaEGateway` | GATEWAY NÃO PODE DEPENDER DAS OUTRAS CAMADAS | o gateway só conhece o sistema externo; a exceção é o *GatewayAdapter, que implementa interface do domínio. **Corrija:** use tipos próprios do gateway e converta no use case |
| `SHR-002` | `RegrasPersistenciaEGateway` | ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO | entidade é só o formato da tabela. **Corrija:** mova o método para o objeto do domínio em domain/<agregado>; na entidade, só get/set/is |
| `SHR-003` | `RegrasPersistenciaEGateway` | CLIENT E MODELO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY | se o sistema externo mudar o contrato, só o gateway muda. **Corrija:** fora de gateway/<sistema> use o *Gateway, as *Exception e os records próprios do gateway; record próprio não aparece no *Client nem usa classe de dependência de outro sistema. Em ACL, o use case leva o modelo externo do *Gateway ao *Translator e mais ninguém o vê |
| `SHR-004` | `RegrasPersistenciaEGateway` | SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO | chamada externa é orquestração, papel do Service Layer; o entrypoint só traduz a exceção para HTTP. **Corrija:** injete o *Gateway no *UseCase e passe o resultado adiante |
| `SHR-005` | `RegrasPersistenciaEGateway` | UM GATEWAY NÃO PODE DEPENDER DE OUTRO | cada sistema externo tem o próprio gateway. **Corrija:** quem combina dois sistemas é o use case |
| `SHR-006` | `RegrasPersistenciaEGateway` | ENTRYPOINT NÃO PODE CHAMAR GATEWAY | o entrypoint só traduz HTTP e, do gateway, só conhece a exceção. **Corrija:** crie ou use um *UseCase que chama o *Gateway |
| `CAN-001` | `RegrasCanais` | UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL | cada canal tem o próprio contrato e evolui sozinho. **Corrija:** copie o Response/Assembler para o seu canal; o que for comum vai para o use case ou para entrypoint/apresentacao |
| `CAN-004` | `RegrasCanais` | FORMATAÇÃO SÓ NO ENTRYPOINT | use case e consulta não formatam nada; o mesmo dado atende todos os canais. **Corrija:** devolva o valor cru e formate no Assembler/Resource do canal |
| `CAN-005` | `RegrasCanais` | BASE COMUM DE APRESENTAÇÃO NÃO PODE DEPENDER DE UM CANAL | Formatador e Rotulos são a base que todos os canais usam. **Corrija:** mova o que é específico para entrypoint/<canal> |
| `CAN-006` | `RegrasCanais` | USE CASE NÃO PODE DEPENDER DE ENTRYPOINT | o mesmo use case atende todos os canais. **Corrija:** receba e devolva tipos do application; o entrypoint converte Request/Response |
| `CAN-007` | `RegrasCanais` | ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE | mudar uma projeção não pode mudar o JSON do front. **Corrija:** devolva o *Response do canal, montado pelo Assembler |
| `CAN-008` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK | são funções puras, testáveis sem Quarkus. **Corrija:** tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA |
| `CAN-009` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO | montar a resposta não busca dados. **Corrija:** o use case busca tudo que a tela precisa; o Assembler só transforma |
| `TEC-001` | `RegrasTecnologia` | @ENTITY SÓ EM PERSISTENCE | as entidades são o modelo do nosso banco, compartilhado por escrita e leitura. **Corrija:** mova a classe para persistence/<agregado> |
| `TEC-002` | `RegrasTecnologia` | PANACHE NÃO É USADO | o padrão do projeto é Jakarta Data, com HQL validado na compilação. **Corrija:** crie uma interface @Repository com @Find/@Insert/@Update/@Query |
| `TEC-003` | `RegrasTecnologia` | @REPOSITORY (JAKARTA DATA) SÓ EM PERSISTENCE | repositório Jakarta Data é detalhe de persistência. **Corrija:** mova a interface para persistence/<agregado> |
| `TEC-004` | `RegrasTecnologia` | REST CLIENT SÓ EM GATEWAY | chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway. **Corrija:** mova o client para gateway/<sistema> e exponha um *Gateway |
| `TEC-005` | `RegrasTecnologia` | TIMEOUT, RETRY E CIRCUIT BREAKER SÓ EM GATEWAY | resiliência protege chamadas a sistemas externos. **Corrija:** coloque @Timeout/@Retry/@CircuitBreaker no client do gateway |
| `TEC-006` | `RegrasTecnologia` | @PATH SÓ NO ENTRYPOINT | só o entrypoint expõe HTTP. **Corrija:** mova o *Resource para entrypoint/<via>/<agregado> |
| `TEC-007` | `RegrasTecnologia` | BEAN VALIDATION SÓ NO ENTRYPOINT | Bean Validation é o porteiro do formato da entrada; regra de negócio fica no domínio. **Corrija:** anote o *Request do entrypoint; no domínio, lance DominioException |
| `TEC-008` | `RegrasTecnologia` | @SERVEREXCEPTIONMAPPER SÓ NO ENTRYPOINT | traduzir exceção para HTTP é papel do entrypoint. **Corrija:** adicione o mapeamento em ExceptionMappers do entrypoint |
| `TEC-009` | `RegrasTecnologia` | @TRANSACTIONAL EM MÉTODO SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação daqui e coloque no método do *UseCase que chama este código |
| `TEC-010` | `RegrasTecnologia` | @TRANSACTIONAL EM CLASSE SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação da classe e coloque no *UseCase |
| `TEC-011` | `RegrasTecnologia` | CLASSE DE DEPENDÊNCIA DE OUTRO SISTEMA SÓ EM GATEWAY E ENTRYPOINT | o contrato de outro sistema muda sem nos avisar; application e domain usam só tipos nossos. **Corrija:** ao consumir, o *Gateway devolve tipo simples ou record próprio de gateway/<sistema>; ao prover, o entrypoint converte a classe externa no DadosX do use case. Em ACL, o use case leva a classe externa até o *Translator, que a converte |
| `TEC-012` | `RegrasTecnologia` | TIPOS E ANOTAÇÕES DE SEGURANÇA SÓ NO ENTRYPOINT | quem é a pessoa e que papel ela tem vêm do token, e isso é detalhe de entrada; o domínio e o use case recebem um dado puro (Autor) e decidem com ele. **Corrija:** use @RolesAllowed/@Authenticated e JsonWebToken só no entrypoint, monte o Autor ali e entregue-o ao use case; a permissão por situação vai na tabela do fluxo, no domínio |
| `NOM-001` | `RegrasNomes` | *USECASE FICA EM APPLICATION | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-002` | `RegrasNomes` | *RESOURCE FICA EM ENTRYPOINT | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-003` | `RegrasNomes` | *ASSEMBLER FICA EM ENTRYPOINT | montar a resposta do canal é papel do entrypoint. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-004` | `RegrasNomes` | *GATEWAY FICA EM GATEWAY | Gateway é o acesso a sistema externo. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-005` | `RegrasNomes` | *ENTITY FICA EM PERSISTENCE | Entity é o formato da tabela. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-006` | `RegrasNomes` | *MAPPER E *REPOSITORYADAPTER FICAM EM PERSISTENCE | eles convertem entre domínio e banco. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-007` | `RegrasNomes` | PADRAO* FICA EM ENTRYPOINT | padrão de apresentação é por canal. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |

O HQL dos repositórios Jakarta Data também é validado na compilação, pelo Hibernate Processor.

### Canais e apresentação

Os fronts são gerados por low-code e não formatam nada, então o backend entrega tudo pronto para exibir, e cada canal (web, mobile, TAA) segue um padrão próprio da empresa. Os padrões deste exemplo são fictícios.

- **O use case não sabe qual canal o chamou.** Ele devolve o read model em termos de negócio.
- **Cada canal é um BFF dentro do serviço:** `entrypoint.<canal>.<agregado>` tem o Resource, o Response (o contrato do front) e o Assembler (DTO Assembler do PoEAA), uma função pura que decide **o que** o canal recebe.
- **O padrão do canal decide _como_ exibir:** `PadraoWeb`, `PadraoMobile` e `PadraoTaa` implementam `PadraoApresentacao`. Um formato novo na interface só compila quando todos os canais decidirem como exibi-lo. Todos usam a base comum `Formatador` (locale explícito, sem o espaço U+00A0 do `NumberFormat`) e `Rotulos` (catálogo por extenso que cada canal usa, abrevia ou transforma).
- **Somar ou formatar o que já veio da consulta** é papel do Assembler (ex.: `totalCotas` no mobile). Um número com **significado de negócio** (total pago, saldo devedor) nasce na consulta e serve todos os canais, senão os canais divergem.
- **O response traz o valor bruto e o formatado** (`valorCredito` e `valorCreditoFormatado`, `estado` e `situacao`): o low-code usa o bruto para ordenar, filtrar e escolher cor, e o formatado para exibir.
- **O contrato é congelado:** só mudanças aditivas; uma quebra vira `/v2`. `CanaisContratoTest` confere os nomes exatos dos campos e os textos de cada canal, e `PadroesDeCanalTest` é a tabela viva dos padrões (mesma entrada, uma saída por canal). Ele também falha se um estado novo do domínio não tiver rótulo.
- **Quando houver vários serviços,** `entrypoint.apresentacao` é o primeiro candidato a virar uma biblioteca interna versionada, para que todos sigam o mesmo padrão.

| Entrada | Web | Mobile | TAA |
|---|---|---|---|
| `250000.00` | `R$ 250.000,00` | `R$ 250 mil` | `R$250.000,00` |
| `12345678909` | `123.456.789-09` | `***.456.789-**` | `***.***.789-09` |
| `Júlia Araújo Lima` | `Júlia Araújo Lima` | `Júlia Lima` | `JULIA ARAUJO LIMA` |
| `AGUARDANDO_VISTORIA` | `Aguardando vistoria` | `Aguard. vistoria` | `AGUARDANDO VISTORIA` |

### Processo de pagamento

`ProcessoPagamento` é o motor único de todos os tipos de pagamento. Ele não conhece nenhum tipo em particular: pergunta ao fluxo do tipo se a ação vale no estado atual e para onde ela leva.

- **State dirigido por tabela** (`domain.processo.fluxo`): `EstadoProcesso` e `AcaoProcesso` são enums, e `FluxoProcesso` é a tabela imutável `(estado, ação) → próximo estado`. As ações disponíveis em cada estado saem da mesma tabela.
- **Um fluxo por tipo** (`domain.processo.tipo`): `TipoProcesso` guarda a tabela do tipo. Toda tabela parte de `FluxoProcesso.comum()` e troca só as linhas em que o tipo difere (`FluxoImobiliario`, `FluxoServico`).

```
Imobiliário: ABERTO → EM_ANALISE → AGUARDANDO_VISTORIA → APROVADO → PAGAMENTO_SOLICITADO → PAGO
Serviço:     ABERTO → EM_ANALISE → APROVADO → PAGAMENTO_SOLICITADO → PAGO
Alçada (R16): concluir a análise (serviço) ou a vistoria (imobiliário) leva a APROVADO se o valor vigente é até a alçada, inclusive, e a AGUARDANDO_ALCADA se passa dela
Aprovar na alçada: AGUARDANDO_ALCADA → APROVADO; em AGUARDANDO_ALCADA valem também reprovar (→ REPROVADO), devolver para análise (→ EM_ANALISE) e cancelar
Solicitar pagamento: APROVADO → PAGAMENTO_SOLICITADO (só grava; pessoa)
Rotina (nunca pessoa): PAGAMENTO_SOLICITADO → PAGO (confirmar, com protocolo) | → APROVADO (recusar, com motivo) | → PAGAMENTO_SOLICITADO (tentar de novo)
Solicitar documento: EM_ANALISE → PENDENTE_DOCUMENTO; receber documento: PENDENTE_DOCUMENTO → EM_ANALISE
Reprovar: EM_ANALISE ou AGUARDANDO_ALCADA → REPROVADO (final)
Devolver para análise: APROVADO ou AGUARDANDO_ALCADA → EM_ANALISE (e AGUARDANDO_VISTORIA → EM_ANALISE no imobiliário)
Alterar: ABERTO → ABERTO (o cliente troca valor e favorecidos na hora)
Propor alteração: EM_ANALISE → AGUARDANDO_CONCORDANCIA (o backoffice propõe, com motivo)
Concordar: AGUARDANDO_CONCORDANCIA → EM_ANALISE (a proposta passa a valer)
Discordar: AGUARDANDO_CONCORDANCIA → EM_ANALISE (a proposta é descartada; motivo obrigatório)
Qualquer estado não final → CANCELADO, exceto PAGAMENTO_SOLICITADO (exceção declarada na tabela: só a rotina tira o processo dali) (em AGUARDANDO_CONCORDANCIA só valem concordar, discordar e cancelar)
```

**Alçada e transição condicional na tabela.** A linha que leva a APROVADO ao concluir a análise (serviço) e a vistoria (imobiliário) tem dois destinos, escolhidos por uma condição declarada na própria tabela: `.transicaoCondicional(EM_ANALISE, CONCLUIR_ANALISE, CondicaoDaTransicao.VALOR_PASSA_DA_ALCADA, AGUARDANDO_ALCADA, APROVADO)`. A condição recebe um `ContextoDaTransicao(valor, alcada)`; a raiz só entrega o valor vigente e a alçada, e não há `if (valor > alcada)` na entidade, no caso de uso nem no entrypoint. `acoesDisponiveis` continua saindo da tabela. O imobiliário troca só as linhas em que difere (concluir análise → AGUARDANDO_VISTORIA, sem condição; concluir vistoria, condicional). A alçada é a configuração `processo.alcada` (padrão `50000.00`, no `application.properties`): `ExecutarAcaoProcessoUseCase` a lê por `@ConfigProperty` e a passa ao domínio como `Dinheiro`. Valor igual à alçada vai a APROVADO; a decisão usa o valor vigente de cada conclusão. Quem aprova na alçada não pode ser quem concluiu a análise (R17): ver "Autoria e papéis".

**Autoria e papéis** (ADR [003](docs/adr/ADR_003_identidade_por_jwt.md)). Todo pedido, menos `/q/*`, leva `Authorization: Bearer <token JWT>`, verificado pela aplicação (`quarkus-smallrye-jwt`): `sub` é o usuário, `groups` os papéis (`cliente`, `backoffice`, `aprovador`, `tesouraria`) e `clienteId` o cadastro do cliente. A segurança só existe no `entrypoint` (`TEC-012`): `@RolesAllowed` em cada endpoint é a primeira barreira, e `AutorDoToken` monta o `Autor` (usuário, `Papel`s, `clienteId`), dado puro que os casos de uso entregam ao domínio.

- **Papel por linha da tabela.** `.transicao(de, ação, para, Papel...)` declara quem pode pedir cada linha; reprovar e devolver para análise mudam de papel conforme a situação, e a raiz confere o `Autor` contra a linha (`PermissaoNegadaException`, 403). Ações da rotina não têm papel e gravam o autor `"sistema"`.
- **Cliente da cota (R18).** Abrir, alterar, concordar e discordar: o `clienteId` do autor precisa ser o da cota (`Cota.exigirDoCliente`, 403).
- **R17.** A raiz guarda o usuário da última conclusão de análise (`autor_ultima_conclusao_analise`); a linha `AGUARDANDO_ALCADA` + `APROVAR_NA_ALCADA` é marcada `exigindoOutraPessoaQueConcluiuAAnalise`.
- **R19.** `Transicao.autor` e `GET /processos/{id}/historico` com `autor` (migration `V9`; linhas antigas, `desconhecido`).
- **Ordem das recusas:** 401, 403 de papel (anotação), 400 de formato, 404, 403 de dono, de R17 e de papel por situação, 409.

Pago, reprovado e cancelado são finais: não aceitam nenhuma ação e liberam a cota para um novo processo.

**Motivo obrigatório.** Solicitar documento, reprovar, devolver para análise, cancelar, propor alteração e discordar só valem com o motivo informado. Quem sabe disso é o domínio: `AcaoProcesso.exigeMotivo()` marca essas ações e `ProcessoPagamento.executar` lança `DominioException` quando o motivo falta. O entrypoint confere o formato (`ProcessoResource.Motivo`: obrigatório, não vazio, até 500 caracteres) e responde 400 com o campo `motivo`, antes de o processo ser buscado. O motivo vai ao histórico, no campo `observacao`.

**Valor e crédito disponível.** Todo processo tem `valor` (`Dinheiro`, maior que zero, até duas casas), protegido em `ProcessoPagamento`; o entrypoint confere o formato (`@NotNull @Positive @Digits`) e responde 400 com o campo `valor`. O crédito disponível da cota é o crédito menos o valor dos processos **pagos** (`Cota.creditoDisponivel`); reprovado e cancelado não consomem crédito. `Cota.exigirCreditoPara` recusa o valor acima do disponível com `CreditoInsuficienteException` (HTTP 409, a mensagem diz quanto há disponível). A abertura lê a soma dos pagos dentro do mesmo bloqueio da cota que garante um só processo em andamento, então pedidos simultâneos não estouram o crédito. `GET /cotas/{id}` devolve o `creditoDisponivel`. A migration `V5` cria a coluna `valor` e, na implantação, dá aos processos existentes o crédito da cota.

**Favorecidos.** O valor do processo é dividido entre favorecidos (`Favorecido`, Value Object do agregado `ProcessoPagamento`: nome, `Documento`, papel e parte em `Dinheiro`). A raiz protege as invariantes em `ProcessoPagamento.novo(...)`, com `DominioException` (400): ao menos um favorecido, cada parte maior que zero, partes somando exatamente o valor (a mensagem diz quanto falta ou passa) e o mesmo documento não se repete. `Documento` é CPF (11) ou CNPJ (14), só dígitos; o dígito verificador não é conferido nesta entrega. O entrypoint confere só o formato (`@NotEmpty`, tamanhos, `@Pattern`, `@Digits`) e responde 400 com `campos` (`favorecidos[1].documento`) antes de qualquer busca; as regras do domínio rodam ao montar o agregado, antes do 404 e do 409. Os favorecidos não têm repositório próprio: o adapter do processo os grava ao inserir o processo (tabela `processo_favorecido`, com a coluna `ordem`) e os carrega ao reconstituí-lo. A migration `V6` dá a cada processo existente um favorecido `titular`, com o nome e o CPF (só dígitos) do cliente da cota e o valor inteiro do processo. A alteração de valor e favorecidos está na seção seguinte.

**Alteração com concordância do cliente.** Em ABERTO o cliente altera valor e favorecidos livremente (`ProcessoPagamento.alterar`, com as mesmas invariantes da abertura: o conjunto é conferido por um único método, `exigirDivisaoValida`). Depois que a análise começa, só o backoffice propõe uma alteração (`proporAlteracao`, em EM_ANALISE, com motivo) e o processo passa a AGUARDANDO_CONCORDANCIA.

- **Mudança pendente.** A proposta é um Value Object, `AlteracaoProposta` (valor, favorecidos e motivo, com as mesmas invariantes do conjunto), guardado à parte: o valor e os favorecidos **vigentes** (`GET /processos`, `/favorecidos`, o crédito de `GET /cotas/{id}`) continuam os anteriores até a resposta. A raiz garante que há proposta pendente se, e somente se, o estado é AGUARDANDO_CONCORDANCIA. Não há prazo: a cota segue indisponível e o cancelamento é a saída.
- **Desfazer.** Concordar torna a proposta vigente e ela some; discordar (com motivo) e cancelar a descartam, e os vigentes ficam como estavam. O backoffice pode propor de novo, sem limite. Concordar e discordar passam pela tabela do fluxo e pelo `ExecutarAcaoProcessoUseCase`; alterar e propor têm método próprio na raiz (`AcaoProcesso.comMetodoProprio`), e o `executar` genérico as recusa.
- **Crédito.** `AlterarProcessoUseCase` e `ProporAlteracaoProcessoUseCase` conferem o valor contra o crédito como a abertura (R13): bloqueiam a cota, depois o processo (sempre nessa ordem, para não haver impasse), leem a soma dos pagos na mesma transação e chamam `cota.exigirCreditoPara`. O valor proposto é conferido ao propor; reconferir ao concordar é dúvida aberta (D21). Concordar e discordar simultâneos: a `versao` deixa um passar (200) e recusa o outro (409).
- **Histórico.** `Transicao` ganha `valorAnterior` e `valorNovo` (só em ALTERAR e PROPOR_ALTERACAO; nulos nas demais). O motivo vai em `observacao`.
- **Banco.** A migration `V7` cria `processo_alteracao_proposta` (no máximo uma por processo) e `processo_proposta_favorecido` (com os mesmos `unique` de `processo_favorecido`, dentro do conjunto proposto) e as colunas `valor_anterior` e `valor_novo` do histórico. Ao salvar, o adapter compara o que está gravado com o agregado: se os favorecidos vigentes mudaram, apaga e regrava; se a proposta mudou, grava ou apaga.

| Endpoint | Corpo | Resultado |
|---|---|---|
| `POST /processos/{id}/alterar` | `{"valor", "favorecidos": [...]}` | só em ABERTO; `200 {"estado": "ABERTO"}` |
| `POST /processos/{id}/propor-alteracao` | `{"valor", "favorecidos": [...], "motivo"}` | só em EM_ANALISE; `200 {"estado": "AGUARDANDO_CONCORDANCIA"}` |
| `POST /processos/{id}/concordar` | opcional `{"observacao"}` | só em AGUARDANDO_CONCORDANCIA; `200 {"estado": "EM_ANALISE"}` |
| `POST /processos/{id}/discordar` | obrigatório `{"motivo"}` | só em AGUARDANDO_CONCORDANCIA; `200 {"estado": "EM_ANALISE"}` |
| `GET /processos/{id}/alteracao-proposta` | | `200 {"valor", "favorecidos", "motivo"}` enquanto aguarda; `404` sem proposta |

400 de formato (antes de 404 e 409), 400 de regra (soma, documento repetido), 404 e 409 (situação, crédito). Quem pode chamar cada endpoint está em "Autoria e papéis" e na tabela de endpoints.

Transições inválidas lançam `TransicaoInvalidaException` (HTTP 409). Cada cota tem no máximo um processo em andamento. No banco, `processo_pagamento` guarda `tipo` e `estado` em texto, e `processo_transicao` guarda o histórico: uma linha por mudança de estado, com a ação, a data e a observação. O mapper reconstrói o tipo e o estado pelo código gravado (`TipoProcesso.valueOf`, `EstadoProcesso.valueOf`), sem herança JPA. A coluna `versao` impede que duas operações simultâneas gravem por cima uma da outra.

Um único caso de uso, `ExecutarAcaoProcessoUseCase`, atende as ações que só mudam o estado. `SolicitarPagamentoProcessoUseCase` é separado porque a solicitação traz a identificação; a rotina tem os seus próprios casos de uso. As ações `CONFIRMAR_PAGAMENTO`, `RECUSAR_PAGAMENTO` e `TENTAR_PAGAMENTO` são da rotina (`AcaoProcesso.daRotina()`): não aparecem em `/acoes` nem passam pelo executar genérico.

**Como estender o motor**

| Quero... | Faça |
|---|---|
| Um tipo novo de pagamento | constante em `TipoProcesso` + um `Fluxo<Tipo>` partindo de `FluxoProcesso.comum()` + rótulo em `Rotulos` |
| Um estado novo | constante em `EstadoProcesso` + as linhas no fluxo que o usa + rótulo em `Rotulos` |
| Uma ação nova | constante em `AcaoProcesso` + as linhas nos fluxos que a aceitam + endpoint em `ProcessoResource` |
| Um desvio novo por tipo | uma linha `.transicao(...)` no `Fluxo<Tipo>` |

## Guardrails para IA e revisão

O ArchUnit detecta o erro no build. Estas peças evitam que ele aconteça e impedem que ele entre:

| Camada | Peça | Onde |
|---|---|---|
| Orientar | Regras sempre ativas para qualquer agente de IA | `AGENTS.md` |
| Impor | Hooks do agente (Copilot CLI): arquivo protegido não é alterado e o turno só termina com a verificação rápida passando | `.github/hooks/` |
| Orientar | Do pedido de negócio ao caso de uso confirmado, antes do código: `/especificar-caso-de-uso` | `.github/skills/especificar-caso-de-uso/` |
| Orientar | O padrão de arquitetura, o ADR e as receitas de código novo: `/arquiteto-padrao` | `.github/skills/arquiteto-padrao/` |
| Orientar | Como trazer um projeto legado para o padrão: `/adequar-ao-padrao` | `.github/skills/adequar-ao-padrao/` |
| Orientar | Testes de aceitação pelos endpoints: `/teste-aceitacao-modelos` a partir do requisito, `/teste-aceitacao-legado` a partir do código existente | `.github/skills/teste-aceitacao-modelos/`, `.github/skills/teste-aceitacao-legado/` |
| Orientar | O papel e as proibições de cada pacote, lidos pela IA ao abrir a pasta | `package-info.java` |
| Detectar | Regras ArchUnit, testes de contrato, HQL validado na compilação | `src/test/...` |
| Bloquear | Donos das partes sensíveis | `.github/CODEOWNERS` |
| Bloquear | `./mvnw verify` na esteira corporativa (a esteira de versão usa `./mvnw verify -Pversao`) | fora deste repositório |

**Neste repositório, a pasta `.github` é um modelo:** o GitHub só lê `.github` na raiz do repositório. Ao criar um projeto a partir deste exemplo, ela vai para a raiz, junto com o `AGENTS.md`.

O CODEOWNERS sozinho só **pede** revisão. Para que ele e a esteira **bloqueiem** o merge, configure no GitHub, em *Settings → Rules → Rulesets* (ou *Branches → Branch protection*), para a `main`:
1. *Require a pull request before merging*, com *Require review from Code Owners*.
2. *Require status checks to pass*, marcando o check da esteira que roda `./mvnw verify`. A proteção de branch continua nele; só a esteira de geração de versão usa `./mvnw verify -Pversao`, que acrescenta os `*IT`.
3. *Block force pushes* e, se quiser, *Require linear history*.

## API

| Tipo | Método e caminho | Descrição |
|---|---|---|
| Escrita (`backoffice`) | `POST /clientes`, `PUT/DELETE /clientes/{id}` | CRUD de cliente (CPF único) |
| Escrita (`backoffice`) | `POST /cotas`, `PUT/DELETE /cotas/{id}` | CRUD de cota |
| Escrita (`cliente`, dono da cota) | `POST /processos` `{cotaId, tipo, valor, favorecidos: [{nome, documento, papel, valor}]}` | Cria processo (`IMOBILIARIO` ou `SERVICO`) com o valor (número maior que zero, até 2 casas, até o crédito disponível da cota), dividido entre ao menos um favorecido (partes somando o valor) |
| Escrita (`backoffice`; `aprovar-na-alcada`: `aprovador`, outra pessoa que a da conclusão da análise) | `POST /processos/{id}/iniciar-analise` · `concluir-analise` · `concluir-vistoria` · `aprovar-na-alcada` · `receber-documento` | Transições (`aprovar-na-alcada` só em AGUARDANDO_ALCADA; concluir análise ou vistoria responde APROVADO ou AGUARDANDO_ALCADA conforme `processo.alcada`); corpo opcional `{"observacao": "..."}` (até 500 caracteres) |
| Escrita (`solicitar-documento`: `backoffice`; `reprovar` e `devolver-para-analise`: `backoffice` ou `aprovador`, conforme a situação; `cancelar`: `backoffice` ou `aprovador`) | `POST /processos/{id}/solicitar-documento` · `reprovar` · `devolver-para-analise` · `cancelar` | Transições; corpo obrigatório `{"motivo": "..."}` (não vazio, até 500 caracteres), gravado no histórico |
| Escrita (`tesouraria`) | `POST /processos/{id}/solicitar-pagamento` | Só leva APROVADO a PAGAMENTO_SOLICITADO (corpo opcional `{"observacao"}`); a rotina fala com a Tesouraria. Não existe endpoint para disparar a rotina nem para a Tesouraria avisar |
| Leitura (qualquer papel) | `GET /clientes` | Clientes com a quantidade de cotas |
| Leitura | `GET /cotas/{id}` | Cota com o nome do cliente, o processo atual e o `creditoDisponivel` |
| Canal web | `GET /web/v1/cotas/{id}` | Cota no padrão web, com o processo atual |
| Canal mobile | `GET /mobile/v1/cotas/{id}` · `GET /mobile/v1/clientes` | Card da cota; clientes com o total de cotas somado |
| Canal TAA | `GET /taa/v1/cotas/{id}` | Cota no padrão do terminal |
| Leitura | `GET /processos?tipo=&estado=&pagina=1&tamanho=20` | Processos filtrados (`estado` inclui `PENDENTE_DOCUMENTO`, `PAGAMENTO_SOLICITADO` e `REPROVADO`) e paginados, com o valor e o protocolo de pagamento |
| Leitura | `GET /processos/{id}/acoes` · `GET /processos/{id}/historico` | Ações que o processo aceita no estado atual; histórico das transições |
| Leitura | `GET /processos/{id}/favorecidos` | Favorecidos `[{nome, documento, papel, valor}]` na ordem informada; 404 se o processo não existe |

Papéis: `POST /processos/{id}/alterar`, `concordar` e `discordar` são do `cliente` (dono da cota); `propor-alteracao`, do `backoffice`. As consultas (`GET`) e os canais (`/web`, `/mobile`, `/taa`) aceitam qualquer papel autenticado; `/q/openapi` e o Swagger UI são abertos. Sem token, token inválido, expirado ou de outro emissor: 401; token sem o papel da ação, cliente que não é o dono da cota ou aprovação de quem concluiu a análise: 403. Ambos saem no formato de erro abaixo.

Validação em duas camadas: o **formato** da entrada (obrigatório, tamanho, faixa) é checado com Bean Validation nos requests do entrypoint (`ClienteRequest`, `CotaRequest`), antes do use case, e as restrições aparecem no OpenAPI; a **regra de negócio** continua no domínio, que vale para qualquer chamador. Todo erro sai no mesmo formato, e `campos` só vem preenchido em erro de formato:

```json
{ "mensagem": "Dados inválidos", "campos": [ { "campo": "cpf", "erro": "não deve estar em branco" } ] }
```

Erros: 401 e 403 de identidade (acima), 400 para entrada fora do formato ou violação de regra de domínio, 404 para recurso inexistente, 409 para conflito ou transição inválida, 503 para sistema externo indisponível. Excluir cliente com cotas, ou cota com processos, retorna 409.

## Como rodar

Requisitos: JDK 25 (`sdk env install` com SDKMAN) e Docker. O Dev Services sobe o PostgreSQL em dev e nos testes.

```bash
./mvnw quarkus:dev            # API em http://localhost:8080 e Swagger UI em /q/swagger-ui
./mvnw test                   # testes de domínio, ArchUnit e @QuarkusTest
./mvnw verify                 # domínio, ArchUnit e @QuarkusTest (inclusive aceitação) + empacotamento
./mvnw verify -Pversao        # o anterior + *IT contra o artefato empacotado (geração de versão fechada)
docker compose up --build     # app empacotada + PostgreSQL (rode ./mvnw package antes)
```

**Token de desenvolvimento.** Em `quarkus:dev` a aplicação confia na chave pública de `src/main/resources/jwt/publica-desenvolvimento.pem`, e `scripts/token-dev.sh` assina um token com o par privado (`docs/dev/privada-desenvolvimento.pem`, só para desenvolvimento):

```bash
TOKEN=$(scripts/token-dev.sh ana backoffice)                 # usuário e papéis (separados por vírgula)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/processos
scripts/token-dev.sh maria cliente 12                        # cliente: o terceiro argumento é o clienteId do cadastro
```

Em produção, o provedor de identidade emite os tokens e a aplicação recebe a chave pública e o emissor dele: `MP_JWT_VERIFY_PUBLICKEY_LOCATION` (obrigatória; arquivo ou URL) e `MP_JWT_VERIFY_ISSUER` (padrão `https://exemplo-negocial`). As chaves de `src/test/resources/aceitacao/chaves/` são de teste e não valem fora dos testes. Decisão e alternativas: [ADR 003](docs/adr/ADR_003_identidade_por_jwt.md).

A alçada de aprovação é `processo.alcada` (padrão `50000.00`); em produção pode ser trocada por `PROCESSO_ALCADA`. Em produção, informe o banco com `DB_URL`, `DB_USER` e `DB_PASSWORD`, e a tesouraria com `TESOURARIA_URL`. A rotina de pagamento roda a cada `pagamento.rotina.intervalo` (padrão `5m`; `off` desliga). Em `quarkus:dev`, sem uma tesouraria em `http://localhost:8089`, os processos ficam em PAGAMENTO_SOLICITADO e cada rodada registra uma tentativa no histórico. Nos testes de integração o agendamento está desligado (`%test.pagamento.rotina.intervalo=off`) e o caso de uso é chamado direto; nos de aceitação o intervalo é de 1 s.

Cópia do exemplo mantido em um repositório privado (branch `exemplo-negocial`). Rode os comandos acima de dentro desta pasta.
