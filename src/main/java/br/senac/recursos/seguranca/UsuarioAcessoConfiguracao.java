package br.senac.recursos.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A gestão de usuários é exclusiva do Administrador. O padrão "/api/usuarios/**" cobre também a raiz. */
@Configuration
public class UsuarioAcessoConfiguracao {

    @Bean
    RegraDeAcesso gestaoDeUsuarios() {
        return RegraDeAcesso.soPara("/api/usuarios/**", Perfil.ADMINISTRADOR);
    }
}
