---
mode: agent
description: Cria o módulo de um canal novo (aplicação própria, dependências corporativas do canal, token, padrão de apresentação).
---
Crie o módulo do canal descrito abaixo seguindo `.github/copilot-instructions.md`, `.github/instructions/canais.instructions.md` e o exemplo `canal-mobile`.

Canal: ${input:canal:nome curto em minúsculas, ex.: "parceiro"}
Escrita: ${input:escrita:"somente leitura" ou o que o canal pode gravar}
Dependências corporativas: ${input:deps:groupId:artifactId das bibliotecas do canal (segurança, log...)}
Token: ${input:token:formato do token do canal e de onde vem o id do usuário}

1. Módulo `canal-<canal>/` com `pom.xml` copiado de `canal-mobile` (packaging `quarkus`, depende só de `canal-comum`) e as dependências corporativas do canal. Inclua o módulo em `<modules>` do pom raiz e como dependência de teste em `arquitetura/pom.xml`.
2. `src/main/resources/application.properties` com `quarkus.application.name`, porta própria e título do OpenAPI. Configuração comum vem do núcleo.
3. Pacotes em `canal/<canal>/`:
   - `package-info.java` (com `@SomenteLeitura` se o canal não escreve);
   - `seguranca/MapeadorDeToken<Canal>`: `caminhosProtegidos()` = `"/<canal>/"` e o token -> `Solicitante(id, "<CANAL>")`;
   - `apresentacao/Padrao<Canal>`: o padrão de exibição da empresa para o canal;
   - `entrypoint/`: os endpoints (prompt `novo-endpoint-de-canal`).
4. Domínio: acrescente o canal em `CanalDeAtendimento` com o que ele pode abrir, e teste em `CanalDeAtendimentoTest`.
5. Testes no módulo: `Padrao<Canal>Test`, `Contrato<Canal>Test` (com e sem token) e `OpenApiTest`/`OpenApiIT`.
6. Deploy: `src/main/docker/Dockerfile.jvm`, serviço no `docker-compose.yml` e a rota `/<canal>/` no API gateway corporativo.
7. Rode `./mvnw verify` na raiz. Nenhuma regra ArchUnit muda para um canal novo.
