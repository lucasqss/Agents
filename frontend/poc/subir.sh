#!/usr/bin/env bash
# Sobe a POC.
#   ./subir.sh           o serviço exemplo-testes em modo dev (Quarkus) + as telas contra ele. Ctrl+C derruba os dois.
#   ./subir.sh falso     só as telas, com o serviço simulado no navegador (sem Java, sem Docker).
# Requisitos: Node 22+; no modo normal, também JDK 25 e Docker (o Dev Services sobe o PostgreSQL).
set -euo pipefail

AQUI=$(cd "$(dirname "$0")" && pwd)
cd "$AQUI"
[ -d node_modules ] || npm ci

if [ "${1:-}" = "falso" ]; then
  exec npm run dev:falso
fi

(cd "$AQUI/../../exemplo-testes" && ./mvnw -q quarkus:dev -Dquarkus.console.enabled=false) &
API=$!
trap 'kill $API 2>/dev/null || true' EXIT INT TERM

echo "Telas: http://localhost:5173   (a API sobe em http://localhost:8080; a primeira vez demora)"
npm run dev
