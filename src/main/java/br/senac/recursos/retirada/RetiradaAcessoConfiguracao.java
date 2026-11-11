package br.senac.recursos.retirada;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Só o Responsável registra a retirada e a devolução (DP-04); o Administrador também consulta. */
@Configuration
public class RetiradaAcessoConfiguracao {

    @Bean
    RegraDeAcesso registroDeRetirada() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/reservas/*/retiradas", Perfil.RESPONSAVEL);
    }

    @Bean
    RegraDeAcesso registroDeDevolucao() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/reservas/*/retiradas/*/devolucao", Perfil.RESPONSAVEL);
    }

    @Bean
    RegraDeAcesso consultaDeRetiradas() {
        return RegraDeAcesso.soPara(HttpMethod.GET, "/api/reservas/*/retiradas", Perfil.RESPONSAVEL,
                Perfil.ADMINISTRADOR);
    }
}
