package br.senac.recursos.professor;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Perfil exigido em cada operação dos professores: consulta aberta a qualquer autenticado, escrita só do Administrador. */
@Configuration
public class ProfessorAcessoConfiguracao {

    @Bean
    RegraDeAcesso consultaDeProfessores() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/professores/**");
    }

    @Bean
    RegraDeAcesso listaDeProfessores() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/professores");
    }

    @Bean
    RegraDeAcesso cadastroDeProfessores() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/professores", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso alteracaoDeProfessores() {
        return RegraDeAcesso.soPara(HttpMethod.PUT, "/api/professores/**", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso exclusaoDeProfessores() {
        return RegraDeAcesso.soPara(HttpMethod.DELETE, "/api/professores/**", Perfil.ADMINISTRADOR);
    }
}
