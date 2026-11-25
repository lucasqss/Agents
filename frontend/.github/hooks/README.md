# Hooks do agente (front)

Os mesmos scripts do harness do backend (`exemplo-testes/.github/hooks`), com a configuração do front em `harness.conf` e a resposta também no formato do **Claude Code** (além do Copilot CLI).

| Evento | Script | O que faz |
|---|---|---|
| início da sessão | `inicio-da-sessao.sh` | Guarda como o front estava e diz ao agente o que vai ser verificado e o que é protegido. |
| antes de editar | `proteger-arquivos.sh` | Nega editar arquivo protegido (as barreiras: `verificar.sh`, lint, tsconfig, apoio dos e2e, os hooks). |
| fim do turno | `verificar-antes-de-terminar.sh` | Se o agente mexeu em código do front, roda `npm run verificar` na POC. Falhou, ou mexeu em protegido: o turno não termina, e o agente recebe o motivo. |

- **Copilot CLI:** `harness.json` (formato do Copilot).
- **Claude Code:** `../../.claude/settings.json`. Vale quando a sessão é aberta em `frontend/`, ou num front criado pelo `novo-front.sh`.
- **Limite de bloqueios:** o Claude Code é liberado depois de 5 bloqueios seguidos, e o Copilot CLI depois de 8. O CI (`.github/workflows/frontend.yml`, na raiz do repositório) é a garantia final.
- **Desligar numa sessão:** `HARNESS_DESLIGADO=1`.
