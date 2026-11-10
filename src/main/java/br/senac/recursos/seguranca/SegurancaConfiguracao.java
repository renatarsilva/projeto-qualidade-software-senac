package br.senac.recursos.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.senac.recursos.erro.FabricaDeProblemas;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
            Converter<Jwt, AbstractAuthenticationToken> conversorDoToken, ObjectProvider<RegraDeAcesso> regras)
            throws Exception {
        List<RegraDeAcesso> declaradas = regras.orderedStream().toList();
        http.securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacao -> {
                    autorizacao.requestMatchers("/api/auth/login").permitAll();
                    for (RegraDeAcesso regra : declaradas) {
                        var alvo = regra.metodo() == null
                                ? autorizacao.requestMatchers(regra.padrao())
                                : autorizacao.requestMatchers(regra.metodo(), regra.padrao());
                        if (regra.perfis().isEmpty()) {
                            alvo.authenticated();
                        } else {
                            String[] papeis = regra.perfis().stream().map(Perfil::name).toArray(String[]::new);
                            alvo.hasAnyRole(papeis);
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
        http.authorizeHttpRequests(regras -> regras
                        .requestMatchers("/error").permitAll() // a página de erro é aberta, senão o erro viraria login
                        .anyRequest().authenticated())
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

    /**
     * Confere o usuário no banco a cada chamada (CA-15): quem foi desativado ou não existe mais perde o acesso na hora,
     * e o perfil que vale é o do banco, e não o que ficou gravado no token. O perfil vira a permissão ROLE_<perfil>.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> conversorDoToken(UsuarioRepository usuarios) {
        return jwt -> {
            Usuario usuario = buscaUsuarioAtivo(usuarios, jwt.getSubject());
            List<GrantedAuthority> permissoes = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
            return new JwtAuthenticationToken(jwt, permissoes, String.valueOf(usuario.getId()));
        };
    }

    private Usuario buscaUsuarioAtivo(UsuarioRepository usuarios, String subject) {
        try {
            return usuarios.findById(Long.parseLong(subject))
                    .filter(Usuario::isAtivo)
                    .orElseThrow(() -> new InvalidBearerTokenException("usuário inexistente ou desativado"));
        } catch (NumberFormatException erro) {
            throw new InvalidBearerTokenException("token sem usuário válido");
        }
    }

    /** Resposta 401 da API: sem token, token inválido ou expirado. Usa a fábrica de erros da T-22. */
    @Bean
    AuthenticationEntryPoint entradaDaApi(ObjectMapper mapeador) {
        return (requisicao, resposta, erro) -> escreve(mapeador, resposta,
                FabricaDeProblemas.autenticacaoNecessaria(requisicao.getRequestURI()));
    }

    /** Resposta 403 da API: usuário autenticado sem permissão. Usa a fábrica de erros da T-22. */
    @Bean
    AccessDeniedHandler acessoNegadoDaApi(ObjectMapper mapeador) {
        return (requisicao, resposta, erro) -> escreve(mapeador, resposta,
                FabricaDeProblemas.acessoNegado(requisicao.getRequestURI()));
    }

    private static void escreve(ObjectMapper mapeador, HttpServletResponse resposta, ProblemDetail problema)
            throws IOException {
        resposta.setStatus(problema.getStatus());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapeador.writeValue(resposta.getWriter(), problema);
    }
}
