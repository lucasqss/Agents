#!/usr/bin/env bash
# Monta a base de um A/B a partir da POC atual: uma cópia autônoma (o contrato vai junto), com a TAREFA da avaliação.
#   montar-base.sh <destino> [tarefa]     tarefa: a pasta da avaliação (padrão: fila)
set -euo pipefail
DESTINO=${1:?"uso: montar-base.sh <destino> [tarefa]"}
TAREFA=${2:-fila}
AQUI=$(cd "$(dirname "$0")" && pwd)
POC="$AQUI/../poc"
[ -e "$DESTINO" ] && { echo "$DESTINO já existe" >&2; exit 1; }
mkdir -p "$DESTINO"
(cd "$POC" && tar cf - --exclude=node_modules --exclude=dist --exclude=test-results --exclude=playwright-report --exclude=./README.md .) | (cd "$DESTINO" && tar xf -)
if [ -d "$POC/node_modules" ]; then cp -al "$POC/node_modules" "$DESTINO/node_modules"; else (cd "$DESTINO" && npm ci); fi
# O contrato do serviço vem junto (a POC o lê por caminho relativo ao repositório).
SERVICO=$(python3 -c 'import json; print(json.load(open("'"$POC"'/contrato.config.json"))["servico"])')
cp "$POC/$SERVICO" "$DESTINO/contrato/servico.json"
python3 - "$DESTINO" <<'EOF'
import json, sys
d = sys.argv[1]
c = json.load(open(f"{d}/contrato.config.json")); c["servico"] = "contrato/servico.json"; json.dump(c, open(f"{d}/contrato.config.json", "w"), indent=2)
p = json.load(open(f"{d}/package.json")); p["scripts"]["arquitetura"] = 'UI_TELAS="src/telas src/App.tsx" bash scripts/verificar-arquitetura.sh .'; json.dump(p, open(f"{d}/package.json", "w"), indent=2)
EOF
cp "$AQUI/../.github/skills/ui-jornada/scripts/verificar.sh" "$DESTINO/scripts/verificar-arquitetura.sh"
cp "$AQUI/$TAREFA/TAREFA.md" "$DESTINO/TAREFA.md"
(cd "$DESTINO" && npm run -s verificar >/dev/null) && echo "Base pronta e verde: $DESTINO"
