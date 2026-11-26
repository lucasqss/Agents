# Agents

Modelo para criar e manter serviços **Java 25 + Quarkus 3.33** com agentes de IA (Claude Code, Copilot CLI) sem perder a arquitetura. O repositório traz um serviço de exemplo completo e, junto dele, as instruções, skills, hooks e barreiras de build que fazem o agente trabalhar dentro do padrão.

## O que tem aqui

| Pasta | O que é |
|---|---|
| [`exemplo-negocial/`](exemplo-negocial/) | Serviço de referência (processos de pagamento de consórcio): domínio puro, camadas verificadas por ArchUnit, testes de aceitação e as barreiras de qualidade do build. [README](exemplo-negocial/README.md) |
| [`.claude/skills/`](.claude/skills/) | **Todas as skills do repositório, num lugar só.** Backend: `especificar-caso-de-uso`, `arquiteto-padrao` (com as regras ArchUnit, o teste de contrato e o contrato OpenAPI para o front), `adequar-ao-padrao`. Testes por nível: `teste-unidade`, `teste-componente`, `teste-aceitacao` (especificações em português executadas pelo Cucumber). Front: `ui-jornada` (arquitetura), `react-jornada` (receitas), `prototipo-de-jornada` (o ciclo com o usuário). **Abra a sessão do agente aqui, na raiz**: é daqui que elas carregam. |
| [`exemplo-negocial/AGENTS.md`](exemplo-negocial/AGENTS.md) | Regras sempre ativas para qualquer agente: como trabalhar, o que é proibido, o que rodar antes de terminar. |
| [`exemplo-negocial/.github/hooks/`](exemplo-negocial/.github/hooks/) | Hooks do agente: arquivo protegido não é alterado e o turno só termina com a verificação rápida passando. |
| [`exemplo-testes/`](exemplo-testes/) | Modelo de automação de testes, menor: só o processo de pagamento (máquina de estados por tipo). Unidade, componente (`@QuarkusTest`), integração (`@QuarkusIntegrationTest`) e aceitação com **especificações em português** (Cucumber) que o negócio valida, sobre massa de **IDs fixos**, reprodutível em todo build, mais as mesmas barreiras. [README](exemplo-testes/README.md) |
| [`frontend/`](frontend/) | Convenções de front para gerar com agentes as telas que desenham as jornadas e acionam a API (thin client): a POC de referência (React + shadcn/ui + cliente gerado do OpenAPI), o gerador de front novo, os hooks e a avaliação das skills. [README](frontend/README.md) |
| [`testes-gauge/`](testes-gauge/) | Smoke tests do exemplo com Gauge (especificações em Markdown, passos em Java), rodando contra a aplicação empacotada. Sobe tudo com `docker compose run --rm --build smoke`. [README](testes-gauge/README.md) |
| [`ignore/`](ignore/) | Modelo de `.claudeignore` que limita o agente ao `src/`. |

## Por onde começar

- **Entender o padrão:** leia o [README do exemplo](exemplo-negocial/README.md) (arquitetura, guardrails, barreiras de qualidade) e rode `./mvnw verify` dentro de `exemplo-negocial/` (JDK 25 e Docker).
- **Projeto novo:** copie para a raiz do seu repositório as skills que vai usar (de `.claude/skills/` para o `.claude/skills/` dele), os hooks (`exemplo-negocial/.github/hooks/`) e o `exemplo-negocial/AGENTS.md`, e peça ao agente o primeiro caso de uso. A skill `especificar-caso-de-uso` escreve o requisito com você; a `arquiteto-padrao` monta o `pom.xml` (com as barreiras), as pastas, as regras ArchUnit e o código.
- **Projeto legado:** com as mesmas skills e hooks copiados, use a skill `adequar-ao-padrao`. Ela mostra o diagnóstico, congela o contrato REST antes de mexer e libera as regras camada por camada.

## O que o build garante

Cada mudança, de pessoa ou de agente, passa por estas barreiras, da mais barata para a mais cara: Enforcer (dependências proibidas), ErrorProne + NullAway (nulos e erros semânticos na compilação), ArchUnit (camadas), testes de propriedade e de contrato REST, `@QuarkusTest` com PostgreSQL real e, a cada pull request, teste de mutação (`-Pauditoria`). Detalhes na seção [Barreiras de qualidade no build](exemplo-negocial/README.md#barreiras-de-qualidade-no-build).
