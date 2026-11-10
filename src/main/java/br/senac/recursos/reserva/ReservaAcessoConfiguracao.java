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
}
