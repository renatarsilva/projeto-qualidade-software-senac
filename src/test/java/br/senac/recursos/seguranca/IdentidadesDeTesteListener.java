package br.senac.recursos.seguranca;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/** Entrega o banco do contexto do teste às IdentidadesDeTeste (registrado em META-INF/spring.factories). */
public class IdentidadesDeTesteListener extends AbstractTestExecutionListener {

    @Override
    public void prepareTestInstance(TestContext testContext) {
        var contexto = testContext.getApplicationContext();
        if (contexto.getBeanNamesForType(JdbcTemplate.class).length > 0) {
            IdentidadesDeTeste.usaBanco(contexto.getBean(JdbcTemplate.class));
        }
    }
}
