#!/usr/bin/env bash
# Confere se src/api/esquema.d.ts está igual ao que o contrato atual gera. Se o backend mudou o OpenAPI e ninguém
# rodou `npm run gerar:api`, o build para aqui, antes de a tela usar um tipo velho.
set -euo pipefail
cd "$(dirname "$0")/.."
TEMP=$(mktemp -d)
trap 'rm -rf "$TEMP"' EXIT
npx openapi-typescript ../../exemplo-testes/docs/api/openapi-base.json --properties-required-by-default -o "$TEMP/esquema.d.ts" >/dev/null 2>&1
if ! diff -q "$TEMP/esquema.d.ts" src/api/esquema.d.ts >/dev/null; then
  echo "API-001: src/api/esquema.d.ts está desatualizado em relação ao contrato (docs/api/openapi-base.json)."
  echo "  Corrija: rode 'npm run gerar:api' e ajuste o que o typecheck apontar. Nunca edite esquema.d.ts à mão."
  exit 1
fi
echo "Contrato da API ok: esquema.d.ts confere com o OpenAPI."
