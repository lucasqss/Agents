package br.com.claudio.base;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/** Roda os mesmos testes contra o artefato empacotado (failsafe, fase verify). */
@QuarkusIntegrationTest
class OpenApiIT extends OpenApiTest {
}
