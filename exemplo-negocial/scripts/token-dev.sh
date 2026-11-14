#!/usr/bin/env bash
# Gera um token JWT de DESENVOLVIMENTO (RS256) para chamar a API em `quarkus:dev`. Ver ADR 003.
# Uso: scripts/token-dev.sh <usuario> <papeis, separados por vírgula> [clienteId] [emissor]
#   scripts/token-dev.sh ana backoffice
#   scripts/token-dev.sh bruno aprovador
#   scripts/token-dev.sh carla backoffice,aprovador
#   scripts/token-dev.sh maria cliente 12        # clienteId = id do cadastro do cliente
#   curl -H "Authorization: Bearer $(scripts/token-dev.sh ana backoffice)" localhost:8080/processos
# Assina com docs/dev/privada-desenvolvimento.pem, par da chave pública configurada em %dev. Não vale em produção:
# lá a chave pública é a do provedor de identidade (MP_JWT_VERIFY_PUBLICKEY_LOCATION).
set -euo pipefail

if [ $# -lt 2 ]; then
    sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//' >&2
    exit 1
fi

usuario=$1
papeis=$2
cliente_id=${3:-}
emissor=${4:-https://exemplo-negocial}
raiz=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
chave="$raiz/docs/dev/privada-desenvolvimento.pem"

base64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

grupos=$(printf '%s' "$papeis" | awk -F, '{ for (i = 1; i <= NF; i++) printf "%s\"%s\"", (i > 1 ? "," : ""), $i }')
agora=$(date +%s)
expira=$((agora + 8 * 3600))
extra=""
[ -n "$cliente_id" ] && extra=",\"clienteId\":$cliente_id"

cabecalho=$(printf '{"alg":"RS256","typ":"JWT"}' | base64url)
corpo=$(printf '{"iss":"%s","sub":"%s","upn":"%s","groups":[%s]%s,"iat":%s,"exp":%s}' \
    "$emissor" "$usuario" "$usuario" "$grupos" "$extra" "$agora" "$expira" | base64url)
assinatura=$(printf '%s.%s' "$cabecalho" "$corpo" | openssl dgst -sha256 -sign "$chave" | base64url)

printf '%s.%s.%s\n' "$cabecalho" "$corpo" "$assinatura"
