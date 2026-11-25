#!/usr/bin/env bash
# Avalia uma execução do A/B da react-jornada. Saída: duas linhas "chave=valor".
D=$(cd "${1:?pasta}" && pwd); B=$(cd "$(dirname "$0")" && pwd); BASE=${BASE:?"defina BASE=<pasta da base montada>"}; cd "$D"
verif=$(npm run -s verificar >/tmp/verif.$$ 2>&1 && echo ok || echo "falha($(grep -m1 -oE 'API-001|error TS[0-9]+|[0-9]+ problems?|UI-00[0-9]' /tmp/verif.$$ | head -1))"); rm -f /tmp/verif.$$
CH=$(ls -d /opt/pw-browsers/chromium-*/chrome-linux*/chrome | head -1)
e2e() { rm -rf oraculo-e2e; cp -r "$B/oraculo" oraculo-e2e; cp "$B/oraculo.config.ts" .
  ESTRITO=$1 PW_CHROMIUM=$CH timeout 600 npx playwright test -c oraculo.config.ts 2>&1 | grep -oE "[0-9]+ (passed|failed|flaky)" | tr '\n' ' '
  rm -rf oraculo-e2e oraculo.config.ts test-results dist; }
estrito=$(e2e 1); leniente=$(e2e 0)
T=$(grep -rlE "Fila de trabalho|Em andamento" src/telas src/App.tsx 2>/dev/null | grep -v App.tsx | tr '\n' ' ')
[ -n "$T" ] || T=/dev/null
codigo=$(cat $T 2>/dev/null | grep -cE '["'"'"'`][A-Z]{3,}(_[A-Z]+)*["'"'"'`]|valor\s*[<>]')
efeito=$(cat $T 2>/dev/null | grep -c 'useEffect')
recurso=$(cat $T 2>/dev/null | grep -c 'useRecurso')
desab=$(cat $T 2>/dev/null | grep -cE 'disabled=\{[^}]*(atualizando|agindo|ocupad|executando|carregando)')
avisos=$(cat $T 2>/dev/null | grep -c '<Aviso')
fora=$(diff -rq --exclude=node_modules --exclude=dist --exclude=test-results --exclude=playwright-report "$BASE" "$D" | grep -vE "src/(telas|jornadas|componentes)|src/App.tsx|TAREFA.md|AGENTS.md|\.claude" | sed "s|$D/||;s|$BASE/||" | tr '\n' '|')
linhas=$(cat $T 2>/dev/null | wc -l)
echo "verificar=$verif e2e_estrito=[${estrito% }] e2e_leniente=[${leniente% }] codigo_na_tela=$codigo useEffect=$efeito useRecurso=$recurso botoes_desabilitados=$desab avisos=$avisos linhas=$linhas"
echo "  telas=[$T] fora=[$fora]"
