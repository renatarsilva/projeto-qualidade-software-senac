package br.senac.recursos.web;

import br.senac.recursos.seguranca.Usuario;
import br.senac.recursos.seguranca.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carrega a conta para o login das telas (ADR-0007). O nome de usuário da sessão é o <b>id</b> do usuário, como na API:
 * assim os serviços recebem o dono da mesma forma nos dois caminhos. Conta inexistente e conta desativada dão o mesmo
 * erro (spec T-15, CA-02), para a tela não revelar qual e-mail existe.
 */
@Service
class ContaDoUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarios;

    ContaDoUsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarios.findByEmail(email).filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsernameNotFoundException("conta inexistente ou desativada"));
        return User.withUsername(String.valueOf(usuario.getId())).password(usuario.getSenhaHash())
                .roles(usuario.getPerfil().name()).build();
    }
}
