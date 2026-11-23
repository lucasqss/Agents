# Front: convenções para gerar telas com agentes

Aqui ficam as convenções de front do repositório: a arquitetura, a stack e as barreiras que fazem um agente de IA, mesmo um modelo mais simples, gerar telas certas. O alvo são os **rascunhos e POCs das jornadas**: telas que chamam os endpoints JSON do backend Quarkus, sem regra de negócio no front (thin client).

## O que já existe

| O quê | Onde |
|---|---|
| Arquitetura (cinco camadas, "o front descreve, o backend decide", UI-001 a UI-009, `verificar.sh`), agnóstica de framework | [`.github/skills/ui-jornada/`](.github/skills/ui-jornada/SKILL.md), com link em `.claude/skills/` |
| Exemplo em React (uma jornada, serviço simulado, e2e) | [`../exemplo-testes/prototipo-telas/`](../exemplo-testes/prototipo-telas/), com a skill `prototipo-de-jornada` |
| Exemplo em React com três jornadas, JWT de dev e upload | [`../exemplo-negocial/prototipo-telas/`](../exemplo-negocial/prototipo-telas/) |

A skill `ui-jornada` mora aqui; `exemplo-testes/.github/skills/ui-jornada` é um link para ela.

## Como a skill foi validada

Teste A/B com agentes de nível Haiku: a mesma tarefa (tela de detalhe do processo), um teste no navegador escondido do agente e o `verificar.sh`. Com a skill, React e Angular acertaram a jornada em 4 de 4; sem ela, nenhum acertou, e metade pôs regra de negócio na tela. A comparação de stacks que levou ao React está na análise publicada à parte.
