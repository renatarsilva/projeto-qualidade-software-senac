package br.senac.recursos.seguranca;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Declara o perfil exigido de cada rota de teste. A rota "sem-regra" fica de propósito sem declaração. */
@TestConfiguration(proxyBeanMethods = false)
class RegrasDeAcessoDeTeste {

    @Bean
    RegraDeAcesso rotaDoAdministrador() {
        return RegraDeAcesso.soPara("/api/teste/administrador", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso rotaDeQualquerAutenticado() {
        return RegraDeAcesso.qualquerAutenticado("/api/teste/autenticado");
    }
}
