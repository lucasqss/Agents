---
name: arquiteto-padrao
description: Padrão de arquitetura e de pastas dos projetos Java 25 + Quarkus 3.33 (componentes Domínio, ACL e BFF), com camadas no topo e agregados dentro delas. Use ao criar ou alterar componente, classe, caso de uso, regra de negócio, entidade, repositório Jakarta Data, consulta, integração externa, endpoint, canal, migration ou regra ArchUnit, ou ao decidir em qual pasta algo deve ficar.
---

# Arquiteto-padrão

Versão: 2026-11-08. Este documento é a referência da equipe e dos agentes de IA. A decisão completa e os motivos estão em [adr/ADR_001.md](adr/ADR_001.md). Se este texto e o ADR divergirem, **vale o ADR**: avise o dono para corrigir.

Para trazer um projeto antigo para este padrão, use a skill `adequar-ao-padrao`.

## Onde cada coisa mora

Para decidir onde um arquivo fica, responda a duas perguntas, nesta ordem:

1. **Que tipo de coisa é?** Isso dá a camada: `entrypoint`, `application`, `domain`, `persistence` ou `gateway`.
2. **De qual agregado é?** Isso dá a pasta dentro da camada: `cliente`, `cota`, `processo`.

O caminho é sempre um destes:

| Camada | Caminho | O que tem |
|---|---|---|
| Entrada | `entrypoint/<via>/<agregado>/` | Resource, Request, Response, Assembler |
| Caso de uso | `application/<agregado>/` | um `*UseCase` por classe |
| Regra de negócio | `domain/<agregado>/` | entidade, estados, interface de repositório |
| Banco | `persistence/<agregado>/` | `*Entity`, DataRepository, Mapper, Adapter, `*QueryRepository`, records |
| Sistema externo | `gateway/<sistema>/` | Client, DTOs, `*Gateway` |

A **via** é por onde a chamada chega: `rest` (API neutra), um canal (`web`, `mobile`, `taa`), `mensageria` ou `batch`. Primeiro vem a via, depois o agregado: `entrypoint/mobile/cota/`.

`gateway` é a única camada organizada por **sistema externo**, e não por agregado, porque o mesmo sistema pode servir a vários agregados.

### O projeto inteiro

```text
src/main/java/org/acme/consorcio/
├── entrypoint/
│   ├── rest/
│   │   ├── cliente/     ClienteResource.java, ClienteQueryResource.java, ClienteRequest.java
│   │   ├── cota/        CotaResource.java, CotaQueryResource.java, CotaRequest.java
│   │   └── processo/    ProcessoResource.java, ProcessoQueryResource.java
│   ├── web/
│   │   ├── PadraoWeb.java
│   │   └── cota/        CotaWebResource.java, CotaWebResponse.java, CotaWebAssembler.java
│   ├── mobile/
│   │   ├── PadraoMobile.java
│   │   ├── cliente/     ClienteMobileResource.java, ClientesMobileResponse.java, ClienteMobileAssembler.java
│   │   └── cota/        CotaMobileResource.java, CotaMobileResponse.java, CotaMobileAssembler.java
│   ├── taa/
│   │   ├── PadraoTaa.java
│   │   └── cota/        CotaTaaResource.java, CotaTaaResponse.java, CotaTaaAssembler.java
│   ├── apresentacao/    Formatador.java, Rotulos.java, PadraoApresentacao.java
│   ├── ExceptionMappers.java
│   ├── ErroResponse.java
│   └── IdCriado.java
├── application/
│   ├── cliente/         CriarClienteUseCase.java, ListarClientesUseCase.java
│   ├── cota/            CriarCotaUseCase.java, DetalharCotaUseCase.java
│   ├── processo/        CriarProcessoUseCase.java, PagarProcessoUseCase.java
│   ├── NaoEncontradoException.java
│   ├── ConflitoException.java
│   └── Pagina.java
├── domain/
│   ├── cliente/         Cliente.java, ClienteRepository.java
│   ├── cota/            Cota.java, CotaRepository.java
│   ├── processo/        ProcessoPagamento.java, ProcessoPagamentoRepository.java,
│   │                    TipoProcesso.java, FluxoProcesso.java, TransicaoInvalidaException.java, estado/
│   └── DominioException.java
├── persistence/
│   ├── cliente/         ClienteEntity.java, ClienteDataRepository.java, ClienteMapper.java,
│   │                    ClienteRepositoryAdapter.java, ClienteQueryRepository.java, ClienteResumo.java
│   ├── cota/            (mesmos papéis)
│   └── processo/        (mesmos papéis)
└── gateway/
    ├── tesouraria/      TesourariaClient.java, TesourariaGateway.java, OrdemPagamentoRequest.java
    └── SistemaExternoIndisponivelException.java
```

### Exemplo completo: "suspender a cota" (escrita)

| Arquivo | O que muda |
|---|---|
| `src/main/resources/db/migration/V3__cota_suspensao.sql` | coluna nova, se precisar gravar |
| `domain/cota/Cota.java` | método `suspender()`, com a regra |
| `application/cota/SuspenderCotaUseCase.java` | busca, chama `suspender()`, salva |
| `persistence/cota/CotaEntity.java` | campo novo |
| `persistence/cota/CotaMapper.java` | mapeia o campo novo |
| `entrypoint/rest/cota/SuspenderCotaRequest.java` | formato da entrada |
| `entrypoint/rest/cota/CotaResource.java` | endpoint novo |
| `src/test/java/.../domain/cota/CotaTest.java` | teste da regra, sem Quarkus |

### Exemplo completo: "extrato da cota no mobile" (leitura)

| Arquivo | O que muda |
|---|---|
| `persistence/cota/ExtratoCota.java` | record com os campos da consulta |
| `persistence/cota/CotaQueryRepository.java` | método `@Query` em HQL |
| `application/cota/ConsultarExtratoCotaUseCase.java` | filtros e paginação |
| `entrypoint/mobile/cota/ExtratoCotaMobileResponse.java` | contrato do front |
| `entrypoint/mobile/cota/CotaMobileAssembler.java` | monta e formata |
| `entrypoint/mobile/cota/CotaMobileResource.java` | endpoint novo |
| `src/test/java/.../integracao/CanaisContratoTest.java` | nomes exatos dos campos |

Repare que a leitura não passa por `domain/`.

### Errado × certo

| Errado | Certo | Por quê |
|---|---|---|
| `entrypoint/cota/mobile/` | `entrypoint/mobile/cota/` | Primeiro a via, depois o agregado. Sempre na mesma ordem, para todo mundo achar. |
| `entrypoint/mobile/CotaMobileResource.java` | `entrypoint/mobile/cota/CotaMobileResource.java` | Na raiz do canal só fica o `Padrao<Canal>`. |
| Um `MobileResource` com endpoints de cota e de cliente | Um Resource por agregado | Cada endpoint pertence a um agregado. |
| `shared/`, `common/`, `util/` | A raiz da camada, se for um tipo listado; senão, pergunte | Essas pastas viram depósito de tudo que ninguém sabe onde pôr. |
| `command/`, `query/` | `application/<agregado>/` e `persistence/<agregado>/` | Não separamos leitura e escrita por pasta. |
| Classe qualquer na raiz de `domain/` ou `application/` | `domain/<agregado>/` | A raiz só aceita o que vários agregados usam, e só os tipos listados abaixo. |
| `gateway/cota/TesourariaGateway.java` | `gateway/tesouraria/TesourariaGateway.java` | Gateway se organiza por sistema externo. |
| `*UseCase` com `criar()`, `atualizar()` e `excluir()` | `CriarCotaUseCase`, `AtualizarCotaUseCase`, `ExcluirCotaUseCase` | Uma classe, uma operação. Assim nenhuma cresce sem limite. |
| Tela lendo pelo repositório do domínio | `*QueryRepository` + record em `persistence/<agregado>/` | Consulta de tela não precisa de regra de negócio e fica mais rápida. |
| Caso de uso de escrita usando `*QueryRepository` | Método novo na interface de repositório do `domain/<agregado>/` | A decisão de escrita fala a língua do negócio. |
| `domain/cota/Cota.java` importando `domain/cliente/Cliente` | Guardar só `Long clienteId` | Um agregado não conhece o outro por dentro. Quem junta os dois é o caso de uso. |

### O que pode ficar na raiz de uma camada

Só o que 2 ou mais agregados usam, e só estes tipos:

| Camada | Pode ficar na raiz |
|---|---|
| `entrypoint/` | `ExceptionMappers`, `ErroResponse`, `IdCriado` |
| `application/` | exceções comuns (`NaoEncontradoException`, `ConflitoException`), `Pagina` |
| `domain/` | `DominioException` |
| `gateway/` | `SistemaExternoIndisponivelException` |

Se algo usado por vários agregados não está nessa lista, pare e pergunte ao dono.

## Regra de ouro

**Você não decide arquitetura. Você escolhe um dos tipos abaixo e segue a estrutura dele.** Se a tarefa não couber em nenhum tipo, pare e pergunte ao dono antes de escrever código. Não improvise pastas.

## Passo a passo

1. Descubra o **tipo do componente** no `README` dele (linha "Tipo X porque Y"). Se não houver, aplique a árvore de decisão e proponha o tipo ao dono.
2. Descubra o **agregado** e a **camada** (seção "Onde cada coisa mora").
3. Leia a referência indicada na tabela "Onde coloco X?" **antes** de escrever.
4. Para tarefa conhecida, siga a receita (seção "Receitas").
5. Rode `./mvnw verify`. Ele compila (o HQL é validado aqui) e roda domínio, ArchUnit, contrato e integração.
6. Se uma regra ArchUnit falhar, **o código está no lugar errado: mude o código, nunca a regra.** A falha traz um ID (ex.: `DOM-002`) e um "Corrija:". Se achar que a regra está errada, diga isso e pare.

## Árvore de decisão (qual tipo de componente?)

1. Tem regra de negócio ou é dono de dados? → **Domínio**.
2. Traduz entre o nosso vocabulário e o de um sistema externo (legado, outro departamento, Bacen, tesouraria)? → **ACL**, só se atender à regra de entrada. Senão vira `gateway/<sistema>/` dentro do domínio que usa.
3. Adapta ou compõe dados para um canal (web, mobile, TAA)? → **BFF**, só se atender à regra de entrada. Senão vira `entrypoint/<canal>/<agregado>/` dentro do domínio.
4. Nenhum dos casos → **não crie componente.**

Na dúvida entre componente separado e solução interna ao domínio, escolha a **interna**.

**Regra de entrada do ACL separado** (pelo menos uma): 2 ou mais domínios consomem o mesmo sistema externo; o sistema externo exige tratamento isolado (cache, rate limit, circuit breaker, disponibilidade ruim); política do outro departamento exige isolamento ou segurança separados.

**Regra de entrada do BFF separado** (pelo menos uma): a tela compõe dados de 2 ou mais domínios; o canal tem ritmo de entrega próprio e não pode depender do deploy do domínio.

**Outros departamentos consumindo o nosso componente não é ACL:** é o `entrypoint` do domínio, com contrato versionado.

**Proibido: componente "repassador"** (só encaminha chamadas, sem traduzir, compor nem coordenar). Traduz → ACL. Compõe para canal → BFF. Coordena fluxo → caso de uso do domínio dono. Só repassa → API gateway ou barramento do banco, sem componente nosso.

## Tipos ACL e BFF (mesmo esqueleto)

O tipo Domínio é o da seção "Onde cada coisa mora". ACL e BFF não têm regra de negócio nem banco:

```text
entrypoint/               # ACL: rest ou mensageria | BFF: entrypoint/<canal>/
translator/               # tradução pura, sem framework e sem estado
gateway/<sistema>/        # ACL: sistema externo | BFF: API do componente de domínio
```

Sem `domain`, sem `persistence`, sem `@Entity`, sem `@Transactional`. O ACL protege o domínio do mundo de fora; o BFF adapta o domínio para o canal.

## Onde coloco X?

| Preciso de... | Coloque em | Leia |
|---|---|---|
| Regra de negócio, validação de negócio, transição de estado | `domain/<agregado>/`, na entidade. Nunca no use case, no resource nem na `*Entity` | [dominio](references/dominio.md) |
| Consultar algo para decidir uma escrita (existe? está ativo?) | método novo em `domain/<agregado>/XRepository`, implementado em `persistence/<agregado>/`. Nunca um `*QueryRepository` | [use-cases](references/use-cases.md) |
| Validar o formato da entrada (obrigatório, tamanho, faixa) | `XRequest` (record) em `entrypoint/<via>/<agregado>/`, com Bean Validation | [canais](references/canais.md) |
| Gravar algo | `application/<agregado>/XUseCase`: carrega pelo repositório, chama o método de negócio, salva | [use-cases](references/use-cases.md) |
| Tela ou listagem nova | record + `@Query` HQL em `persistence/<agregado>/`, `application/<agregado>/XUseCase`, resource | [persistência](references/persistencia-jakarta-data.md) |
| Repositório, entidade JPA, mapper | `persistence/<agregado>/` | [persistência](references/persistencia-jakarta-data.md) |
| Chamar sistema de outra área | `gateway/<sistema>/XGateway`. Só **use cases** chamam gateways | [gateway](references/gateway.md) |
| Endpoint REST ou de canal | `entrypoint/rest/<agregado>/` ou `entrypoint/<canal>/<agregado>/` | [canais](references/canais.md) |
| Formatar para uma tela (moeda, máscara, rótulo) | Assembler em `entrypoint/<canal>/<agregado>/`, usando `Padrao<Canal>` | [canais](references/canais.md) |
| Número com significado de negócio (total pago, saldo) | uma consulta em `persistence/<agregado>/`, não o Assembler | [canais](references/canais.md) |
| Consumidor de fila ou tópico | `entrypoint/mensageria/<agregado>/` (só chama caso de uso) | |
| Job agendado ou processamento de arquivo | `entrypoint/batch/<agregado>/` (só chama caso de uso) | |
| Coordenar dois agregados | um caso de uso, em `application/<agregado principal>/` | [use-cases](references/use-cases.md) |
| Conversão entre modelo externo e interno (ACL/BFF) | `translator/` | |
| Mudar o banco | nova migration + campo na `*Entity` + mapper (escrita) e/ou HQL (leitura) | [migrations](references/migrations.md) |
| Criar ou mudar regra ArchUnit | só com pedido do dono | [archunit](references/archunit.md) |
| Algo "que vários agregados usam" | raiz da camada, se for um tipo listado. Senão, **pare e pergunte ao dono** | |

Antes de terminar: confira [estilo e proibidos](references/estilo-e-proibidos.md) e os [testes esperados](references/testes.md).

## Receitas

Siga a ordem e pare se algum passo exigir quebrar uma regra.

- [Novo caso de uso de escrita](references/receitas/novo-caso-de-uso-escrita.md)
- [Nova consulta](references/receitas/nova-consulta.md)
- [Novo endpoint de canal](references/receitas/novo-endpoint-de-canal.md)
- [Nova integração externa](references/receitas/nova-integracao-externa.md)

## Convenções

- **Javadoc de cada classe de padrão** declara padrão, papel e fonte, de forma curta. Ex.: `Padrão: State, estado concreto [GoF].` ou `Padrão: Data Mapper [PoEAA].`. A citação completa dos livros fica uma vez só, no `package-info.java`.
- A descrição de cada pacote vive em `package-info.java`.
- `entrypoint/` nunca acessa `persistence/` nem `gateway/` direto.
- `domain/` não depende de `entrypoint/`, `application/`, `persistence/`, `gateway/` nem de framework.
- `@Transactional` só em `application/`.
- `*UseCase` tem um único método público.

## Quando parar e pedir ao dono

- Criar componente, tipo ou agregado novo.
- Extrair ACL ou BFF para componente separado.
- Separar o componente em módulos (só faz sentido quando as partes não têm join nem transação em comum).
- Alterar, desligar ou afrouxar regra ArchUnit.
- Editar uma migration existente, ou renomear/remover campo de um `*Response` de canal.
- Processamento batch pesado (alto volume, janela noturna, escala própria).

## Por que é assim

- **Camadas no topo:** nossos agregados se cruzam o tempo todo em consultas e validações. Separar por módulo criaria uma pasta a mais sem separar nada de verdade.
- **Agregado dentro da camada:** "que tipo de coisa é e de qual agregado" tem resposta mecânica. Qualquer pessoa acha o arquivo, e qualquer revisor vê o que está fora do lugar.
- **Sem `shared`:** essa pasta vira depósito. A raiz da camada aceita só uma lista curta de tipos.
- **Sem pastas de CQRS:** elas custam caro para o que entregam. Mantemos só o que importa: tela não passa pelo domínio.
- **Regra de negócio no domínio, em Java puro:** dá para testar sem subir o Quarkus e sem banco.
- **Um caso de uso por classe:** é o limite que impede classes gigantes.

Referências: Mark Richards, *Fundamentals of Software Architecture* e *Software Architecture: The Hard Parts*; Martin Fowler, *Patterns of Enterprise Application Architecture* e blikis de CQRS e camadas. Os detalhes estão no [ADR](adr/ADR_001.md).
