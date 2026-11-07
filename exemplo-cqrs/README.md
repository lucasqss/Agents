# Consórcio CQRS: exemplo de referência

Projeto didático para validar uma arquitetura CQRS com domínio puro em Quarkus, aplicada a processos de pagamento de consórcio. Ele não implementa o negócio completo.

**Stack:** Java 25, Quarkus 3.33, Hibernate ORM com Panache, Jakarta Data (Hibernate Data Repositories), PostgreSQL (Dev Services), Flyway, JUnit 5, RestAssured e ArchUnit.

## Arquitetura

Hexagonal/clean **pragmática**, com os padrões do *Patterns of Enterprise Application Architecture* (Fowler). Só existe uma pasta quando o problema pede, e só há interface (porta) onde ela protege alguma coisa.

Pacote raiz `br.com.claudio.consorcio`:

```
shared/
  persistence/          entidades JPA: modelo do NOSSO banco, só mapeamento, getters e setters
  integracao/<sistema>/ Gateway de cada sistema externo + client REST, DTOs externos e resiliência
command/
  entrypoint/           REST de escrita + tradução de exceções para HTTP
  application/          use cases (Service Layer): transação e orquestração; chamam repositórios e gateways
  domain/               Domain Model puro: Cliente, Cota, ProcessoPagamento (+ estados) e interfaces de repositório
  persistence/          repositórios Panache + Data Mappers domínio <-> JPA escritos à mão
query/
  entrypoint/           REST de leitura
  application/          use cases de leitura (normalização de filtros, paginação)
  persistence/          repositórios Jakarta Data com @Query HQL + records projetados (read models)
```

- **Escrita:** `entrypoint → application → domain ← persistence`. A persistência implementa as interfaces que o domínio declara (Separated Interface), então o use case não vê JPA.
- **Leitura:** `entrypoint → application → persistence`. Não há domínio para proteger, então também não há porta. O repositório Jakarta Data já é uma interface, com a implementação gerada, e projeta direto no record.

### Dois tipos de infraestrutura

| | Nosso banco | Sistemas externos (corporativos) |
|---|---|---|
| Modelo | nosso, versionado por migrations do Flyway | deles; muda sem aviso |
| Onde fica | `command.persistence` / `query.persistence` + entidades em `shared.persistence` | `shared.integracao.<sistema>`, atrás de um **Gateway** |
| Acoplamento | a escrita mapeia para o domínio; a leitura projeta direto do banco | o Gateway traduz; client e DTOs externos não saem do pacote |
| Transação | a mesma do use case | fora do nosso controle (veja o trade-off abaixo) |
| Falha | rara, vira 500 | esperada: timeout, retry e circuit breaker no client; vira `IntegracaoIndisponivelException` (503) |
| Teste | PostgreSQL real (Dev Services) | **Service Stub** (`TesourariaFake`), nunca o sistema real |
| Config | datasource (`DB_*`); permite réplica de leitura no futuro | `quarkus.rest-client.<sistema>.*` (`TESOURARIA_URL`) |

Só os use cases chamam sistemas externos, por isso eles usam o Gateway direto, sem porta. Se um dia uma regra do **domínio** precisar de um sistema externo, aí sim o domínio declara uma interface e a implementação delega ao Gateway.

**Exemplo: a Tesouraria.** Ao pagar, `AvancarProcessoUseCase` faz a transição primeiro (assim uma transição inválida nem chega à tesouraria), depois chama `TesourariaGateway.registrarPagamento` e guarda o protocolo no processo. Se a tesouraria falhar, a exceção desfaz a transação e o processo continua APROVADO. Trade-off: a chamada externa acontece dentro da transação do banco. Em produção, quando isso pesar, use um outbox.

### Padrões (PoEAA)

Service Layer (use cases), Domain Model (`command.domain`), Repository + Data Mapper (`command.persistence`), Separated Interface (repositórios do domínio), Gateway e Service Stub (integrações). Num módulo que só orquestra, sem regra de negócio de verdade, um **Transaction Script** no use case basta, sem pasta `domain`.

### Regras de dependência (`ArquiteturaTest`, ArchUnit)

1. **Domínio puro:** `command.domain` não depende de JPA, Quarkus, Hibernate, `shared` nem das outras camadas do command.
2. **Entidades sem comportamento:** em `shared.persistence` só há campos, mapeamento e getters/setters.
3. **Use cases sem banco:** `command.application` não depende de `command.persistence`, `shared.persistence`, JPA nem Panache.
4. **Consulta independente:** `query` nunca importa `command`. O HQL dos repositórios Jakarta Data é validado na compilação pelo Hibernate Processor.
5. **Shared neutro:** `shared` não depende de `command` nem de `query`.
6. **Integrações só via Gateway:** fora de `shared.integracao`, só `*Gateway` e `*Exception` são visíveis, e apenas para `application` e `entrypoint`.
7. **Sem ciclos** entre os pacotes.

### Processo de pagamento

`ProcessoPagamento` é abstrata, com `ProcessoImobiliario` e `ProcessoServico`. Os estados seguem o padrão State (`command.domain.estado`):

```
Imobiliário: ABERTO → EM_ANALISE → AGUARDANDO_VISTORIA → APROVADO → PAGO
Serviço:     ABERTO → EM_ANALISE → APROVADO → PAGO
Qualquer estado não final → CANCELADO
```

Transições inválidas lançam `TransicaoInvalidaException` (HTTP 409). Cada cota tem no máximo um processo em andamento. No banco existe uma única tabela, `processo_pagamento`, com `tipo` e `estado` em texto. O mapper reconstrói a subclasse e o estado, sem herança JPA.

## API

| Lado | Método e caminho | Descrição |
|---|---|---|
| Comando | `POST /clientes`, `PUT/DELETE /clientes/{id}` | CRUD de cliente (CPF único) |
| Comando | `POST /cotas`, `PUT/DELETE /cotas/{id}` | CRUD de cota |
| Comando | `POST /processos` `{cotaId, tipo}` | Cria processo (`IMOBILIARIO` ou `SERVICO`) |
| Comando | `POST /processos/{id}/iniciar-analise` · `concluir-analise` · `concluir-vistoria` · `pagar` · `cancelar` | Transições (`pagar` registra a ordem na Tesouraria) |
| Consulta | `GET /clientes` | Clientes com a quantidade de cotas |
| Consulta | `GET /cotas/{id}` | Cota com o nome do cliente e o processo atual |
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
