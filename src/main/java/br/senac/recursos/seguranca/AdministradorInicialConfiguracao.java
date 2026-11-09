package br.senac.recursos.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Liga o administrador inicial à subida da aplicação. */
@Configuration
public class AdministradorInicialConfiguracao {

    @Bean
    ApplicationRunner garantirAdministradorNaSubida(UsuarioRepository usuarios,
            @Value("${app.admin-inicial.email:}") String email,
            @Value("${app.admin-inicial.senha:}") String senha) {
        AdministradorInicial inicial = new AdministradorInicial(usuarios, new CodificadorDeSenha(), email, senha);
        return argumentos -> inicial.garantirAdministrador();
    }
}
