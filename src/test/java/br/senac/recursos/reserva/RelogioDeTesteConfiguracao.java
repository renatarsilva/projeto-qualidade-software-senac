package br.senac.recursos.reserva;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Troca o Clock da aplicação pelo relógio fixo dos testes (ADR-0008). */
@TestConfiguration
public class RelogioDeTesteConfiguracao {

    @Bean
    @Primary
    RelogioDeTeste relogioDeTeste() {
        return new RelogioDeTeste();
    }
}
