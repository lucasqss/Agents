#!/usr/bin/env bash
# Cria as execuções de um A/B a partir da base: <raiz>/temp-sem-skill/rN e <raiz>/temp-com-skill/rN, N de 1 a <n>.
# A única diferença entre os grupos: as skills (cópias) e a linha que manda usá-las, no AGENTS.md. Cada execução tem a
# sua porta de e2e (PW_PORTA), para os agentes rodarem em paralelo sem brigar pela mesma porta.
#   montar-execucoes.sh <base> <raiz> <n> [tarefa]
set -euo pipefail
BASE=${1:?"uso: montar-execucoes.sh <base> <raiz> <n> [tarefa]"}; RAIZ=${2:?}; N=${3:?}; TAREFA=${4:-fila}
AQUI=$(cd "$(dirname "$0")" && pwd)
porta=4300
for grupo in sem com; do
  for r in $(seq 1 "$N"); do
    d="$RAIZ/temp-$grupo-skill/r$r"; porta=$((porta + 1))
    mkdir -p "$d"
    (cd "$BASE" && tar cf - --exclude=node_modules --exclude=dist .) | (cd "$d" && tar xf -)
    cp -al "$BASE/node_modules" "$d/node_modules"
    sed "s|npm run test:e2e\`|npm run test:e2e\` (use \`PW_PORTA=$porta\`)|" "$AQUI/$TAREFA/AGENTS.$grupo.md" > "$d/AGENTS.md"
    if [ "$grupo" = com ]; then
      mkdir -p "$d/.claude/skills"
      cp -rL "$AQUI/../.github/skills/ui-jornada" "$AQUI/../.github/skills/react-jornada" "$d/.claude/skills/"
    fi
  done
done
echo "Execuções em $RAIZ/temp-{sem,com}-skill/r1..r$N. Apague as duas pastas ao terminar."
