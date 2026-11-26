# Instruções para agentes de IA

Este repositório é um modelo para criar serviços Java + Quarkus e as telas das jornadas com agentes de IA, sem perder a arquitetura. Tem três projetos, cada um com o seu `AGENTS.md`:

| Pasta | O que é |
|---|---|
| `exemplo-negocial/` | Serviço de referência: domínio puro, camadas verificadas por ArchUnit, barreiras de build |
| `exemplo-testes/` | Modelo de automação de testes: unidade, componente, integração e aceitação em português |
| `frontend/` | Telas das jornadas (thin client): a POC de referência, o gerador de front novo, a avaliação das skills |

**As skills de todos ficam em `.claude/skills/`**, aqui na raiz:
- **backend:** `especificar-caso-de-uso`, `arquiteto-padrao`, `adequar-ao-padrao`;
- **testes:** `teste-unidade`, `teste-componente`, `teste-aceitacao`;
- **front:** `ui-jornada`, `react-jornada`, `prototipo-de-jornada`.

**Ao trabalhar num projeto, siga o `AGENTS.md` dele.** Ele diz quais skills usar, como trabalhar, o que é proibido e o que rodar antes de terminar. Os comandos (`./mvnw verify`, `npm run verificar`) rodam de dentro da pasta do projeto.
