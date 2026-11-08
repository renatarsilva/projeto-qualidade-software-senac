package br.senac.recursos.sala;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Perfil exigido em cada operação das salas: consulta aberta a qualquer autenticado, escrita só do Administrador. */
@Configuration
public class SalaAcessoConfiguracao {

    @Bean
    RegraDeAcesso consultaDeSalas() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/salas/**");
    }

    @Bean
    RegraDeAcesso listaDeSalas() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/salas");
    }

    @Bean
    RegraDeAcesso cadastroDeSalas() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/salas", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso alteracaoDeSalas() {
        return RegraDeAcesso.soPara(HttpMethod.PUT, "/api/salas/**", Perfil.ADMINISTRADOR);
    }
}
