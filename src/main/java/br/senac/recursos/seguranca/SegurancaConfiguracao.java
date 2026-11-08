package br.senac.recursos.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Duas cadeias de segurança (ADR-0007): a API sem estado e sem CSRF, e as telas com formulário e sessão. */
@Configuration
@EnableWebSecurity
public class SegurancaConfiguracao {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain telas(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(regras -> regras.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
