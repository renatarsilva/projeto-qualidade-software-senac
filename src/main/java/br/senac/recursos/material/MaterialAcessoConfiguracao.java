package br.senac.recursos.material;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Perfil exigido em cada operação dos materiais: consulta aberta a qualquer autenticado, escrita só do Administrador. */
@Configuration
public class MaterialAcessoConfiguracao {

    @Bean
    RegraDeAcesso consultaDeMateriais() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/materiais/**");
    }

    @Bean
    RegraDeAcesso listaDeMateriais() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/materiais");
    }

    @Bean
    RegraDeAcesso cadastroDeMateriais() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/materiais", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso alteracaoDeMateriais() {
        return RegraDeAcesso.soPara(HttpMethod.PUT, "/api/materiais/**", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso exclusaoDeMateriais() {
        return RegraDeAcesso.soPara(HttpMethod.DELETE, "/api/materiais/**", Perfil.ADMINISTRADOR);
    }
}
