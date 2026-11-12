package br.senac.recursos.bdd;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.reserva.RelogioDeTesteConfiguracao;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Liga o Cucumber ao Spring: a aplicação inteira, o PostgreSQL real (Testcontainers) e o Clock fixo de teste
 * (segunda, 2026-11-09, 08:00 UTC, ADR-0008). É o mesmo contexto dos testes de API, então ele é reaproveitado.
 */
@CucumberContextConfiguration
@TesteDeApiComBanco
@Import(RelogioDeTesteConfiguracao.class)
// As 20 solicitações simultâneas do cenário usam um pool de 25 conexões, como o teste de concorrência da T-37.
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
public class ConfiguracaoDoCucumber {
}
