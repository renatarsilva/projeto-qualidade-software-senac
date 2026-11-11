package br.senac.recursos.web;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * A cadeia de segurança das telas (ADR-0007, spec T-44): login por formulário, sessão por cookie e CSRF ligado.
 * Negado por padrão: a rota que nenhuma regra abaixo libera é recusada, inclusive para o Administrador. As regras repetem
 * a matriz de autorização da API, e o teste da matriz das telas cruza cada rota com cada perfil.
 */
@Configuration
class TelasSegurancaConfiguracao {

    private static final String SOLICITANTE = Perfil.SOLICITANTE.name();
    private static final String RESPONSAVEL = Perfil.RESPONSAVEL.name();
    private static final String ADMINISTRADOR = Perfil.ADMINISTRADOR.name();

    @Bean
    @Order(2)
    SecurityFilterChain telas(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        http.authorizeHttpRequests(regras -> regras
                        .requestMatchers("/error", "/login").permitAll() // a página de erro é aberta, senão o erro viraria login
                        .requestMatchers("/webjars/**", "/css/**").permitAll()
                        // A documentação da API (OpenAPI e Swagger UI) é pública (RF-14, T-51).
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        // Qualquer usuário autenticado: a posse da reserva é conferida no serviço (NC-01).
                        .requestMatchers(HttpMethod.GET, "/", "/recursos", "/reservas", "/reservas/{id:\\d+}")
                        .authenticated()
                        // Só o Solicitante cria, altera, cancela e faz o check-in (matriz R-01 a R-04).
                        .requestMatchers(HttpMethod.GET, "/reservas/nova", "/reservas/{id:\\d+}/editar")
                        .hasRole(SOLICITANTE)
                        .requestMatchers(HttpMethod.POST, "/reservas", "/reservas/{id:\\d+}/editar",
                                "/reservas/{id:\\d+}/cancelar", "/reservas/{id:\\d+}/checkin")
                        .hasRole(SOLICITANTE)
                        // Só o Responsável aprova, rejeita, retira e devolve (R-05, RT-01, RT-02).
                        .requestMatchers(HttpMethod.GET, "/aprovacoes").hasRole(RESPONSAVEL)
                        .requestMatchers(HttpMethod.POST, "/reservas/{id:\\d+}/aprovar", "/reservas/{id:\\d+}/rejeitar",
                                "/reservas/{id:\\d+}/retiradas", "/reservas/{id:\\d+}/retiradas/{retirada:\\d+}/devolucao")
                        .hasRole(RESPONSAVEL)
                        .requestMatchers(HttpMethod.GET, "/relatorio").hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        // Cadastros, usuários e manutenção: só o Administrador.
                        .requestMatchers("/admin/salas", "/admin/materiais", "/admin/professores", "/admin/usuarios",
                                "/admin/usuarios/{id:\\d+}/ativo", "/admin/usuarios/{id:\\d+}/perfil",
                                "/admin/manutencoes", "/admin/manutencoes/{id:\\d+}/encerrar")
                        .hasRole(ADMINISTRADOR)
                        .anyRequest().denyAll())
                .formLogin(login -> login.loginPage("/login").failureUrl("/login?erro")
                        .defaultSuccessUrl("/", true))
                .logout(saida -> saida.logoutSuccessUrl("/login?saiu"))
                .addFilterAfter(new ContaDaSessaoFilter(usuarios), SecurityContextHolderFilter.class);
        return http.build();
    }

    /** O mesmo BCrypt da API, para conferir a senha do formulário contra o hash do banco. */
    @Bean
    PasswordEncoder codificadorDasTelas() {
        return new BCryptPasswordEncoder();
    }
}
