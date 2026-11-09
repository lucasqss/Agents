# Consórcio CQRS: exemplo de referência

Projeto didático para validar uma arquitetura CQRS com domínio puro em Quarkus, aplicada a processos de pagamento de consórcio. Ele não implementa o negócio completo.

**Stack:** Java 25, Quarkus 3.33, Hibernate ORM com Jakarta Data (Hibernate Data Repositories) na escrita e na leitura, PostgreSQL (Dev Services), Flyway, JUnit 5, RestAssured e ArchUnit.

## Arquitetura

Hexagonal/clean **pragmática**, com os padrões do *Patterns of Enterprise Application Architecture* (Fowler). Só existe uma pasta quando o problema pede, e só há interface (porta) onde ela protege alguma coisa.

Pacote raiz `br.com.claudio.consorcio`:

```
shared/
  persistence/          entidades JPA: modelo do NOSSO banco, só mapeamento, getters e setters
  gateway/<sistema>/    Gateway de cada sistema externo + client REST, DTOs externos e resiliência
  apresentacao/         Formatador e Rotulos (base comum) + um padrão por canal: web/, mobile/, taa/
command/
  entrypoint/           REST de escrita + tradução de exceções para HTTP
  application/          use cases (Service Layer): transação e orquestração; chamam repositórios e gateways
  domain/               Domain Model puro: Cliente, Cota, ProcessoPagamento (+ estados) e interfaces de repositório
  persistence/          repositórios Jakarta Data + Data Mappers domínio <-> JPA escritos à mão
query/
  entrypoint/           REST de leitura neutro (sem formatação)
    web/ mobile/ taa/   um BFF por canal: Resource + Response (contrato) + Assembler
  application/          use cases de leitura (normalização de filtros, paginação)
  persistence/          repositórios Jakarta Data com @Query HQL + records projetados (read models)
```

- **Escrita:** `entrypoint → application → domain ← persistence`. A persistência implementa as interfaces que o domínio declara (Separated Interface), então o use case não vê JPA. Quando a escrita precisa consultar algo (existe? está ativo?), o método entra nessa interface do domínio; nunca num repositório do query.
- **Leitura:** `entrypoint → application → persistence`. Não há domínio para proteger, então também não há porta. O repositório Jakarta Data já é uma interface, com a implementação gerada, e projeta direto no record.

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

Service Layer (use cases), Domain Model (`command.domain`), Repository + Data Mapper (`command.persistence`), Separated Interface (repositórios do domínio), Gateway e Service Stub (integrações). Num módulo que só orquestra, sem regra de negócio de verdade, um **Transaction Script** no use case basta, sem pasta `domain`.

### Regras de arquitetura (ArchUnit)

Os combinados acima são verificados a cada build, em `src/test/java/<pacote raiz>/arquitetura/`. Cada grupo é uma classe, e `ArquiteturaTest` só os agrega.

**Para usar em outro projeto, é só copiar e colar.** Copie a pasta `arquitetura` para `src/test/java/<pacote raiz do projeto>/arquitetura/` e deixe a IDE ajustar a linha `package` ao colar. Nenhum arquivo tem o nome do projeto:
- `Raiz` descobre o pacote raiz pelo lugar da pasta (`<raiz>.arquitetura`) e ancora todos os padrões nele. `"query"` casa só com `<raiz>.query`, nunca com pacotes de bibliotecas como `org.hibernate.query`.
- Regras de pastas que o projeto ainda não tem (sem canal, sem gateway, sem `command.gateway`) passam vazias até a primeira classe aparecer.
- `GER-001` nunca passa vazia: se a pasta foi colada no pacote errado e nenhuma classe foi encontrada, o build falha em vez de deixar tudo passar.

Pré-requisitos no projeto de destino: a dependência `archunit-junit5` (escopo de teste) e o código organizado em `command`, `query` e `shared` debaixo do pacote raiz.

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

Convenção de cada regra: `.as("ABC-001: TÍTULO EM CAIXA ALTA").because("por quê. Corrija: como")`, com o campo começando pelo ID (`dom002Persistencia`). O prefixo é o do grupo (GER, DOM, CMD, QRY, SHR, CAN, TEC, NOM). Um ID nunca é reaproveitado: se uma regra sair, o número dela fica vago. `CatalogoDeRegrasTest` garante o formato, IDs únicos e um prefixo por grupo.

| ID | Grupo | Regra | Por quê e como corrigir |
|---|---|---|---|
| `GER-001` | `RegrasGerais` | TODO CÓDIGO FICA EM COMMAND, QUERY OU SHARED | a raiz do projeto só tem command (escrita), query (leitura) e shared. **Corrija:** mova a classe para o lado certo; se nenhuma classe foi analisada, a pasta arquitetura está no pacote errado (deve ser <raiz>.arquitetura) |
| `GER-002` | `RegrasGerais` | SEM CICLOS ENTRE PACOTES | ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra. **Corrija:** inverta a dependência com uma interface ou mova a classe compartilhada |
| `DOM-001` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK | o domínio é Java puro, testável sem subir o Quarkus. **Corrija:** tire @Inject/@ApplicationScoped/@Transactional do domínio; quem injeta e abre transação é o use case (command.application) |
| `DOM-002` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA | o domínio não sabe que existe banco. **Corrija:** declare uma interface de repositório no domínio; a implementação, a entidade JPA e o Mapper ficam em command.persistence e shared.persistence |
| `DOM-003` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE APPLICATION (SERVICE LAYER) | o use case usa o domínio, nunca o contrário. **Corrija:** passe o dado como parâmetro do método do domínio ou mova a lógica para o use case |
| `DOM-004` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE GATEWAY EXTERNO | só use cases chamam sistemas externos. **Corrija:** o use case chama o Gateway e passa o resultado para o domínio; se a regra do domínio precisa do sistema, declare a interface no domínio e implemente em command.gateway |
| `DOM-005` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE HTTP NEM DO ENTRYPOINT | HTTP é detalhe de entrada. **Corrija:** lance uma DominioException; o entrypoint traduz para o status HTTP no ExceptionMappers |
| `DOM-006` | `RegrasDominio` | DOMÍNIO NÃO PODE USAR BEAN VALIDATION | o domínio protege as próprias invariantes. **Corrija:** valide formato (@NotBlank, @Size) no Request do entrypoint e regra de negócio no construtor/método do domínio lançando DominioException |
| `DOM-007` | `RegrasDominio` | DOMÍNIO NÃO PODE DEPENDER DE APRESENTAÇÃO DE CANAL | formatação muda por canal; regra de negócio não. **Corrija:** devolva o valor cru e formate no entrypoint do canal com o Padrao do canal |
| `CMD-001` | `RegrasCommand` | COMMAND NÃO PODE DEPENDER DE QUERY | a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica. **Corrija:** busque pelo repositório do domínio (command.domain) e, se faltar método, crie-o lá |
| `CMD-002` | `RegrasCommand` | USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO | o use case vê o banco só pela interface do domínio (Separated Interface). **Corrija:** injete a interface de command.domain (ex.: CotaRepository), não o *RepositoryAdapter |
| `CMD-003` | `RegrasCommand` | USE CASE NÃO PODE USAR ENTIDADE JPA NEM JAKARTA DATA | o use case trabalha com objetos do domínio. **Corrija:** use o objeto do domínio; a conversão entidade <-> domínio é do Mapper em command.persistence |
| `CMD-004` | `RegrasCommand` | ENTRYPOINT DE ESCRITA NÃO PODE ACESSAR O BANCO | o entrypoint só traduz HTTP e chama use case. **Corrija:** crie ou use um use case em command.application |
| `CMD-005` | `RegrasCommand` | COMMAND.PERSISTENCE SÓ É USADO PELA INTERFACE DO DOMÍNIO | a implementação dos repositórios é encontrada via CDI pela interface do domínio. **Corrija:** dependa da interface em command.domain, nunca da classe de command.persistence |
| `CMD-006` | `RegrasCommand` | IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO FICA EM COMMAND.GATEWAY | cada pasta tem um papel só. **Corrija:** mova a classe para command.gateway |
| `CMD-007` | `RegrasCommand` | COMMAND.GATEWAY SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO | ele não orquestra nem acessa banco. **Corrija:** deixe só a tradução domínio <-> Gateway; orquestração vai para o use case |
| `CMD-008` | `RegrasCommand` | COMMAND.GATEWAY SÓ É USADO PELA INTERFACE DO DOMÍNIO | quem precisa usa a interface, nunca a implementação. **Corrija:** injete a interface declarada em command.domain |
| `QRY-001` | `RegrasQuery` | QUERY NÃO PODE DEPENDER DE COMMAND | leitura e escrita evoluem separadas (CQRS). **Corrija:** crie um record de projeção e a consulta em query.persistence; não reuse o domínio |
| `QRY-002` | `RegrasQuery` | QUERY.PERSISTENCE NÃO PODE DEPENDER DE APPLICATION NEM ENTRYPOINT | a direção é entrypoint -> application -> persistence. **Corrija:** o repositório devolve records próprios; quem adapta é o use case ou o entrypoint |
| `QRY-003` | `RegrasQuery` | ENTRYPOINT DE LEITURA NÃO PODE CHAMAR REPOSITÓRIO | o entrypoint chama use cases; dos repositórios ele só vê os records projetados. **Corrija:** crie ou use um *UseCase em query.application |
| `SHR-001` | `RegrasShared` | SHARED NÃO PODE DEPENDER DE COMMAND NEM QUERY | shared é usado pelos dois lados, nunca o contrário; por isso um gateway nunca importa o domínio. **Corrija:** use tipos próprios do shared (ex.: DTO do gateway) e converta no use case |
| `SHR-002` | `RegrasShared` | ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO | entidade é só o formato da tabela. **Corrija:** mova o método para o objeto do domínio em command.domain; na entidade, só get/set/is |
| `SHR-003` | `RegrasShared` | CLIENT E DTO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY | se o sistema externo mudar o contrato, só o gateway muda. **Corrija:** fora de shared.gateway use só a classe *Gateway e as *Exception dele |
| `SHR-004` | `RegrasShared` | SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO | chamada externa é orquestração, papel do Service Layer; o entrypoint só traduz a exceção para HTTP. **Corrija:** injete o *Gateway no *UseCase e passe o resultado adiante |
| `SHR-005` | `RegrasShared` | UM GATEWAY NÃO PODE DEPENDER DE OUTRO | cada sistema externo tem o próprio gateway. **Corrija:** quem combina dois sistemas é o use case |
| `CAN-001` | `RegrasCanais` | UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL | cada canal tem o próprio contrato e evolui sozinho. **Corrija:** copie o Response/Assembler para o seu canal; o que for comum vai para o use case |
| `CAN-002` | `RegrasCanais` | PADRÃO DE APRESENTAÇÃO DE UM CANAL NÃO PODE USAR O DE OUTRO | cada canal segue o padrão da própria plataforma. **Corrija:** o que é comum aos canais vai para a base shared.apresentacao (Formatador, Rotulos) |
| `CAN-003` | `RegrasCanais` | PADRÃO DO CANAL SÓ É USADO PELO ENTRYPOINT DO MESMO CANAL | o mobile não pode sair formatado no padrão web. **Corrija:** em entrypoint.<canal> use só shared.apresentacao.<canal> (ou a base comum) |
| `CAN-004` | `RegrasCanais` | FORMATAÇÃO SÓ NO ENTRYPOINT | use case e consulta não formatam nada; o mesmo dado atende todos os canais. **Corrija:** devolva o valor cru e formate no Assembler/Resource do canal |
| `CAN-005` | `RegrasCanais` | BASE COMUM DE APRESENTAÇÃO NÃO PODE DEPENDER DE UM CANAL | Formatador e Rotulos são a base que todos os canais usam. **Corrija:** mova o que é específico para shared.apresentacao.<canal> |
| `CAN-006` | `RegrasCanais` | USE CASE NÃO PODE DEPENDER DE ENTRYPOINT | o mesmo use case atende todos os canais. **Corrija:** receba e devolva tipos do application; o entrypoint converte Request/Response |
| `CAN-007` | `RegrasCanais` | ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE | mudar uma projeção não pode mudar o JSON do front. **Corrija:** devolva o *Response do canal, montado pelo Assembler |
| `CAN-008` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK | são funções puras, testáveis sem Quarkus. **Corrija:** tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA |
| `CAN-009` | `RegrasCanais` | ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO | montar a resposta não busca dados. **Corrija:** o use case busca tudo que a tela precisa; o Assembler só transforma |
| `TEC-001` | `RegrasTecnologia` | @ENTITY SÓ EM SHARED.PERSISTENCE | as entidades são o modelo do nosso banco, compartilhado por escrita e leitura. **Corrija:** mova a classe para shared.persistence |
| `TEC-002` | `RegrasTecnologia` | PANACHE NÃO É USADO | o padrão do projeto é Jakarta Data, com HQL validado na compilação. **Corrija:** crie uma interface @Repository com @Find/@Insert/@Update/@Query |
| `TEC-003` | `RegrasTecnologia` | @REPOSITORY (JAKARTA DATA) SÓ EM COMMAND.PERSISTENCE OU QUERY.PERSISTENCE | repositório Jakarta Data é detalhe de persistência de cada lado. **Corrija:** mova a interface para o persistence do lado que a usa |
| `TEC-004` | `RegrasTecnologia` | REST CLIENT SÓ EM SHARED.GATEWAY | chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway. **Corrija:** mova o client para shared.gateway.<sistema> e exponha um *Gateway |
| `TEC-005` | `RegrasTecnologia` | TIMEOUT, RETRY E CIRCUIT BREAKER SÓ EM SHARED.GATEWAY | resiliência protege chamadas a sistemas externos. **Corrija:** coloque @Timeout/@Retry/@CircuitBreaker no client do gateway |
| `TEC-006` | `RegrasTecnologia` | @PATH SÓ NO ENTRYPOINT | só o entrypoint expõe HTTP. **Corrija:** mova o *Resource para command.entrypoint ou query.entrypoint.<canal> |
| `TEC-007` | `RegrasTecnologia` | BEAN VALIDATION SÓ NO ENTRYPOINT | Bean Validation é o porteiro do formato da entrada; regra de negócio fica no domínio. **Corrija:** anote o *Request do entrypoint; no domínio, lance DominioException |
| `TEC-008` | `RegrasTecnologia` | @SERVEREXCEPTIONMAPPER SÓ NO ENTRYPOINT | traduzir exceção para HTTP é papel do entrypoint. **Corrija:** adicione o mapeamento em ExceptionMappers do entrypoint |
| `TEC-009` | `RegrasTecnologia` | @TRANSACTIONAL EM MÉTODO SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação daqui e coloque no método do *UseCase que chama este código |
| `TEC-010` | `RegrasTecnologia` | @TRANSACTIONAL EM CLASSE SÓ NO USE CASE | o use case (Service Layer) define a fronteira da transação. **Corrija:** tire a anotação da classe e coloque no *UseCase |
| `NOM-001` | `RegrasNomes` | *USECASE FICA EM APPLICATION | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-002` | `RegrasNomes` | *RESOURCE FICA EM ENTRYPOINT | o sufixo diz a camada. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-003` | `RegrasNomes` | *ASSEMBLER FICA EM ENTRYPOINT | montar a resposta do canal é papel do entrypoint. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-004` | `RegrasNomes` | *GATEWAY FICA EM SHARED.GATEWAY OU COMMAND.GATEWAY | Gateway é o acesso a sistema externo. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-005` | `RegrasNomes` | *ENTITY FICA EM SHARED.PERSISTENCE | Entity é o formato da tabela. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-006` | `RegrasNomes` | *MAPPER E *REPOSITORYADAPTER FICAM EM COMMAND.PERSISTENCE | eles convertem entre domínio e banco do lado de escrita. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |
| `NOM-007` | `RegrasNomes` | PADRAO* FICA EM SHARED.APRESENTACAO | padrão de apresentação é por canal e compartilhado. **Corrija:** mova a classe para a pasta certa ou troque o sufixo do nome |

O HQL dos repositórios Jakarta Data também é validado na compilação, pelo Hibernate Processor.

### Canais e apresentação

Os fronts são gerados por low-code e não formatam nada, então o backend entrega tudo pronto para exibir, e cada canal (web, mobile, TAA) segue um padrão próprio da empresa. Os padrões deste exemplo são fictícios.

- **O use case não sabe qual canal o chamou.** Ele devolve o read model em termos de negócio.
- **Cada canal é um BFF dentro do serviço:** `query.entrypoint.<canal>` tem o Resource, o Response (o contrato do front) e o Assembler (DTO Assembler do PoEAA), uma função pura que decide **o que** o canal recebe.
- **O padrão do canal decide _como_ exibir:** `PadraoWeb`, `PadraoMobile` e `PadraoTaa` implementam `PadraoApresentacao`. Um formato novo na interface só compila quando todos os canais decidirem como exibi-lo. Todos usam a base comum `Formatador` (locale explícito, sem o espaço U+00A0 do `NumberFormat`) e `Rotulos` (catálogo por extenso que cada canal usa, abrevia ou transforma).
- **Somar ou formatar o que já veio da consulta** é papel do Assembler (ex.: `totalCotas` no mobile). Um número com **significado de negócio** (total pago, saldo devedor) nasce no lado de consulta e serve todos os canais, senão os canais divergem.
- **O response traz o valor bruto e o formatado** (`valorCredito` e `valorCreditoFormatado`, `estado` e `situacao`): o low-code usa o bruto para ordenar, filtrar e escolher cor, e o formatado para exibir.
- **O contrato é congelado:** só mudanças aditivas; uma quebra vira `/v2`. `CanaisContratoTest` confere os nomes exatos dos campos e os textos de cada canal, e `PadroesDeCanalTest` é a tabela viva dos padrões (mesma entrada, uma saída por canal). Ele também falha se um estado novo do domínio não tiver rótulo.
- **Quando houver vários serviços,** `shared.apresentacao` é o primeiro candidato a virar uma biblioteca interna versionada, para que todos sigam o mesmo padrão.

| Entrada | Web | Mobile | TAA |
|---|---|---|---|
| `250000.00` | `R$ 250.000,00` | `R$ 250 mil` | `R$250.000,00` |
| `12345678909` | `123.456.789-09` | `***.456.789-**` | `***.***.789-09` |
| `Júlia Araújo Lima` | `Júlia Araújo Lima` | `Júlia Lima` | `JULIA ARAUJO LIMA` |
| `AGUARDANDO_VISTORIA` | `Aguardando vistoria` | `Aguard. vistoria` | `AGUARDANDO VISTORIA` |

### Processo de pagamento

`ProcessoPagamento` é o motor único de todos os tipos de pagamento. Dois padrões do GoF dividem o trabalho:

- **State** (`command.domain.estado`): o estado atual decide se a ação é válida e qual o próximo estado.
- **Strategy** (`FluxoProcesso`, escolhida por `TipoProcesso`): o tipo decide os desvios do fluxo. Hoje, só o que vem depois da análise.

```
Imobiliário: ABERTO → EM_ANALISE → AGUARDANDO_VISTORIA → APROVADO → PAGO
Serviço:     ABERTO → EM_ANALISE → APROVADO → PAGO
Qualquer estado não final → CANCELADO
```

Transições inválidas lançam `TransicaoInvalidaException` (HTTP 409). Cada cota tem no máximo um processo em andamento. No banco existe uma única tabela, `processo_pagamento`, com `tipo` e `estado` em texto. O mapper reconstrói o tipo e o estado pelo código gravado (`TipoProcesso.valueOf`, `EstadoProcesso.porNome`), sem herança JPA.

**Como estender o motor**

| Quero... | Faça |
|---|---|
| Um tipo novo de pagamento | constante em `TipoProcesso` + uma implementação de `FluxoProcesso` + rótulo em `Rotulos` |
| Um estado novo | record em `estado/`, incluir em `permits` e em `EstadoProcesso.todos()` + rótulo em `Rotulos` |
| Uma ação nova | método em `EstadoProcesso` (inválido por padrão) + método em `ProcessoPagamento` + caso de uso e endpoint |
| Um desvio novo por tipo | método em `FluxoProcesso` e em cada fluxo concreto |

Cada classe do motor traz no Javadoc uma linha com o padrão, o papel e a fonte. Fontes: **[GoF]** Gamma, Helm, Johnson e Vlissides, *Design Patterns* (1994); **[PoEAA]** Fowler, *Patterns of Enterprise Application Architecture* (2002).

## Guardrails para IA e revisão

O ArchUnit detecta o erro no build. Estas peças evitam que ele aconteça e impedem que ele entre:

| Camada | Peça | Onde |
|---|---|---|
| Orientar | Regras sempre ativas para qualquer agente de IA | `AGENTS.md` |
| Orientar | O padrão de arquitetura, o ADR e as receitas de código novo: `/arquiteto-padrao` | `.github/skills/arquiteto-padrao/` |
| Orientar | Como trazer um projeto legado para o padrão: `/adequar-ao-padrao` | `.github/skills/adequar-ao-padrao/` |
| Orientar | O papel e as proibições de cada pacote, lidos pela IA ao abrir a pasta | `package-info.java` |
| Detectar | Regras ArchUnit, testes de contrato, HQL validado na compilação | `src/test/...` |
| Bloquear | Donos das partes sensíveis | `.github/CODEOWNERS` |
| Bloquear | `./mvnw verify` na esteira corporativa | fora deste repositório |

**Neste repositório, a pasta `.github` é um modelo:** o GitHub só lê `.github` na raiz do repositório. Ao criar um projeto a partir deste exemplo, ela vai para a raiz, junto com o `AGENTS.md`.

O CODEOWNERS sozinho só **pede** revisão. Para que ele e a esteira **bloqueiem** o merge, configure no GitHub, em *Settings → Rules → Rulesets* (ou *Branches → Branch protection*), para a `main`:
1. *Require a pull request before merging*, com *Require review from Code Owners*.
2. *Require status checks to pass*, marcando o check da esteira que roda `./mvnw verify`.
3. *Block force pushes* e, se quiser, *Require linear history*.

## API

| Lado | Método e caminho | Descrição |
|---|---|---|
| Comando | `POST /clientes`, `PUT/DELETE /clientes/{id}` | CRUD de cliente (CPF único) |
| Comando | `POST /cotas`, `PUT/DELETE /cotas/{id}` | CRUD de cota |
| Comando | `POST /processos` `{cotaId, tipo}` | Cria processo (`IMOBILIARIO` ou `SERVICO`) |
| Comando | `POST /processos/{id}/iniciar-analise` · `concluir-analise` · `concluir-vistoria` · `pagar` · `cancelar` | Transições (`pagar` registra a ordem na Tesouraria) |
| Consulta | `GET /clientes` | Clientes com a quantidade de cotas |
| Consulta | `GET /cotas/{id}` | Cota com o nome do cliente e o processo atual |
| Canal web | `GET /web/v1/cotas/{id}` | Cota no padrão web, com o processo atual |
| Canal mobile | `GET /mobile/v1/cotas/{id}` · `GET /mobile/v1/clientes` | Card da cota; clientes com o total de cotas somado |
| Canal TAA | `GET /taa/v1/cotas/{id}` | Cota no padrão do terminal |
| Consulta | `GET /processos?tipo=&estado=&pagina=1&tamanho=20` | Processos filtrados e paginados, com o protocolo de pagamento |

Validação em duas camadas: o **formato** da entrada (obrigatório, tamanho, faixa) é checado com Bean Validation nos requests do entrypoint (`ClienteRequest`, `CotaRequest`), antes do use case, e as restrições aparecem no OpenAPI; a **regra de negócio** continua no domínio, que vale para qualquer chamador. Todo erro sai no mesmo formato, e `campos` só vem preenchido em erro de formato:

```json
{ "mensagem": "Dados inválidos", "campos": [ { "campo": "cpf", "erro": "não deve estar em branco" } ] }
```

Erros: 400 para entrada fora do formato ou violação de regra de domínio, 404 para recurso inexistente, 409 para conflito ou transição inválida, 503 para sistema externo indisponível. Excluir cliente com cotas, ou cota com processos, retorna 409.

## Como rodar

Requisitos: JDK 25 (`sdk env install` com SDKMAN) e Docker. O Dev Services sobe o PostgreSQL em dev e nos testes.

```bash
./mvnw quarkus:dev            # API em http://localhost:8080 e Swagger UI em /q/swagger-ui
./mvnw test                   # testes de domínio, ArchUnit e @QuarkusTest
./mvnw verify                 # o anterior + empacotamento + testes de integração (*IT)
docker compose up --build     # app empacotada + PostgreSQL (rode ./mvnw package antes)
```

Em produção, informe o banco com `DB_URL`, `DB_USER` e `DB_PASSWORD`, e a tesouraria com `TESOURARIA_URL`. Em `quarkus:dev`, sem uma tesouraria em `http://localhost:8089`, o `pagar` retorna 503.

Cópia do exemplo mantido em um repositório privado (branch `exemplo-cqrs`). Rode os comandos acima de dentro desta pasta.
