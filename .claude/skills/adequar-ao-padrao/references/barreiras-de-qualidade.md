# Barreiras de qualidade em legado

As barreiras do build ([pom obrigatório](../../arquiteto-padrao/references/pom-obrigatorio.md), seção "Barreiras de qualidade") entram no legado na mesma lógica do `@ArchIgnore`: cada uma liga quando o build consegue ficar verde com ela, e nada é afrouxado depois.

| Quando | Barreira | Como ligar |
|---|---|---|
| **Antes da primeira refatoração** | Contrato REST (openapi-diff + `ContratoRestTest`) | Gere `docs/api/openapi-base.json` a partir do código **ainda não mexido** e copie o `ContratoRestTest`. A partir daí o build prova a regra fixa 3: nenhuma refatoração muda o contrato da API. |
| Junto com o baseline | Enforcer, sem `bannedDependencies` | `requireJavaVersion`, `requireMavenVersion`, `banDuplicatePomDependencyVersions` e `dependencyConvergence`. Se a convergência acusar conflito, declare a versão que a árvore já traz. |
| Depois de [remover-proibidos](remover-proibidos.md) | Enforcer, com `bannedDependencies` | O Lombok, o MapStruct e o Panache já saíram; a regra impede que voltem. |
| Junto com o baseline | ErrorProne, sem NullAway e sem `-Werror` | Ligue o plugin com `-Xep:NullAway:OFF`. Corrija os **erros** (eles são bugs de verdade: comparação errada, valor de retorno ignorado). Os avisos ficam listados para depois. |
| A cada camada liberada no ArchUnit | NullAway, pacote por pacote | Acrescente o pacote da camada em `nullaway.pacotes` (vírgula separa), na mesma ordem do [método](metodo-archignore.md): domínio primeiro. Declare com `@Nullable` o que pode ser nulo, use `Objects.requireNonNull` onde uma invariante garante o valor, e siga as cascatas até o build ficar verde. Nunca `@SuppressWarnings("NullAway")`. |
| Quando não sobrar `@ArchIgnore` | `-Werror` | Zere os avisos do ErrorProne e ligue `-Werror` nas duas execuções do compilador. |
| Ao tocar em Value Object ou tabela de fluxo | jqwik | Escreva o `*PropriedadesTest` da regra que você está movendo para o domínio: ele mostra, com o menor exemplo, se a mudança alterou o comportamento. |
| Ao final | PITest (`-Pauditoria`) | Rode com `pitest.limite` em `0`, fixe o valor medido e relate os mutantes sobreviventes ao dono como dívida de teste. |

## O que relatar

- Na adoção do NullAway, cada `@Nullable` acrescentado é uma decisão sobre o modelo (o valor pode mesmo faltar?). Relate os que mudaram a assinatura pública de uma classe de domínio.
- Um nulo que o NullAway acusou e que **não** tinha tratamento é um bug do legado: siga a regra de correção de bug (teste que reproduz primeiro) e relate.
- Mudança na linha de base do OpenAPI durante a adequação não é permitida. Se o contrato precisa mudar, é outra tarefa, com o dono.
