package br.senac.recursos.manutencao;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Só o Administrador registra, lista e encerra manutenções (RF-08). */
@Configuration
public class ManutencaoAcessoConfiguracao {

    @Bean
    RegraDeAcesso registroDeManutencao() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/recursos/*/manutencoes", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso listaDeManutencoes() {
        return RegraDeAcesso.soPara(HttpMethod.GET, "/api/recursos/*/manutencoes", Perfil.ADMINISTRADOR);
    }

    @Bean
    RegraDeAcesso encerramentoDeManutencao() {
        return RegraDeAcesso.soPara(HttpMethod.DELETE, "/api/recursos/*/manutencoes/*", Perfil.ADMINISTRADOR);
    }
}
