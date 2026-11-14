package br.com.claudio.base;

import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Roda os mesmos testes contra o artefato empacotado (failsafe, fase verify). O artefato sobe no perfil prod, que exige a
 * chave pública e o emissor do provedor de identidade (ADR 003): o recurso de teste os entrega, como nos ITs de aceitação.
 */
@QuarkusIntegrationTest
@WithTestResource(TesourariaStubResource.class)
class OpenApiIT extends OpenApiTest {
}
