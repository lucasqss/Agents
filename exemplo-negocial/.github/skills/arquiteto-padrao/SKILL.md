---
name: arquiteto-padrao
description: Padrão de arquitetura e de pastas dos projetos Java 25 + Quarkus 3.33 (componentes Negocial, ACL e BFF), com camadas no topo e agregados dentro delas. Use ao criar ou alterar componente, tradução de sistema externo (ACL), tela ou composição de canal (BFF), classe, caso de uso, regra de negócio, entidade, repositório Jakarta Data, consulta, integração externa, endpoint, canal, migration ou regra ArchUnit, ou ao decidir em qual pasta algo deve ficar. Não use para testes de aceitação de usuário, que ficam com as skills teste-aceitacao-modelos (a partir do requisito) e teste-aceitacao-legado (a partir do código existente).
---

# Arquiteto-padrão

Versão: 2026-11-12 (tipos Negocial, ACL e BFF). Este documento é a referência da equipe e dos agentes de IA. A decisão completa e os motivos estão em [adr/ADR_001.md](adr/ADR_001.md). Se este texto e o ADR divergirem, **vale o ADR**: avise o dono para corrigir.

Para trazer um projeto antigo para este padrão, use a skill `adequar-ao-padrao`. Para testes de aceitação de usuário, use a skill `teste-aceitacao-modelos` (a partir do requisito) ou `teste-aceitacao-legado` (a partir do código existente), e não esta.

Nomes como `cota`, `cliente`, `processo` e `tesouraria` nos exemplos são ilustrativos: não procure essas classes no repositório. Modelos de cada peça estão em [exemplos de código](references/exemplos-codigo.md).

## Onde cada coisa mora

Esta seção descreve o tipo **Negocial**, o mais comum. Se o componente é ACL ou BFF (veja o `README` dele ou `src/test/java/<pacote raiz>/arquitetura/Tipo.java`), o mapa de pastas é o de [tipo ACL](references/tipo-acl.md) ou [tipo BFF](references/tipo-bff.md).

Para decidir onde um arquivo fica, responda a duas perguntas, nesta ordem:

1. **Que tipo de coisa é?** Isso dá a camada: `entrypoint`, `application`, `domain`, `persistence` ou `gateway`.
2. **De qual agregado é?** Isso dá a pasta dentro da camada: `cliente`, `cota`, `processo`.

O caminho é sempre um destes:

| Camada | Caminho | O que tem |
|---|---|---|
| Entrada | `entrypoint/<via>/<agregado>/` | Resource, Request, Response, Assembler |
| Caso de uso | `application/<agregado>/` | um `*UseCase` por classe e os records `DadosX` (comando de entrada da escrita) |
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
│   ├── cliente/         CriarClienteUseCase.java, ListarClientesUseCase.java, DadosCliente.java
│   ├── cota/            CriarCotaUseCase.java, DetalharCotaUseCase.java, DadosCota.java
│   ├── processo/        CriarProcessoUseCase.java, PagarProcessoUseCase.java
│   ├── NaoEncontradoException.java
│   ├── ConflitoException.java
│   └── Pagina.java
├── domain/
│   ├── cliente/         Cliente.java, ClienteRepository.java
│   ├── cota/            Cota.java, CotaRepository.java
│   ├── processo/        ProcessoPagamento.java, ProcessoPagamentoRepository.java
│   │   ├── fluxo/       EstadoProcesso.java, AcaoProcesso.java, FluxoProcesso.java, Transicao.java,
│   │   │                TransicaoInvalidaException.java
│   │   └── tipo/        TipoProcesso.java, FluxoImobiliario.java, FluxoServico.java
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
| `src/test/java/.../integracao/CanaisContratoTest.java` (crie se ainda não existir) | nomes exatos dos campos |

Repare que a leitura não passa por `domain/`.

### Subpastas dentro do agregado

Um agregado pode ter subpastas em `domain/<agregado>/` quando um conceito dele junta várias classes. O nome é o do conceito, nunca o de um padrão ou de uma técnica: `fluxo/` (estados, ações e a tabela de transições), `tipo/` (o que muda por tipo). A raiz do agregado fica com a entidade e a interface de repositório. As dependências vão num sentido só, sem ciclo (GER-002): `fluxo` ← `tipo` ← raiz do agregado.

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

Só o que 2 ou mais agregados usam, e só estes tipos (além deles, toda camada pode ter o próprio `package-info.java`):

| Camada | Pode ficar na raiz |
|---|---|
| `entrypoint/` | `ExceptionMappers`, `ErroResponse`, `IdCriado` |
| `application/` | exceções comuns (`NaoEncontradoException`, `ConflitoException`), `Pagina` |
| `domain/` | `DominioException`, `Dinheiro` |
| `gateway/` | `SistemaExternoIndisponivelException` |

Se algo usado por vários agregados não está nessa lista, pare e pergunte ao dono.

## Regra de ouro

**Você não decide arquitetura. Você escolhe um dos tipos abaixo e segue a estrutura dele.** Se a tarefa não couber em nenhum tipo, pare e pergunte ao dono antes de escrever código. Não improvise pastas.

## Passo a passo

1. Descubra o **tipo do componente**: `Tipo.NOME` em `src/test/java/<pacote raiz>/arquitetura/Tipo.java` e a linha "Tipo X porque Y" do `README`. Se não houver, aplique a árvore de decisão e proponha o tipo ao dono.
2. Confira se `src/test/java/<pacote raiz>/arquitetura/` existe. Se não existir, instale as regras do tipo seguindo [archunit](references/archunit.md).
3. Descubra o **agregado** e a **camada** (seção "Onde cada coisa mora"; em ACL e BFF, a referência do tipo).
4. Leia a referência indicada na tabela "Onde coloco X?" **antes** de escrever.
5. Para tarefa conhecida, siga a receita (seção "Receitas").
6. Rode `./mvnw verify`. Ele compila (o HQL é validado aqui) e roda domínio, ArchUnit, contrato e integração. Se não houver `mvnw`, ou se ele falhar por fim de linha CRLF, use `mvn verify`.
7. Se uma regra ArchUnit falhar, **o código está no lugar errado: mude o código, nunca a regra.** A falha traz um ID (ex.: `DOM-002`) e um "Corrija:". Se achar que a regra está errada, diga isso e pare.

## Árvore de decisão (qual tipo de componente?)

| | Negocial | ACL | BFF |
|---|---|---|---|
| Existe para | regra e dados de um bounded context | traduzir entre um sistema externo e o nosso vocabulário | adaptar os negociais para um canal |
| Banco | um, só dele | nenhum de negócio | nenhum |
| Regra de negócio | sim, só aqui | não | não |
| Unidade | um por contexto | um por sistema externo | um por canal |
| Pastas | `entrypoint`, `application`, `domain`, `persistence`, `gateway` | `entrypoint`, `application`, `translator`, `gateway` | `entrypoint`, `application`, `gateway` |

1. Tem regra de negócio ou é dono de dados? → **Negocial**. Processo longo com estado próprio também é Negocial.
2. Traduz entre o nosso vocabulário e o de um sistema externo (legado, outro departamento, Bacen, tesouraria)? → **ACL**, só se atender à regra de entrada. Senão vira `gateway/<sistema>/` dentro do negocial que usa.
3. Adapta ou compõe dados para um canal (web, mobile, TAA)? → **BFF**, só se atender à regra de entrada. Senão vira `entrypoint/<canal>/<agregado>/` dentro do negocial.
4. Serve leitura que filtra, ordena ou pagina por dados de 2 ou mais negociais? → tipo **Consulta**, reservado no ADR: **pare e pergunte ao dono**. Nunca leia o banco de outro componente.
5. Nenhum dos casos → **não crie componente.**

Na dúvida entre componente separado e solução interna ao negocial, escolha a **interna**.

**Regra de entrada do ACL separado** (pelo menos uma): 2 ou mais negociais consomem o mesmo sistema externo; o sistema externo exige tratamento isolado (cache, rate limit, circuit breaker, disponibilidade ruim); política do outro departamento exige isolamento ou segurança separados.

**Regra de entrada do BFF separado** (pelo menos uma): a tela compõe dados de 2 ou mais negociais; o canal tem ritmo de entrega próprio e não pode depender do deploy do negocial.

**Outros departamentos consumindo o nosso componente não é ACL:** é o `entrypoint` do negocial, com contrato versionado.

**Proibido: componente "repassador"** (só encaminha chamadas, sem traduzir, compor nem coordenar). Traduz → ACL. Compõe para canal → BFF. Coordena fluxo → caso de uso do negocial dono. Só repassa → API gateway ou barramento do banco, sem componente nosso.

### Quem chama quem

| Quem chama | Pode chamar |
|---|---|
| Front | só o BFF do seu canal |
| BFF | componentes negociais |
| Negocial | outro negocial (API ou evento) e ACL |
| ACL de saída | o sistema externo |
| ACL de entrada | o negocial dono do assunto |

Proibido: negocial chamar BFF; BFF chamar BFF, ACL ou sistema externo; ACL chamar ACL; qualquer componente ler o banco de outro. O `verify` de um componente não enxerga isso: antes de criar um `gateway/<sistema>/`, confira nesta tabela se o destino é permitido.

## Tipos ACL e BFF

O que vale nos três tipos: **entrypoint só chama caso de uso; só caso de uso chama `*Gateway`; um `*UseCase` por operação.** O que muda:

- **ACL:** `entrypoint → application → gateway e translator`. O Gateway devolve o modelo externo, o caso de uso passa pelo `*Translator` e devolve o record do nosso vocabulário. Leia [tipo ACL](references/tipo-acl.md).
- **BFF:** `entrypoint/<canal> → application → gateway/<negocial>`. O caso de uso compõe, o Assembler formata. Não tem `translator`. Leia [tipo BFF](references/tipo-bff.md).

Nenhum dos dois tem `domain`, `persistence`, JPA nem transação: o `verify` falha (GER-001, ACL-001, BFF-001).

## Onde coloco X?

| Preciso de... | Coloque em | Leia |
|---|---|---|
| Regra de negócio, validação de negócio | `domain/<agregado>/`, na entidade. Nunca no use case, no resource nem na `*Entity` | [dominio](references/dominio.md) |
| Ciclo de vida: estado, ação ou transição nova; fluxo que muda por tipo | enums e tabela de transições em `domain/<agregado>/fluxo/`; desvios de cada tipo em `domain/<agregado>/tipo/`. Sem `if (tipo == ...)` na entidade | [dominio](references/dominio.md) |
| Valor com regra própria e sem identidade (dinheiro, CPF, período) | `record` em `domain/<agregado>/`, ou na raiz de `domain/` se vários agregados usam. Dinheiro é sempre `Dinheiro`, nunca `BigDecimal` solto | [valores](references/valores.md) |
| Consultar algo para decidir uma escrita (existe? está ativo?) | método novo em `domain/<agregado>/XRepository`, implementado em `persistence/<agregado>/`. Nunca um `*QueryRepository` | [use-cases](references/use-cases.md) |
| Validar o formato da entrada (obrigatório, tamanho, faixa) | `XRequest` (record) em `entrypoint/<via>/<agregado>/`, com Bean Validation | [canais](references/canais.md) |
| Gravar algo | `application/<agregado>/XUseCase`: carrega pelo repositório, chama o método de negócio, salva | [use-cases](references/use-cases.md) |
| Tela ou listagem nova | record + `@Query` HQL em `persistence/<agregado>/`, `application/<agregado>/XUseCase`, resource | [persistência](references/persistencia-jakarta-data.md) |
| Repositório, entidade JPA, mapper | `persistence/<agregado>/` | [persistência](references/persistencia-jakarta-data.md) |
| Chamar sistema de outra área | `gateway/<sistema>/XGateway`. Só **use cases** chamam gateways. Única exceção: o `XGatewayAdapter`, quando uma regra do domínio precisa do sistema externo | [gateway](references/gateway.md) |
| Usar dependência do catálogo corporativo (client ou contrato gerado de outro sistema) | só em `gateway/<sistema>/` (consumir) ou `entrypoint/rest/<agregado>/` (prover). O caso de uso recebe tipo simples, `DadosX` ou record próprio do gateway | [gateway](references/gateway.md), [canais](references/canais.md) |
| Endpoint REST ou de canal | `entrypoint/rest/<agregado>/` ou `entrypoint/<canal>/<agregado>/` | [canais](references/canais.md) |
| Formatar para uma tela (moeda, máscara, rótulo) | Assembler em `entrypoint/<canal>/<agregado>/`, usando `Padrao<Canal>` | [canais](references/canais.md) |
| Número com significado de negócio (total pago, saldo) | uma consulta em `persistence/<agregado>/`, não o Assembler | [canais](references/canais.md) |
| Consumidor de fila ou tópico | `entrypoint/mensageria/<agregado>/` (só chama caso de uso) | |
| Job agendado ou processamento de arquivo | `entrypoint/batch/<agregado>/` (só chama caso de uso) | |
| Duas requisições ao mesmo tempo, ou a mesma duas vezes | versão no agregado; bloqueio quando o caso de uso confere e depois age | [concorrência](references/concorrencia.md) |
| Gravar e chamar sistema externo na mesma operação | caso de uso, numa das três formas descritas; a escolha do projeto fica em ADR | [transação e sistema externo](references/transacao-e-sistema-externo.md) |
| Usar um padrão que o projeto ainda não usa (Special Case, Query Object, Plugin...) | confira antes se é usado, proibido ou sob demanda | [padrões do PoEAA](references/padroes-poeaa.md) |
| Coordenar dois agregados | um caso de uso, em `application/<agregado principal>/` | [use-cases](references/use-cases.md) |
| Conversão entre modelo externo e o nosso (só em ACL) | `translator/<sistema>/XTranslator` | [tipo ACL](references/tipo-acl.md) |
| Tela que junta dados de vários negociais (só em BFF) | `application/<recurso>/XUseCase` + Assembler em `entrypoint/<canal>/<recurso>/` | [tipo BFF](references/tipo-bff.md) |
| Mudar o banco | nova migration + campo na `*Entity` + mapper (escrita) e/ou HQL (leitura) | [migrations](references/migrations.md) |
| Criar ou mudar regra ArchUnit | só com pedido do dono | [archunit](references/archunit.md) |
| Configurar `pom.xml` (versões, dependências, compilador) | `pom.xml` do componente | [pom obrigatório](references/pom-obrigatorio.md) |
| Algo "que vários agregados usam" | raiz da camada, se for um tipo listado. Senão, **pare e pergunte ao dono** | |

Antes de terminar: confira [estilo e proibidos](references/estilo-e-proibidos.md) e os [testes esperados](references/testes.md).

## Receitas

Siga a ordem e pare se algum passo exigir quebrar uma regra.

- [Nova funcionalidade, de ponta a ponta](references/receitas/nova-funcionalidade.md)
- [Novo agregado](references/receitas/novo-agregado.md)
- [Novo caso de uso de escrita](references/receitas/novo-caso-de-uso-escrita.md)
- [Novo estado, ação ou tipo em um fluxo](references/receitas/novo-estado-acao-ou-tipo.md)
- [Nova consulta](references/receitas/nova-consulta.md)
- [Novo endpoint de canal](references/receitas/novo-endpoint-de-canal.md)
- [Nova integração externa](references/receitas/nova-integracao-externa.md)
- [Nova tradução em um ACL](references/tipo-acl.md#receita-nova-operação-de-tradução)
- [Nova tela em um BFF](references/tipo-bff.md#receita-nova-tela)
- [Nova decisão de arquitetura (ADR)](references/receitas/nova-decisao.md)

## Convenções

- **Javadoc de cada classe de padrão** declara padrão, papel e fonte, de forma curta. Ex.: `Padrão: State dirigido por tabela [GoF].` ou `Padrão: Data Mapper [PoEAA].`. A citação completa dos livros fica uma vez só, no `package-info.java`.
- A descrição de cada pacote vive em `package-info.java`, inclusive na raiz da camada.
- De `gateway/<sistema>/`, o caso de uso só importa o `*Gateway`, as `*Exception` e os records próprios; nunca o client, o DTO externo nem classe do catálogo corporativo.
- `entrypoint/` nunca acessa `gateway/` direto. De `persistence/<agregado>/`, só pode importar os records projetados; nunca `*Entity`, `*QueryRepository`, `*DataRepository`, `*Mapper` nem `*RepositoryAdapter`.
- `domain/` não depende de `entrypoint/`, `application/`, `persistence/`, `gateway/` nem de framework.
- `@Transactional` só em `application/`.
- `*UseCase` tem um único método público.

## Quando parar e pedir ao dono

- Criar componente, tipo ou agregado novo, ou trocar o `Tipo.java` de um componente.
- Extrair ACL ou BFF para componente separado.
- Qualquer coisa que pareça o tipo Consulta, ou guardar estado em um ACL (cache, mensagem já processada).
- BFF que precisaria gravar em dois negociais, ou chamar ACL ou sistema externo.
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
