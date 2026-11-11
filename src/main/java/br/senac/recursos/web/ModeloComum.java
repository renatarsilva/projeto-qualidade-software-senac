package br.senac.recursos.web;

import br.senac.recursos.seguranca.UsuarioRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Entrega a todas as telas o nome e o perfil de quem está logado, para o menu. */
@ControllerAdvice(basePackageClasses = ModeloComum.class)
class ModeloComum {

    private final UsuarioRepository usuarios;

    ModeloComum(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    record Conta(String nome, String perfil) {
    }

    @ModelAttribute("conta")
    Conta conta(Authentication autenticacao) {
        if (!(autenticacao instanceof UsernamePasswordAuthenticationToken)) {
            return null;
        }
        return usuarios.findById(Long.parseLong(autenticacao.getName()))
                .map(u -> new Conta(u.getNome(), u.getPerfil().name())).orElse(null);
    }
}
