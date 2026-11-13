#!/usr/bin/env bash
# preToolUse (ferramentas de edição): nega alterar arquivo protegido que já existe no git.
# É o aviso rápido. A garantia é o agentStop, que confere o git seja qual for a ferramenta usada.
. "$(dirname "${BASH_SOURCE[0]}")/comum.sh"

alvo=$(git ls-files -- . | sed "s|^$(git rev-parse --show-prefix)||" | protegidos | while IFS= read -r f; do
    case "$ENTRADA" in *"$f"*) echo "$f"; break ;; esac
done)
[ -z "$alvo" ] && exit 0

motivo="Arquivo protegido: $alvo. Não altere nem apague. Corrija o código que depende dele; se achar que o arquivo protegido está errado, pare e explique ao usuário."
printf '{"permissionDecision":"deny","permissionDecisionReason":"%s"}\n' "$(printf '%s' "$motivo" | json | sed 's/\\n$//')"
