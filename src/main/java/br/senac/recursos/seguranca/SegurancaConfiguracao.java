package br.senac.recursos.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/** Duas cadeias de segurança (ADR-0007): a API sem estado e sem CSRF, e as telas com formulário e sessão. */
@Configuration
@EnableWebSecurity
public class SegurancaConfiguracao {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http, AuthenticationEntryPoint entradaDaApi, JwtDecoder decodificador)
            throws Exception {
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros.authenticationEntryPoint(entradaDaApi))
                .oauth2ResourceServer(token -> token
                        .jwt(jwt -> jwt.decoder(decodificador))
                        .authenticationEntryPoint(entradaDaApi));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain telas(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(regras -> regras.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    /** Decodifica o token da API: assinatura HS256 e validade "hora atual menor que a expiração" (P-01). */
    @Bean
    JwtDecoder decodificadorDoToken(@Value("${app.seguranca.chave-assinatura}") String chave, Clock clock) {
        SecretKey chaveSecreta = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chaveSecreta)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(jwt -> {
            if (jwt.getExpiresAt() != null && clock.instant().isBefore(jwt.getExpiresAt())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "token expirado", null));
        });
        return decodificador;
    }

    /** Resposta 401 da API: sem token, token inválido ou expirado. Não revela nenhum detalhe interno. */
    @Bean
    AuthenticationEntryPoint entradaDaApi(ObjectMapper mapeador) {
        return (requisicao, resposta, erro) -> {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "É necessário estar autenticado para acessar este recurso.");
            problema.setTitle("Autenticação necessária");
            problema.setInstance(URI.create(requisicao.getRequestURI()));
            resposta.setStatus(HttpStatus.UNAUTHORIZED.value());
            resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
            mapeador.writeValue(resposta.getWriter(), problema);
        };
    }
}
