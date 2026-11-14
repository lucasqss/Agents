# Smoke tests com Gauge

Verificação rápida de que o `exemplo-negocial` empacotado sobe e atende os endpoints principais: documentação, identidade, cadastro de cliente e cota, e as consultas. As especificações são Markdown em português (`specs/`), legíveis pelo negócio; cada frase é implementada em Java (`src/test/java/br/com/claudio/smoke/`).

Não substitui os testes do próprio serviço (unidade, componente e aceitação, nas skills do exemplo): roda **contra a aplicação já em pé**, como um smoke depois do deploy.

## Rodar

Requisitos: Docker com Compose e acesso à internet (imagens do Docker Hub, Maven Central e as releases do Gauge no GitHub).

```bash
cd testes-gauge
docker compose run --rm --build smoke    # sobe tudo, roda o smoke e devolve o código de saída dele
docker compose down                      # derruba a aplicação e o banco (down -v também apaga o banco)
```

O `run` sobe as dependências na ordem e espera cada uma:

| Serviço | O que faz |
|---|---|
| `empacotar` | `./mvnw package -DskipTests` do `exemplo-negocial` (gera `target/quarkus-app`) e termina |
| `postgres` | o banco, até responder |
| `app` | a aplicação empacotada (perfil `prod`), com a chave pública de desenvolvimento, até a porta 8080 abrir |
| `smoke` | `./mvnw test-compile gauge:execute`: compila os passos e roda as especificações |

Saída esperada:

```
# Smoke da API de consórcio
  ## A documentação da API está publicada	 ✔
  ## Sem token, a API recusa	 ✔ ✔
  ...
Specifications:	1 executed	1 passed	0 failed	0 skipped
Scenarios:	6 executed	6 passed	0 failed	0 skipped
```

Relatório: `reports/html-report/index.html`. Uma falha mostra a frase e o motivo (`GET /cotas/999999999: esperado 200, obtido 404`) e o `run` sai com código diferente de zero, o que basta para a esteira.

Use `docker compose run`, não `docker compose up --abort-on-container-exit`: o `empacotar` termina por natureza, e o `up` com essa opção derrubaria tudo assim que ele acabasse.

## Contra uma aplicação que já está rodando

Sem Docker para a aplicação (por exemplo, `./mvnw quarkus:dev` no `exemplo-negocial`, ou um ambiente de homologação), com o Gauge e o plugin java instalados na máquina:

```bash
export API_URL=http://localhost:8080
export CHAVE_PRIVADA_JWT=../exemplo-negocial/docs/dev/privada-desenvolvimento.pem
./mvnw test-compile gauge:execute
```

A chave é a de **desenvolvimento**: assina tokens que só a aplicação configurada com a chave pública de desenvolvimento aceita (`quarkus:dev` e este compose). Em homologação, troque o `Token` para obter o token do provedor de identidade.

## Acrescentar um smoke

1. Escreva o cenário em `specs/smoke.spec` (ou um `.spec` novo), em português, reaproveitando as frases que já existem.
2. Frase nova: um método com `@Step("frase com <parametro>")` em `PassosSmoke`. Ele chama a API (`Api.get`/`Api.post`) e confere a resposta; dados de um passo para o outro vão no `ScenarioDataStore`.
3. Cada cenário cria os próprios dados com valores únicos: o banco persiste entre execuções.

Smoke é curto: o caminho principal de cada endpoint. Regra de negócio e casos de erro detalhados ficam nos testes do serviço.

## Versões

Gauge 1.6.43, plugin `java` 1.0.5 e `html-report` 4.4.9 (argumentos do `Dockerfile`); `gauge-java` 1.0.5 e `gauge-maven-plugin` 2.0.0 no `pom.xml`. A versão do plugin `java` e a do `gauge-java` no `pom.xml` precisam ser a mesma.

## Observações

- No Linux, os arquivos que o `empacotar` gera em `exemplo-negocial/target/` ficam com dono `root` (o contêiner roda como root). Um `./mvnw clean` no host pode pedir `sudo`.
- A imagem do executor instala o Gauge pelas releases do GitHub; em rede corporativa que bloqueie o GitHub, publique a imagem num registro interno e troque o `build: .` do serviço `smoke` por `image:`.
