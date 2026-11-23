# Agents

Modelo para criar e manter serviços **Java 25 + Quarkus 3.33** com agentes de IA (Claude Code, Copilot CLI) sem perder a arquitetura. O repositório traz um serviço de exemplo completo e, junto dele, as instruções, skills, hooks e barreiras de build que fazem o agente trabalhar dentro do padrão.

## O que tem aqui

| Pasta | O que é |
|---|---|
| [`exemplo-negocial/`](exemplo-negocial/) | Serviço de referência (processos de pagamento de consórcio): domínio puro, camadas verificadas por ArchUnit, testes de aceitação e as barreiras de qualidade do build. [README](exemplo-negocial/README.md) |
| [`exemplo-negocial/AGENTS.md`](exemplo-negocial/AGENTS.md) | Regras sempre ativas para qualquer agente: como trabalhar, o que é proibido, o que rodar antes de terminar. |
| [`exemplo-negocial/.github/skills/`](exemplo-negocial/.github/skills/) | As skills: especificar o caso de uso, o padrão de arquitetura (com as regras ArchUnit e o teste de contrato para copiar), adequar legado e testes por nível (unidade, componente e aceitação com especificações em português executadas pelo Cucumber). `.claude/skills/` aponta para elas. |
| [`exemplo-negocial/.github/hooks/`](exemplo-negocial/.github/hooks/) | Hooks do agente: arquivo protegido não é alterado e o turno só termina com a verificação rápida passando. |
| [`exemplo-testes/`](exemplo-testes/) | Modelo de automação de testes, menor: só o processo de pagamento (máquina de estados por tipo). Unidade, componente (`@QuarkusTest`), integração (`@QuarkusIntegrationTest`) e aceitação com **especificações em português** (Cucumber) que o negócio valida, sobre massa de **IDs fixos**, reprodutível em todo build, mais as mesmas barreiras. [README](exemplo-testes/README.md) |
| [`frontend/`](frontend/) | Convenções de front (React + Vite + TypeScript + Tailwind) para gerar com agentes as telas que desenham as jornadas e acionam a API (thin client): a skill de arquitetura `ui-jornada`, com as regras e o script de verificação. [README](frontend/README.md) |
| [`testes-gauge/`](testes-gauge/) | Smoke tests do exemplo com Gauge (especificações em Markdown, passos em Java), rodando contra a aplicação empacotada. Sobe tudo com `docker compose run --rm --build smoke`. [README](testes-gauge/README.md) |
| [`ignore/`](ignore/) | Modelo de `.claudeignore` que limita o agente ao `src/`. |

## Por onde começar

- **Entender o padrão:** leia o [README do exemplo](exemplo-negocial/README.md) (arquitetura, guardrails, barreiras de qualidade) e rode `./mvnw verify` dentro de `exemplo-negocial/` (JDK 25 e Docker).
- **Projeto novo:** copie `exemplo-negocial/.github/` e `exemplo-negocial/AGENTS.md` para a raiz do seu repositório e peça ao agente o primeiro caso de uso. A skill `especificar-caso-de-uso` escreve o requisito com você; a `arquiteto-padrao` monta o `pom.xml` (com as barreiras), as pastas, as regras ArchUnit e o código.
- **Projeto legado:** com as mesmas pastas copiadas, use a skill `adequar-ao-padrao`. Ela mostra o diagnóstico, congela o contrato REST antes de mexer e libera as regras camada por camada.

## O que o build garante

Cada mudança, de pessoa ou de agente, passa por estas barreiras, da mais barata para a mais cara: Enforcer (dependências proibidas), ErrorProne + NullAway (nulos e erros semânticos na compilação), ArchUnit (camadas), testes de propriedade e de contrato REST, `@QuarkusTest` com PostgreSQL real e, a cada pull request, teste de mutação (`-Pauditoria`). Detalhes na seção [Barreiras de qualidade no build](exemplo-negocial/README.md#barreiras-de-qualidade-no-build).
