#!/usr/bin/env bash
# Confere as regras da arquitetura de UI da skill ui-jornada. Sem npm: só bash, grep e python3.
# Agnóstico de framework: olha os caminhos de cada camada, não a sintaxe de um framework.
#
#   verificar.sh <pasta-do-front>
#
# Os caminhos das camadas (relativos à pasta do front, separados por espaço) vêm de variáveis, com os padrões
# da estrutura da skill (js/ ou src/ com nucleo, api, jornadas, componentes, telas):
#   UI_NUCLEO        infraestrutura: único lugar com fetch/axios/HttpClient
#   UI_API           clientes dos recursos: único lugar com endpoint (chamar(...), URL base)
#   UI_JORNADA       o desenho da jornada: único lugar com código da API (EM_ANALISE)
#   UI_COMPONENTES   componentes visuais puros
#   UI_TELAS         telas
# Exemplo para o protótipo React deste repositório (skill prototipo-de-jornada):
#   UI_NUCLEO="src/api src/apoio" UI_API="src/api src/jornada/api.ts" \
#   UI_JORNADA="src/jornada/vocabulario.ts src/jornada/servicoFalso.ts" \
#   UI_COMPONENTES="src/componentes" UI_TELAS="src/telas src/App.tsx" verificar.sh exemplo-testes/prototipo-telas
#
# Sai com 1 e lista arquivo:linha de cada violação, com o "Corrija:".
set -uo pipefail

FRONT=${1:?"uso: verificar.sh <pasta-do-front>"}
cd "$FRONT" || exit 2
RAIZ=$( [ -d src ] && echo src || echo js )
NUCLEO=${UI_NUCLEO:-$RAIZ/nucleo}
API=${UI_API:-$RAIZ/api}
JORNADA=${UI_JORNADA:-$RAIZ/jornadas}
COMPONENTES=${UI_COMPONENTES:-$RAIZ/componentes}
TELAS=${UI_TELAS:-$RAIZ/telas}

EXT=(--include=*.js --include=*.mjs --include=*.ts --include=*.tsx --include=*.jsx --include=*.vue --include=*.html)
falhas=0

# Filtra do grep as linhas de arquivos que pertencem às camadas permitidas ($@).
fora_de() {
  local padrao=""
  for c in "$@"; do padrao+="${padrao:+|}^${c//./\\.}(/|:)"; done
  grep -vE "$padrao" || true
}

regra() { # $1 = id e título, $2 = corrija, $3 = violações
  if [ -n "$3" ]; then
    falhas=$((falhas + 1))
    printf '\n%s\n  Corrija: %s\n' "$1" "$2"
    printf '%s\n' "$3" | sed 's/^/    /'
  fi
}

existentes() { for c in "$@"; do [ -e "$c" ] && echo "$c"; done; }

regra "UI-001: CHAMADA HTTP SÓ NO NÚCLEO ($NUCLEO)" \
  "use a função do cliente da API; tela e componente nunca chamam HTTP direto" \
  "$(grep -rnE "${EXT[@]}" '\b(fetch|axios|XMLHttpRequest|HttpClient)\b' "$RAIZ" | fora_de $NUCLEO)"

regra "UI-002: ENDPOINT DO BACKEND SÓ NOS CLIENTES DA API ($API)" \
  "crie ou use a função no cliente do recurso; o caminho do endpoint e a URL base só existem lá" \
  "$(grep -rnE "${EXT[@]}" '\bchamar\(|PROTOTIPO\.api|VITE_API|API_URL|apiBaseUrl' "$RAIZ" | fora_de $API $NUCLEO)"

# Alternativa de regex com os caminhos das camadas ($@), sem a raiz e sem extensão: "src/jornada/api.ts" -> "jornada/api".
alternativas() {
  local alt=""
  for c in "$@"; do c=${c#"$RAIZ"/}; c=${c%.*}; alt+="${alt:+|}${c//./\\.}"; done
  echo "$alt"
}
# Clientes da API que não são núcleo (no React deste repositório, src/api é o núcleo e src/jornada/api.ts o cliente).
clientes=(); for c in $API; do [[ " $NUCLEO " == *" $c "* ]] || clientes+=("$c"); done
proibidos=$(alternativas "${clientes[@]}" $JORNADA $TELAS)

comp=$(existentes $COMPONENTES)
regra "UI-003: COMPONENTE NÃO IMPORTA CLIENTE DA API, JORNADA NEM TELAS ($COMPONENTES)" \
  "receba os dados prontos por parâmetro (a tela busca na API e traduz pela jornada) e devolva só a visão" \
  "$([ -n "$comp" ] && grep -rnE "${EXT[@]}" "from ['\"][^'\"]*/($proibidos)(/|['\"])" $comp || true)"

api_dir=$(existentes $API)
regra "UI-004: CLIENTE DA API NÃO DEPENDE DE TELAS NEM COMPONENTES ($API)" \
  "o cliente devolve a resposta; quem decide o que mostrar é a tela" \
  "$([ -n "$api_dir" ] && grep -rnE "${EXT[@]}" "from ['\"][^'\"]*/($(alternativas $TELAS $COMPONENTES))(/|['\"])" $api_dir || true)"

visao=$(existentes $TELAS $COMPONENTES)
regra "UI-005: CÓDIGO DA API SÓ NA JORNADA ($JORNADA)" \
  "traduza pelo vocabulário/contexto da jornada; regra que compara código é do backend" \
  "$([ -n "$visao" ] && grep -rnE "${EXT[@]}" "[\"'\`][A-Z][A-Z0-9]*_[A-Z0-9_]+[\"'\`]" $visao || true)"

invalidas=""
while IFS= read -r j; do
  [ -n "$j" ] && { python3 -m json.tool "$j" >/dev/null 2>&1 || invalidas+="$j: JSON inválido"$'\n'; }
done < <(find . -path ./node_modules -prune -o -path "*jornada*/*.json" -print 2>/dev/null)
regra "UI-006: JORNADA EM JSON É VÁLIDA" "corrija a sintaxe (vírgula, aspas); a jornada é dado, não código" "${invalidas%$'\n'}"

if [ "$falhas" -gt 0 ]; then
  printf '\n%d regra(s) violada(s). Corrija o código, não a regra.\n' "$falhas"
  exit 1
fi
echo "Arquitetura de UI ok ($FRONT): UI-001 a UI-006."
