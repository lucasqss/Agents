# Sistemas externos simulados

Nunca chame o sistema real nos testes. Simule com **WireMock**, num recurso de teste que sobe antes da aplicação e entrega a ela a URL do simulador. Funciona no `@QuarkusTest` e também no `*IT` da aceitação, porque a aplicação chama o simulador por HTTP.

Dependência: `org.wiremock:wiremock-standalone` em escopo de teste (empacotado, não conflita com as dependências do Quarkus).

## O recurso

`src/test/java/<pacote-base>/componente/<Sistema>Simulado.java`

```java
public class AntifraudeSimulado implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        // Propriedade de sistema: o teste fala com o simulador sem carregar as mesmas classes da aplicação.
        System.setProperty("teste.antifraude.porta", String.valueOf(servidor.port()));
        return Map.of("quarkus.rest-client.antifraude.url", servidor.baseUrl());
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
```

No teste: `@QuarkusTest @WithTestResource(AntifraudeSimulado.class)`. O mapa devolvido por `start()` vence a configuração `%test.*` da mesma chave. Configurações que só valem em teste (timeout e espera do circuito encurtados) também entram nesse mapa.

## Controle por verbos de negócio

O teste não monta requisição de WireMock. Uma classe de apoio dá nome ao que o sistema externo faz:

```java
final class Antifraude {

    static void aprovarTudo() { ... }      // situação normal, no @BeforeEach

    static void recusar(String documento) { ... }

    static void ficarIndisponivel() {      // 503 em toda chamada
        WireMock cliente = new WireMock("localhost", Integer.getInteger("teste.antifraude.porta"));
        cliente.resetMappings();
        cliente.register(get(urlPathMatching("/avaliacoes/.*")).willReturn(aResponse().withStatus(503)));
    }

    static List<String> documentosConsultados() { ... }   // o que o serviço enviou
}
```

Devolva um valor que só o simulador emite (um protocolo fixo): encontrá-lo na resposta prova que a chamada passou por ele.

## O que provar

- **Sucesso, recusa e indisponível** viram a resposta prevista do serviço (`503` quando o sistema está fora).
- **Timeout e circuito:** com a resiliência encurtada no recurso, a chamada lenta vira a resposta prevista e o circuito aberto não chama o simulador.
- **Idempotência:** a chamada repetida leva a mesma chave; consulte o que o simulador recebeu.
- Com circuito, não conte quantas chamadas chegaram enquanto o simulador responde erro: com o circuito aberto nem toda tentativa chega.

## Service Stub dentro da aplicação

Um recurso JAX-RS em `src/test` que faz o papel do sistema externo é mais simples para o `@QuarkusTest`, mas **não existe no artefato empacotado**: não serve para o `*IT` da aceitação. Modelo no exemplo: [Service Stub para testes](../../arquiteto-padrao/references/exemplos-codigo.md#service-stub-para-testes).
