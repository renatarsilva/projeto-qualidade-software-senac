package br.senac.recursos.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/** Duas cadeias de segurança (ADR-0007): a API sem estado e sem CSRF, e as telas com formulário e sessão. */
@Configuration
@EnableWebSecurity
public class SegurancaConfiguracao {

    @Bean
    @Order(1)
    SecurityFilterChain api(HttpSecurity http, AuthenticationEntryPoint entradaDaApi,
            AccessDeniedHandler acessoNegadoDaApi, JwtDecoder decodificador,
            JwtAuthenticationConverter conversorDoToken, ObjectProvider<RegraDeAcesso> regras) throws Exception {
        List<RegraDeAcesso> declaradas = regras.orderedStream().toList();
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> {
                    autorizacao.requestMatchers("/api/auth/login").permitAll();
                    for (RegraDeAcesso regra : declaradas) {
                        if (regra.perfis().isEmpty()) {
                            autorizacao.requestMatchers(regra.padrao()).authenticated();
                        } else {
                            String[] papeis = regra.perfis().stream().map(Perfil::name).toArray(String[]::new);
                            autorizacao.requestMatchers(regra.padrao()).hasAnyRole(papeis);
                        }
                    }
                    // Negado por padrão: o que nenhuma regra liberou é recusado (spec T-15, CA-08).
                    autorizacao.anyRequest().denyAll();
                })
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(entradaDaApi)
                        .accessDeniedHandler(acessoNegadoDaApi))
                .oauth2ResourceServer(token -> token
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(conversorDoToken))
                        .authenticationEntryPoint(entradaDaApi)
                        .accessDeniedHandler(acessoNegadoDaApi));
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

    /** O perfil gravado no token vira a permissão ROLE_<perfil> que as regras de acesso consultam. */
    @Bean
    JwtAuthenticationConverter conversorDoToken() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("perfil");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    /** Resposta 403 da API: usuário autenticado sem permissão. Não revela nenhum detalhe interno. */
    @Bean
    AccessDeniedHandler acessoNegadoDaApi(ObjectMapper mapeador) {
        return (requisicao, resposta, erro) -> {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação.");
            problema.setTitle("Acesso negado");
            problema.setInstance(URI.create(requisicao.getRequestURI()));
            resposta.setStatus(HttpStatus.FORBIDDEN.value());
            resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
            mapeador.writeValue(resposta.getWriter(), problema);
        };
    }
}
