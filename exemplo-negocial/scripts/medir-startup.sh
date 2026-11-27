#!/usr/bin/env bash
# Mede o tempo de subida e a memória (RSS) da aplicação empacotada (perfil prod), para comparar ajustes de JVM.
# Dois modos:
#   Imagem (o que vale para produção): IMAGEM=<imagem> scripts/medir-startup.sh [opções do docker run...]
#     docker build -f src/main/docker/Dockerfile.jvm -t negocial . && IMAGEM=negocial scripts/medir-startup.sh
#     IMAGEM=negocial scripts/medir-startup.sh -e GC_CONTAINER_OPTIONS=-XX:+UseG1GC   # troca uma opção da imagem
#     O container roda com --memory ${MEMORIA:-512m} e --cpus ${CPUS:-2}, como um pod pequeno.
#   Jar local (para experimentar flags rápido): scripts/medir-startup.sh [opções da JVM...]
#     ./mvnw package -DskipTests && scripts/medir-startup.sh -XX:+UseSerialGC -Xmx128m
#     A JVM roda de dentro de target/quarkus-app ("-jar quarkus-run.jar"); sem limite de memória, a JVM dimensiona o heap
#     pela máquina toda, então os números de RSS só comparam entre si.
# Sobe um PostgreSQL descartável (Docker), sobe a aplicação, faz 200 chamadas autenticadas (token de scripts/token-dev.sh)
# e mostra o "started in" do Quarkus, o tempo até a primeira resposta e o RSS após subir e após as chamadas.
# Variáveis: RODADAS (padrão 3), PORTA (padrão 18080).
set -euo pipefail

raiz=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
quarkus_app="$raiz/target/quarkus-app"
imagem=${IMAGEM:-}
rodadas=${RODADAS:-3}
porta=${PORTA:-18080}
rede=medir-startup
banco=medir-startup-postgres
app=medir-startup-app
log=$(mktemp)

[ -n "$imagem" ] || [ -f "$quarkus_app/quarkus-run.jar" ] || {
    echo "Falta $quarkus_app/quarkus-run.jar: rode ./mvnw package -DskipTests" >&2; exit 1; }

parar_app() {
    if [ -n "$imagem" ]; then docker rm -f "$app" >/dev/null 2>&1 || true
    elif [ -n "${pid:-}" ]; then kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true; fi
    pid=
}
encerrar() {
    parar_app
    docker rm -f "$banco" >/dev/null 2>&1 || true
    docker network rm "$rede" >/dev/null 2>&1 || true
    rm -f "$log"
}
trap encerrar EXIT

docker rm -f "$banco" "$app" >/dev/null 2>&1 || true
docker network create "$rede" >/dev/null 2>&1 || true
docker run -d --name "$banco" --network "$rede" -p 15432:5432 \
    -e POSTGRES_DB=app -e POSTGRES_USER=app -e POSTGRES_PASSWORD=app postgres:18 >/dev/null
until docker exec "$banco" pg_isready -U app -d app >/dev/null 2>&1; do sleep 0.5; done
sleep 1

token=$("$raiz/scripts/token-dev.sh" ana backoffice)
chamar() { curl -sf -o /dev/null -H "Authorization: Bearer $token" "localhost:$porta/processos"; }

subir() {
    if [ -n "$imagem" ]; then
        docker run -d --name "$app" --network "$rede" -p "$porta:8080" \
            --memory "${MEMORIA:-512m}" --cpus "${CPUS:-2}" \
            -e DB_URL="jdbc:postgresql://$banco:5432/app" -e DB_USER=app -e DB_PASSWORD=app \
            -e MP_JWT_VERIFY_PUBLICKEY_LOCATION=/chaves/publica-desenvolvimento.pem \
            -e PAGAMENTO_ROTINA_INTERVALO=off \
            -v "$raiz/src/main/resources/jwt:/chaves:ro" "$@" "$imagem" >/dev/null
    else
        (cd "$quarkus_app" && exec env \
            DB_URL="jdbc:postgresql://localhost:15432/app" DB_USER=app DB_PASSWORD=app \
            MP_JWT_VERIFY_PUBLICKEY_LOCATION="$raiz/src/main/resources/jwt/publica-desenvolvimento.pem" \
            PAGAMENTO_ROTINA_INTERVALO=off \
            java "$@" -Dquarkus.http.port="$porta" -jar quarkus-run.jar) >"$log" 2>&1 &
        pid=$!
    fi
}
viva() { if [ -n "$imagem" ]; then [ "$(docker inspect -f '{{.State.Running}}' "$app" 2>/dev/null)" = true ]; else kill -0 "$pid" 2>/dev/null; fi; }
saida() { if [ -n "$imagem" ]; then docker logs "$app" 2>&1; else cat "$log"; fi; }
rss_mb() {
    if [ -n "$imagem" ]; then docker exec "$app" awk '/VmRSS/ { print int($2 / 1024) }' /proc/1/status
    else echo $(( $(ps -o rss= -p "$pid") / 1024 )); fi
}

echo "${imagem:+Imagem $imagem (--memory ${MEMORIA:-512m} --cpus ${CPUS:-2}), }opções: ${*:-(nenhuma)}"
for i in $(seq 1 "$rodadas"); do
    inicio=$(date +%s%N)
    subir "$@"
    until chamar; do
        viva || { saida >&2; exit 1; }
        sleep 0.05
    done
    primeira=$(( ($(date +%s%N) - inicio) / 1000000 ))
    rss_subida=$(rss_mb)
    for _ in $(seq 1 200); do chamar; done
    rss_carga=$(rss_mb)
    started=$(saida | grep -o 'started in [0-9.]*s' | head -1)
    saida | grep -E '\[(aot|cds)' | grep -E 'warning|error' | head -3 || true
    printf 'rodada %s: %s | primeira resposta %s ms | RSS %s MB após subir, %s MB após 200 chamadas\n' \
        "$i" "$started" "$primeira" "$rss_subida" "$rss_carga"
    parar_app
done
