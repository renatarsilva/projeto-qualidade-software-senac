package br.senac.recursos.web;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.reserva.RelogioDeTesteConfiguracao;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Teste das telas com PostgreSQL real e o Clock fixo de teste: segunda, 2026-11-09, 08:00 UTC (ADR-0008). */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TesteDeApiComBanco
@Import(RelogioDeTesteConfiguracao.class)
@interface TesteDeTelas {
}
