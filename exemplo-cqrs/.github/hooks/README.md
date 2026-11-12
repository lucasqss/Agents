# Hooks do agente

Fazem o Copilot CLI trabalhar com as mesmas garantias em qualquer tarefa (código novo, avaliação de legado, correção de bug), sem depender de o modelo lembrar das regras.

| Evento | Script | O que faz |
|---|---|---|
| `sessionStart` | `inicio-da-sessao.sh` | Guarda como o projeto estava e entrega ao agente a branch, os arquivos protegidos e a verificação que vai rodar. |
| `preToolUse` | `proteger-arquivos.sh` | Nega a edição de um arquivo protegido que já existe no git. |
| `agentStop` | `verificar-antes-de-terminar.sh` | Se o agente alterou código, roda a verificação. Se ela falha, ou se um arquivo protegido foi alterado por qualquer ferramenta, o turno não termina e o agente recebe o motivo. |

Quando o agente não altera código (avaliação, perguntas, documentação), o `agentStop` não roda nada.

## Para usar em outro projeto

Copie a pasta para `.github/hooks/` na raiz do repositório e edite só o `harness.conf`:

- `VERIFICACAO`: o comando que prova que o projeto continua de pé. Precisa levar segundos e não depender de Docker.
- `CODIGO`: o que conta como código (regex sobre o caminho).
- `PROTEGIDOS`: o que o agente não pode alterar nem apagar depois de existir no git. Criar arquivo novo é permitido.
- `contexto_extra`: texto opcional entregue no início da sessão.

Os scripts precisam de `bash` e `git`. Não usam `jq` nem Python.

## Limites

- Depois de 8 bloqueios seguidos, o Copilot CLI encerra o turno mesmo com a verificação falhando. O CI continua sendo a garantia final.
- Se a verificação passar de `VERIFICACAO_TIMEOUT`, o agente é liberado.
- Para desligar em uma sessão: `HARNESS_DESLIGADO=1 copilot`.
- O estado fica em `.git/harness/` e é um por repositório: duas sessões ao mesmo tempo no mesmo clone compartilham o estado.
