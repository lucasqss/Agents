#!/usr/bin/env bash
# agentStop: o agente só termina o turno com os arquivos protegidos intactos e a verificação passando.
# Não faz nada quando o agente não alterou código (ex.: avaliação de legado, perguntas).
. "$(dirname "${BASH_SOURCE[0]}")/comum.sh"

bloquear() {
    printf '{"decision":"block","reason":"%s"}\n' "$(printf '%s\n' "$1" | json)"
    exit 0
}

# 1. Arquivos protegidos alterados nesta sessão, por qualquer ferramenta.
touch "$ESTADO/protegidos-ja-alterados"      # o que o usuário já tinha alterado antes da sessão não conta
violados=$(alterados_do_head | protegidos | grep -v -x -F -f "$ESTADO/protegidos-ja-alterados" || true)
if [ -n "$violados" ]; then
    bloquear "Você alterou ou apagou arquivo protegido:
$violados
Desfaça com 'git checkout HEAD -- <arquivo>' e resolva o problema no código que depende dele. Se achar que o arquivo protegido está errado, desfaça mesmo assim e explique ao usuário."
fi

# 2. Nada de código mudou desde o início da sessão ou desde a última verificação que passou.
atual=$(digital)
[ -f "$ESTADO/inicio" ] && [ "$atual" = "$(cat "$ESTADO/inicio")" ] && exit 0
[ -f "$ESTADO/verificado" ] && [ "$atual" = "$(cat "$ESTADO/verificado")" ] && exit 0

# 3. Verificação.
saida=$(timeout "${VERIFICACAO_TIMEOUT:-600}" bash -c "$VERIFICACAO" 2>&1)
codigo=$?
if [ "$codigo" -eq 0 ]; then
    echo "$atual" > "$ESTADO/verificado"
    exit 0
fi
[ "$codigo" -eq 124 ] && exit 0      # estourou o tempo: não prende o agente; o CI decide

bloquear "A verificação falhou (código $codigo): $VERIFICACAO

$(printf '%s\n' "$saida" | tail -n 80 | cut -c 1-400)

Corrija a causa no código. Não altere, apague nem desative teste ou regra para passar. Se a falha já existia antes da sua mudança ou não tem relação com ela, diga isso ao usuário em vez de contorná-la."
