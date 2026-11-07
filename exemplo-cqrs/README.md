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

Os combinados acima são verificados a cada build, em `src/test/java/.../arquitetura/`. Cada grupo é uma classe com padrões relativos (`..command.domain..`), e `ArquiteturaTest` só os agrega. **Para outro projeto, copie a pasta e troque o pacote raiz em `ArquiteturaTest`** (em `@AnalyzeClasses` e na regra de ciclos). Regras de pacotes que podem ainda não existir, como `command.gateway`, passam vazias até a primeira classe aparecer.

| Grupo | O que garante |
|---|---|
| `RegrasDominio` | Domínio puro: sem JPA, Jakarta Data, CDI, transação, JAX-RS, Quarkus, Hibernate, MicroProfile, `shared` nem as outras camadas do command. |
| `RegrasCommand` | `command` nunca importa `query` (CQRS nos dois sentidos); use cases e entrypoint sem banco; `command.persistence` só é alcançado pela interface do domínio. **`command.gateway`** (futuro): toda implementação de interface do domínio que chama sistema externo mora lá, só traduz e ninguém a referencia diretamente. |
| `RegrasQuery` | `query` nunca importa `command`; direção `entrypoint → application → persistence`; o entrypoint não usa repositório Jakarta Data, só os records. |
| `RegrasShared` | `shared` não conhece os lados (por isso um gateway nunca importa o domínio); entidades só com getters/setters; client e DTOs externos não saem de `shared.gateway`; gateways só em use cases, no entrypoint (exceção → HTTP) e em `command.gateway`; um sistema externo não depende de outro. |
| `RegrasCanais` | Sem lista de canais: o subpacote é o canal, e um canal novo herda tudo. Canais e padrões não se conhecem; o padrão do canal X só é usado por `entrypoint.X`; formatação só no entrypoint; base comum neutra; use case sem canal; resource de canal nunca devolve tipo de `persistence`; Assembler e padrões são funções puras. |
| `RegrasTecnologia` | Onde cada tecnologia mora: `@Entity` em `shared.persistence`; sem Panache; Jakarta Data só em `command.persistence` e `query.persistence`; REST client e Fault Tolerance em `shared.gateway`; `@Path` e exception mappers no entrypoint; `@Transactional` só em use case. |
| `RegrasNomes` | O nome diz onde a classe mora: `*UseCase`, `*Resource`, `*Assembler`, `*Gateway`, `*Entity`, `*Mapper`/`*RepositoryAdapter`, `Padrao*`. |
| `ArquiteturaTest` | Sem ciclos entre os pacotes de segundo nível. |

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

`ProcessoPagamento` é abstrata, com `ProcessoImobiliario` e `ProcessoServico`. Os estados seguem o padrão State (`command.domain.estado`):

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
| Orientar | Receitas prontas: `/novo-caso-de-uso-escrita`, `/nova-consulta`, `/nova-integracao-externa`, `/novo-endpoint-de-canal` | `.github/prompts/*.prompt.md` |
| Orientar | O papel e as proibições de cada pacote, lidos pela IA ao abrir a pasta | `package-info.java` |
| Detectar | Regras ArchUnit, testes de contrato, HQL validado na compilação | `src/test/...` |
| Bloquear | CI (`./mvnw verify`), donos das partes sensíveis e checklist de PR | `.github/workflows/ci.yml`, `.github/CODEOWNERS`, `.github/pull_request_template.md` |

**Neste repositório, a pasta `.github` é um modelo:** o GitHub só lê `.github` na raiz do repositório. Ao criar um projeto a partir deste exemplo, ela vai para a raiz.

O CODEOWNERS sozinho só **pede** revisão. Para que ele e o CI **bloqueiem** o merge, configure no GitHub, em *Settings → Rules → Rulesets* (ou *Branches → Branch protection*), para a `main`:
1. *Require a pull request before merging*, com *Require review from Code Owners*.
2. *Require status checks to pass*, marcando o job `build` do CI.
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

Erros: 400 para validação de domínio, 404 para recurso inexistente, 409 para conflito ou transição inválida, 503 para sistema externo indisponível. Excluir cliente com cotas, ou cota com processos, retorna 409.

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
