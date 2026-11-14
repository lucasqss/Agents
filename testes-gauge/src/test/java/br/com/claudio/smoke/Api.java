package br.com.claudio.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Chamadas HTTP à aplicação em teste. A URL base vem de {@code API_URL} (padrão: http://localhost:8080). */
final class Api {

    private static final String URL = System.getenv().getOrDefault("API_URL", "http://localhost:8080");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private Api() {
    }

    record Resposta(int status, String corpo) {

        JsonNode json() {
            try {
                return JSON.readTree(corpo);
            } catch (IOException e) {
                throw new IllegalStateException("Resposta não é JSON: " + corpo, e);
            }
        }
    }

    static Resposta get(String caminho, String token) {
        return enviar(requisicao(caminho, token).GET());
    }

    static Resposta post(String caminho, String token, Object corpo) {
        try {
            return enviar(requisicao(caminho, token).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo))));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Espera a aplicação responder, até o limite. Usado antes da suíte: o contêiner pode estar subindo. */
    static void aguardar(Duration limite) {
        long fim = System.nanoTime() + limite.toNanos();
        while (System.nanoTime() < fim) {
            try {
                if (get("/q/openapi", null).status() == 200) {
                    return;
                }
            } catch (IllegalStateException ainda) {
                // ainda subindo
            }
            dormir();
        }
        throw new IllegalStateException("A aplicação não respondeu em " + URL + " dentro de " + limite.toSeconds() + " s");
    }

    private static HttpRequest.Builder requisicao(String caminho, String token) {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(URL + caminho)).timeout(Duration.ofSeconds(10));
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao;
    }

    private static Resposta enviar(HttpRequest.Builder requisicao) {
        try {
            HttpResponse<String> resposta = HTTP.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
            return new Resposta(resposta.statusCode(), resposta.body());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao chamar " + URL + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void dormir() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
