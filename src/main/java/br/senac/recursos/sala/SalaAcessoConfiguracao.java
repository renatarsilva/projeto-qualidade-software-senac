package br.senac.recursos.sala;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Perfil exigido nas rotas de sala. O ciclo 6 refina: consulta aberta a qualquer usuário autenticado. */
@Configuration
public class SalaAcessoConfiguracao {

    @Bean
    RegraDeAcesso regraDeAcessoDasSalas() {
        return RegraDeAcesso.soPara("/api/salas/**", Perfil.ADMINISTRADOR);
    }
}
