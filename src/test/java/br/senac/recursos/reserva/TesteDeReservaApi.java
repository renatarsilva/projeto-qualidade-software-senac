package br.senac.recursos.reserva;

import br.senac.recursos.TesteDeApiComBanco;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/** Teste de API com PostgreSQL real e o Clock fixo de teste (spec T-20, seção 6). */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TesteDeApiComBanco
@Import(RelogioDeTesteConfiguracao.class)
@interface TesteDeReservaApi {
}
