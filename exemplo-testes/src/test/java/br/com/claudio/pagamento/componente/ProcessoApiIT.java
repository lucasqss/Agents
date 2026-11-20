package br.com.claudio.pagamento.componente;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/** Os mesmos testes contra o artefato empacotado (failsafe, fase verify): prova o jar, não só o código. */
@QuarkusIntegrationTest
class ProcessoApiIT extends ProcessoApiTest {
}
