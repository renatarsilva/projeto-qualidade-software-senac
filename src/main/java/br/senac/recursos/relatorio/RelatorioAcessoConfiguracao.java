package br.senac.recursos.relatorio;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** O Responsável e o Administrador consultam o relatório (NC-08); o Solicitante recebe 403. */
@Configuration
public class RelatorioAcessoConfiguracao {

    @Bean
    RegraDeAcesso relatorioDeUtilizacao() {
        return RegraDeAcesso.soPara(HttpMethod.GET, "/api/relatorios/utilizacao", Perfil.RESPONSAVEL,
                Perfil.ADMINISTRADOR);
    }
}
