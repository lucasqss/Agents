---
name: adequar-ao-padrao
description: Traz um repositório Java/Quarkus legado para o padrão da skill arquiteto-padrao, de forma incremental, liberando regras ArchUnit camada por camada. Use para migrar ou refatorar projeto existente, separar entidade que é JPA e domínio ao mesmo tempo, quebrar service god class, trocar Panache por Jakarta Data, reorganizar pastas em camadas e agregados, ou resolver violações de arquitetura em legado.
---

# Adequar ao padrão

Versão: 2026-11-08. Este documento é a referência da equipe e dos agentes de IA para migrar projetos antigos.

Esta skill ensina o **caminho**. O **destino** está na skill `arquiteto-padrao`: leia a seção "Onde cada coisa mora" dela antes de começar e consulte-a para saber como cada peça deve ficar.

## Como funciona, em resumo

1. Mostra-se o diagnóstico do projeto e espera-se a aprovação do dono.
2. Todas as regras de arquitetura (ArchUnit) são desligadas com `@ArchIgnore`. O build fica verde.
3. As regras são religadas uma camada por vez. O build quebra e mostra exatamente o que corrigir.
4. Com a camada corrigida e o build verde, faz-se um commit. Depois, a próxima camada.
5. Termina quando não sobra nenhum `@ArchIgnore`.

## Regras fixas

1. **Mostre o [diagnóstico](references/diagnostico.md) e espere aprovação antes de alterar qualquer arquivo.**
2. **Siga o [método do `@ArchIgnore`](references/metodo-archignore.md):** uma camada por vez, na ordem definida, um commit por camada.
3. **Refatoração não muda comportamento nem banco.** Nenhuma migration, nenhuma coluna renomeada, nenhum contrato de API alterado. Comportamento que mudar sem querer deve ser relatado.
4. **Nunca altere uma regra ArchUnit para passar.** `@ArchIgnore` só sai; nunca entra depois do baseline.
5. Se uma correção exigir decisão que não está nas receitas, **pare e pergunte ao dono.**

## Qual receita usar

| Sintoma no código | Receita |
|---|---|
| Pastas sem agregado, ou `command/query/shared` no topo | [layout-para-camadas](references/layout-para-camadas.md) |
| Classe com `@Entity` e regra de negócio | [entidade-jpa-e-dominio](references/entidade-jpa-e-dominio.md) |
| `extends PanacheEntity`, `PanacheRepository`, `Entidade.find(...)` | [panache-para-jakarta-data](references/panache-para-jakarta-data.md) |
| Service ou `*UseCase` com vários métodos públicos | [service-god-class](references/service-god-class.md) |
| Resource que recebe ou devolve entidade | [entidade-na-api](references/entidade-na-api.md) |
| `@RegisterRestClient` usado direto por qualquer classe | [client-solto-para-gateway](references/client-solto-para-gateway.md) |
| Lombok, MapStruct, SQL nativo, herança JPA | [remover-proibidos](references/remover-proibidos.md) |

## Ao terminar cada camada, relate

- as regras liberadas e as violações corrigidas;
- os arquivos criados, alterados e removidos, por camada;
- qualquer comportamento que mudou sem querer e como ficou explícito.
