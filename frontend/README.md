# Front: convenções para gerar telas com agentes

Aqui ficam as convenções de front do repositório: a arquitetura, a stack e as barreiras que fazem um agente de IA, mesmo um modelo mais simples, gerar telas certas. O alvo são os **rascunhos e POCs das jornadas**: telas que chamam os endpoints JSON do backend Quarkus, sem regra de negócio no front (thin client).

## O que tem aqui

| O quê | Onde |
|---|---|
| Arquitetura, agnóstica de framework: o front descreve, o backend decide; as cinco camadas; UI-001 a UI-009; `verificar.sh` | skill [`ui-jornada`](.github/skills/ui-jornada/SKILL.md) |
| Receitas em React, por tarefa, apontando para o modelo na POC | skill [`react-jornada`](.github/skills/react-jornada/SKILL.md) |
| A aplicação de referência: React 19 + Vite + TS strict + Tailwind 4 + shadcn/ui, cliente gerado do OpenAPI, serviço simulado, ESLint (hooks e acessibilidade) e Playwright | [`poc/`](poc/README.md) |
| Regras do contrato OpenAPI que o backend publica para o front gerar o cliente | `contrato-openapi.md`, na skill `arquiteto-padrao` (`exemplo-negocial`) |

As skills ficam em `.github/skills/` (Copilot), com links em `.claude/skills/` (Claude Code). O `exemplo-testes/.github/skills/ui-jornada` é um link para a daqui.

## As barreiras da POC

`npm run verificar` para no primeiro problema, cada um com o "Corrija:" na mensagem:

1. **Contrato em dia (API-001):** os tipos gerados conferem com o OpenAPI do serviço.
2. **Typecheck:** TypeScript strict. O caminho, os parâmetros, o corpo e as respostas de cada chamada são checados contra o contrato, e o vocabulário da jornada tem de cobrir todo código que o contrato publica.
3. **Lint:** as regras de hooks do React (incluindo as do React Compiler) e de acessibilidade de JSX.
4. **Arquitetura (UI-001 a UI-006):** HTTP só no núcleo, endpoint só na api, componente puro, código da API só na jornada.

`npm run test:e2e` percorre as jornadas num navegador, em computador e celular, com a checagem de acessibilidade (axe) em cada tela. Os testes são escritos a partir da especificação, antes da tela, com seletores pelo papel acessível: no A/B com agentes, foi esse tipo de teste que pegou o que o lint não via (abas feitas com botões, texto diferente do pedido).

## Como a skill de arquitetura foi validada

Teste A/B com agentes de nível Haiku: a mesma tarefa (tela de detalhe do processo), um teste no navegador escondido do agente e o `verificar.sh`. Com a skill, React e Angular acertaram a jornada em 4 de 4; sem ela, nenhum acertou, e metade pôs regra de negócio na tela. A comparação de stacks que levou ao React está na análise publicada à parte.
