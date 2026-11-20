package br.com.claudio.pagamento.aceitacao;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/** Os mesmos cenários contra o jar empacotado, num banco novo com a mesma massa: os IDs esperados são os mesmos. */
@QuarkusIntegrationTest
class ProcessoAceitacaoIT extends ProcessoAceitacaoTest {
}
