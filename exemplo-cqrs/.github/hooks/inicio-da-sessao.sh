#!/usr/bin/env bash
# sessionStart: guarda como o projeto estava e entrega contexto ao agente.
. "$(dirname "${BASH_SOURCE[0]}")/comum.sh"

digital > "$ESTADO/inicio"
alterados_do_head | protegidos > "$ESTADO/protegidos-ja-alterados"
rm -f "$ESTADO/verificado"

pendentes=$(git status --porcelain -- . 2>/dev/null | wc -l | tr -d ' ')
contexto=$(
    echo "Branch: $(git branch --show-current 2>/dev/null). Arquivos com mudança não commitada: $pendentes."
    echo "Siga a seção 'Como trabalhar' do AGENTS.md conforme o tipo da tarefa (código novo, avaliação de legado ou bug)."
    echo "Ao final de cada turno em que você alterar código, o harness roda: $VERIFICACAO. Se falhar, você recebe a falha e deve corrigir a causa."
    echo "Arquivos protegidos (não altere nem apague; se parecer necessário, pare e explique):"
    printf '%s\n' "$PROTEGIDOS" | sed '/^[[:space:]]*$/d; s/^/  /'
    contexto_extra 2>/dev/null
)
printf '{"additionalContext":"%s"}\n' "$(printf '%s\n' "$contexto" | json)"
