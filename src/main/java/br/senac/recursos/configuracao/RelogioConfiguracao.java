package br.senac.recursos.configuracao;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expõe o {@link Clock} da aplicação em UTC (ADR-0008). Todo código que precisa da hora atual
 * o recebe por injeção; os testes substituem este bean por um relógio fixo.
 */
@Configuration
public class RelogioConfiguracao {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
