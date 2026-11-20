package br.com.claudio.pagamento.application.processo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Clock;

/** O relógio dos casos de uso é um bean: o teste de unidade troca por um relógio fixo e confere a data do histórico. */
@ApplicationScoped
class Relogio {

    @Produces
    @ApplicationScoped
    Clock relogio() {
        return Clock.systemUTC();
    }
}
