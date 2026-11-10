package br.senac.recursos.reserva;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Perfil exigido para criar e alterar reservas: só o Solicitante (spec T-20, seção 4). */
@Configuration
public class ReservaAcessoConfiguracao {

    @Bean
    RegraDeAcesso criacaoDeReservas() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/reservas", Perfil.SOLICITANTE);
    }

    @Bean
    RegraDeAcesso alteracaoDeReservas() {
        return RegraDeAcesso.soPara(HttpMethod.PUT, "/api/reservas/**", Perfil.SOLICITANTE);
    }

    // T-21: só o Responsável muda o estado pelo PATCH; cancelamento, check-in e o DELETE (que só recusa) são do dono.

    @Bean
    RegraDeAcesso mudancaDeEstado() {
        return RegraDeAcesso.soPara(HttpMethod.PATCH, "/api/reservas/*/estado", Perfil.RESPONSAVEL);
    }

    @Bean
    RegraDeAcesso cancelamentoDeReservas() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/reservas/*/cancelamento", Perfil.SOLICITANTE);
    }

    @Bean
    RegraDeAcesso checkinDeReservas() {
        return RegraDeAcesso.soPara(HttpMethod.POST, "/api/reservas/*/checkin", Perfil.SOLICITANTE);
    }

    @Bean
    RegraDeAcesso exclusaoDeReservas() {
        return RegraDeAcesso.soPara(HttpMethod.DELETE, "/api/reservas/*", Perfil.SOLICITANTE);
    }
}
