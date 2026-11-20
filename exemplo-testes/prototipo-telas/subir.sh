#!/usr/bin/env bash
# Sobe a API em modo dev e o protótipo de telas. Ctrl+C derruba os dois.
# Requisitos: JDK 25, Docker (o Dev Services sobe o PostgreSQL) e python3.
set -euo pipefail

AQUI=$(cd "$(dirname "$0")" && pwd)
PORTA=${PORTA:-5173}

(cd "$AQUI/.." && ./mvnw -q quarkus:dev -Dquarkus.console.enabled=false) &
API=$!
trap 'kill $API $TELAS 2>/dev/null || true' EXIT INT TERM

(cd "$AQUI" && python3 -m http.server "$PORTA" --bind 127.0.0.1) &
TELAS=$!

echo "Protótipo: http://localhost:$PORTA   (a API sobe em http://localhost:8080; a primeira vez demora)"
wait
