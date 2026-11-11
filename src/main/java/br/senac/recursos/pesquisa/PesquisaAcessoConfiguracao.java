package br.senac.recursos.pesquisa;

import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** A pesquisa de recursos é aberta a qualquer usuário autenticado (spec T-43, seção 4). */
@Configuration
public class PesquisaAcessoConfiguracao {

    @Bean
    RegraDeAcesso pesquisaDeRecursos() {
        return RegraDeAcesso.qualquerAutenticado(HttpMethod.GET, "/api/recursos");
    }
}
