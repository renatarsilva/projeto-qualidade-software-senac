package br.senac.recursos.erro;

import jakarta.servlet.Filter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Só em src/test: libera as rotas de teste de erro (sem depender da T-15) e registra filtros que falham de propósito.
 * A cadeia tem a prioridade mais alta, para vir antes das cadeias reais (spec T-22, seção 6).
 */
@TestConfiguration(proxyBeanMethods = false)
class SegurancaDeTesteDeErros {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain cadeiaDasRotasDeTesteDeErro(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/testes-de-erro/**", "/teste-web/**", "/pagina-que-nao-existe")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(regras -> regras.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    FilterRegistrationBean<Filter> filtroQueFalhaNaApi() {
        return filtroQueFalhaEm("/api/testes-de-erro/falha-no-filtro");
    }

    @Bean
    FilterRegistrationBean<Filter> filtroQueFalhaNaTela() {
        return filtroQueFalhaEm("/teste-web/falha-no-filtro");
    }

    private FilterRegistrationBean<Filter> filtroQueFalhaEm(String caminho) {
        Filter filtro = (requisicao, resposta, cadeia) -> {
            throw new IllegalStateException("falha no filtro: SELECT * FROM usuario em br.senac.recursos");
        };
        FilterRegistrationBean<Filter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns(caminho);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }
}
