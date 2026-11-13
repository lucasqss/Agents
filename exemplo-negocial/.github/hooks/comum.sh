# Funções comuns aos scripts do harness. Não é executado diretamente.
# Dependências: bash, git, sed, awk, grep, tr. Sem jq, sem python.

set -u

ENTRADA=$(cat)                                   # payload JSON que o Copilot CLI entrega no stdin
AQUI=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
RAIZ=$(cd "$AQUI/../.." && pwd)                  # raiz do projeto: a pasta que contém .github
cd "$RAIZ" || exit 0

# Para desligar o harness em uma sessão: HARNESS_DESLIGADO=1 copilot
[ "${HARNESS_DESLIGADO:-}" = "1" ] && exit 0
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

# shellcheck source=harness.conf
. "$AQUI/harness.conf"
VERIFICACAO="${HARNESS_VERIFICACAO:-$VERIFICACAO}"

ESTADO="$(git rev-parse --absolute-git-dir)/harness"
mkdir -p "$ESTADO"

# Texto -> conteúdo de string JSON (sem as aspas externas).
json() {
    tr -d '\000-\010\013-\037' | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' -e 's/\t/\\t/g' | awk '{printf "%s\\n", $0}'
}

# Lê um caminho por linha e devolve os que casam com algum regex de PROTEGIDOS.
protegidos() {
    local padroes
    padroes=$(printf '%s\n' "$PROTEGIDOS" | sed '/^[[:space:]]*$/d')
    [ -z "$padroes" ] && return 0
    grep -E -f <(printf '%s\n' "$padroes") || true
}

# Arquivos que existem no HEAD e estão alterados, apagados ou renomeados na árvore de trabalho.
alterados_do_head() {
    git diff --name-only --diff-filter=MDR HEAD -- . 2>/dev/null | sed "s|^$(git rev-parse --show-prefix)||"
}

# Impressão digital do código: muda sempre que qualquer arquivo de CODIGO muda.
digital() {
    {
        git diff HEAD -- . 2>/dev/null
        git ls-files --others --exclude-standard -- . 2>/dev/null | while IFS= read -r f; do
            printf '%s ' "$f"; git hash-object -- "$f" 2>/dev/null
        done
    } | filtro_codigo | git hash-object --stdin
}

# Mantém do diff só os blocos de arquivos de CODIGO (e as linhas "caminho hash" dos não rastreados).
filtro_codigo() {
    awk -v re="$CODIGO" -v prefixo="$(git rev-parse --show-prefix)" '
        /^diff --git / { f = $3; sub(/^a\//, "", f); sub("^" prefixo, "", f); manter = (f ~ re) ; if (manter) print; next }
        /^[^ ]+ [0-9a-f]+$/ && !/^(index|diff) / { f = $1; sub("^" prefixo, "", f); if (f ~ re) print; next }
        manter { print }
    '
}
