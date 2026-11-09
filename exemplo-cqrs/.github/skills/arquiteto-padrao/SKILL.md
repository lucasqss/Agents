---
name: arquiteto-padrao
description: Padrão de arquitetura e de pastas dos projetos Java 25 + Quarkus 3.33 (componentes Domínio, ACL e BFF). Use ao criar ou alterar componente, módulo, classe, caso de uso, regra de negócio, entidade, repositório Jakarta Data, consulta, integração externa, endpoint, canal, migration ou regra ArchUnit, ou ao decidir em qual pasta algo deve ficar.
---

# Arquiteto-padrão

Versão: 2026-11-08. A decisão completa está em [adr/ADR_001.md](adr/ADR_001.md). Se esta skill e o ADR divergirem, **vale o ADR**: avise o dono para corrigir a skill.

Para trazer um projeto legado para este padrão, use a skill `adequar-ao-padrao`.

## Regra de ouro

**Você não decide arquitetura. Você escolhe um dos tipos abaixo e segue a estrutura dele.** Se a tarefa não cabe em nenhum tipo, pare e pergunte ao dono antes de escrever código. Não improvise pastas.

## Passo a passo

1. Descubra o **tipo do componente** no `README` dele (linha "Tipo X porque Y"). Se não houver, aplique a árvore de decisão e proponha o tipo ao dono.
2. Descubra o **módulo** (pasta de negócio no topo, ex.: `processo`, `pagamento`).
3. Use a tabela "Onde coloco X?" para escolher a pasta e leia a referência indicada **antes** de escrever.
4. Para tarefa conhecida, siga a receita (seção "Receitas").
5. Rode `./mvnw verify`. Ele compila (o HQL é validado aqui) e roda domínio, ArchUnit, contrato e integração.
6. Se uma regra ArchUnit falhar, **o código está no lugar errado: mude o código, nunca a regra.** A falha traz um ID (ex.: `DOM-002`) e um "Corrija:". Se achar que a regra está errada, diga isso e pare.

## Árvore de decisão (qual tipo de componente?)

1. Tem regra de negócio ou é dono de dados? → **Domínio**.
2. Traduz entre o nosso vocabulário e o de um sistema externo (legado, outro departamento, Bacen, tesouraria)? → **ACL**, só se atender à regra de entrada. Senão vira `gateway/<sistema>` dentro do domínio que usa.
3. Adapta ou compõe dados para um canal (web, mobile, TAA)? → **BFF**, só se atender à regra de entrada. Senão vira `entrypoint/<canal>` dentro do domínio.
4. Nenhum dos casos → **não crie componente.**

Na dúvida entre componente separado e solução interna ao domínio, escolha a **interna**.

**Regra de entrada do ACL separado** (pelo menos uma): 2 ou mais domínios consomem o mesmo sistema externo; o sistema externo exige tratamento isolado (cache, rate limit, circuit breaker, disponibilidade ruim); política do outro departamento exige isolamento ou segurança separados.

**Regra de entrada do BFF separado** (pelo menos uma): a tela compõe dados de 2 ou mais domínios; o canal tem ritmo de entrega próprio e não pode depender do deploy do domínio.

**Outros departamentos consumindo o nosso componente não é ACL:** é o `entrypoint` do domínio, com contrato versionado.

**Proibido: componente "repassador"** (só encaminha chamadas, sem traduzir, compor nem coordenar). Traduz → ACL. Compõe para canal → BFF. Coordena fluxo → caso de uso do domínio dono. Só repassa → API gateway ou barramento do banco, sem componente nosso.

## Estruturas

### Tipo Domínio

Sempre tem módulos no topo, mesmo que seja um só. Não existe pasta `shared`.

```text
<modulo>/
├── entrypoint/
│   ├── rest/
│   ├── mensageria/
│   ├── batch/            # jobs agendados que só chamam casos de uso
│   └── <canal>/          # web, mobile, taa
├── application/          # um caso de uso por classe; orquestração do fluxo do módulo
├── domain/               # regras, estados, invariantes, interfaces de repositório
├── persistence/          # *Entity, Jakarta Data, Data Mapper, adapter
└── gateway/<sistema>/    # sistema externo usado por este módulo
```

**CQRS é opcional por módulo**, só quando a leitura é muito diferente da escrita (várias projeções ou canais). Introduzir CQRS é decisão do dono.

```text
<modulo>/
├── command/              # entrypoint, application, domain, persistence (repositório e mapper)
├── query/                # entrypoint/<canal>, application, persistence (HQL + records)
├── persistence/          # *Entity, usadas pelos dois lados
└── gateway/<sistema>/
```

`command` nunca importa `query`, e `query` nunca importa `command`.

Nas referências, `domain` significa `<modulo>/domain` ou `<modulo>/command/domain` num módulo CQRS. O mesmo vale para `application`.

**Módulo `apresentacao`:** única exceção à regra do `shared`. Guarda `Formatador`, `Rotulos` e `Padrao<Canal>` e não conhece nenhum outro módulo.

### Tipos ACL e BFF (mesmo esqueleto)

```text
entrypoint/               # ACL: rest ou mensageria | BFF: entrypoint/<canal>
translator/               # tradução pura, sem framework e sem estado
gateway/<sistema>/        # ACL: sistema externo | BFF: API do componente de domínio
```

Sem `domain`, sem `persistence`, sem `@Entity`, sem `@Transactional`. O ACL protege o domínio do mundo de fora; o BFF adapta o domínio para o canal.

## Onde coloco X?

| Preciso de... | Coloque em | Leia |
|---|---|---|
| Regra de negócio, validação de negócio, transição de estado | `domain`, na entidade de domínio. Nunca no use case, no resource nem na `*Entity` | [dominio](references/dominio.md) |
| Consultar algo para decidir uma escrita (existe? está ativo?) | método novo na interface de repositório do `domain`, implementado em `persistence`. Nunca um repositório do query | [use-cases](references/use-cases.md) |
| Validar o formato da entrada (obrigatório, tamanho, faixa) | `XRequest` (record) no entrypoint, com Bean Validation | [canais](references/canais.md) |
| Gravar algo | use case em `application`: carrega pelo repositório, chama o método de negócio, salva | [use-cases](references/use-cases.md) |
| Tela ou listagem nova | record + `@Query` HQL em `query/persistence`, use case em `query/application`, resource | [persistência](references/persistencia-jakarta-data.md) |
| Repositório, entidade JPA, mapper | `persistence` | [persistência](references/persistencia-jakarta-data.md) |
| Chamar sistema de outra área | `*Gateway` em `gateway/<sistema>`. Só **use cases** chamam gateways | [gateway](references/gateway.md) |
| Endpoint REST ou de canal | `entrypoint/rest` ou `entrypoint/<canal>` | [canais](references/canais.md) |
| Formatar para uma tela (moeda, máscara, rótulo) | Assembler do canal, usando `Padrao<Canal>` | [canais](references/canais.md) |
| Número com significado de negócio (total pago, saldo) | uma consulta em `query/persistence`, não o Assembler | [canais](references/canais.md) |
| Consumidor de fila ou tópico | `entrypoint/mensageria` (só chama caso de uso) | |
| Job agendado ou processamento de arquivo | `entrypoint/batch` (só chama caso de uso) | |
| Conversão entre modelo externo e interno (ACL/BFF) | `translator` | |
| Mudar o banco | nova migration + campo na `*Entity` + mapper (escrita) e/ou HQL (leitura) | [migrations](references/migrations.md) |
| Criar ou mudar regra ArchUnit | só com pedido do dono | [archunit](references/archunit.md) |
| Algo "que vários módulos usam" | **Não crie `shared`.** Pare e pergunte ao dono | |

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
- `entrypoint` nunca acessa `persistence` nem `gateway` direto.
- `domain` não depende de `entrypoint`, `application`, `persistence`, `gateway` nem de framework.
- `@Transactional` só em `application`.
- Chamadas entre módulos só pela `application` do outro módulo. HQL só usa `*Entity` do próprio módulo.

## Quando parar e pedir ao dono

- Criar componente, módulo ou tipo novo.
- Extrair ACL ou BFF para componente separado.
- Introduzir CQRS em um módulo.
- Alterar, desligar ou afrouxar regra ArchUnit.
- Editar uma migration existente, ou renomear/remover campo de um `*Response` de canal.
- Processamento batch pesado (alto volume, janela noturna, escala própria).

## Justificativa (para quem pergunta "por quê?")

- Dividir o topo por **domínio** mantém a mudança de negócio dentro de um módulo; `shared` vira gaveta de bagunça (Richards, particionamento por domínio; *Hard Parts*, reuso).
- **CQRS** é decisão por módulo, não esqueleto de projeto (Fowler, bliki CQRS).
- A estrutura acompanha a **complexidade**: Domain Model só onde há regra rica (Fowler, PoEAA).
- Para equipe inexperiente, limites explícitos ajudam: o time escolhe o tipo, não inventa a arquitetura (Richards, "elastic leash").
