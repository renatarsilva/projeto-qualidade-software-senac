package br.senac.recursos.web;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.Usuario;
import br.senac.recursos.seguranca.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Confere a conta no banco a cada requisição das telas, como a API faz com o token (spec T-44, TC-32): quem foi
 * desativado perde a sessão na hora, e o perfil que vale é o do banco, e não o que ficou gravado na sessão.
 */
class ContaDaSessaoFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;

    ContaDaSessaoFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof UsernamePasswordAuthenticationToken && autenticacao.isAuthenticated()) {
            Optional<Usuario> conta = contaDe(autenticacao.getName());
            if (conta.isEmpty()) {
                SecurityContextHolder.clearContext();
                HttpSession sessao = requisicao.getSession(false);
                if (sessao != null) {
                    sessao.invalidate();
                }
            } else if (!temSoOPerfil(autenticacao, conta.get().getPerfil())) {
                SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        autenticacao.getPrincipal(), autenticacao.getCredentials(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + conta.get().getPerfil().name()))));
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private Optional<Usuario> contaDe(String nome) {
        try {
            return usuarios.findById(Long.parseLong(nome)).filter(Usuario::isAtivo);
        } catch (NumberFormatException erro) {
            return Optional.empty();
        }
    }

    private static boolean temSoOPerfil(Authentication autenticacao, Perfil perfil) {
        return autenticacao.getAuthorities().size() == 1
                && autenticacao.getAuthorities().iterator().next().getAuthority().equals("ROLE_" + perfil.name());
    }
}
