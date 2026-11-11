package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/** Posse da reserva e ordem dos erros (spec T-49, CA-04 e CA-05; TC-05 e TC-06). */
@TesteDeReservaApi
class PosseEOrdemDosErrosApiTest extends BaseDaApiDeReserva {

    @Test
    @DisplayName("TC-05: o Solicitante B altera, cancela e faz check-in da reserva de A e recebe 403 nas três; a reserva não muda")
    void naoDonoNaoMexeNaReserva() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(terca(10, 30));
        String bearerDoOutro = ambiente.bearerDoOutro();

        ambiente.altera(bearerDoOutro, id, corpo(terca(14, 0), terca(15, 0), sala)).andExpect(status().isForbidden());
        ambiente.cancela(bearerDoOutro, id).andExpect(status().isForbidden());
        ambiente.checkin(bearerDoOutro, id).andExpect(status().isForbidden());

        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
        assertThat(ambiente.inicioNoBanco(id)).isEqualTo(terca(10, 0));
    }

    @Test
    @DisplayName("TC-06: quem não é o dono, com corpo inválido, recebe 403 (dono antes de 400)")
    void donoVemAntesDoCorpoInvalido() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.altera(ambiente.bearerDoOutro(), id, corpo(terca(11, 0), terca(10, 0), sala))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-06: o Solicitante envia \"ABC\" no PATCH e recebe 403 (perfil antes de 400)")
    void perfilVemAntesDaValidacao() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), id, "ABC").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-06: o Responsável pede o cancelamento de um id que não existe e recebe 403 (perfil antes de 404)")
    void perfilVemAntesDaExistencia() throws Exception {
        ambiente.cancela(ambiente.bearerDe(Perfil.RESPONSAVEL), 999999).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-06: o Solicitante pede o cancelamento de um id que não existe e recebe 404 (perfil certo)")
    void donoDeReservaInexistenteRecebe404() throws Exception {
        ambiente.cancela(ambiente.bearerDoSolicitante(), 999999).andExpect(status().isNotFound());
        assertThat(ambiente.reservas()).isZero();
    }

    @Test
    @DisplayName("TC-06: a chamada sem token a uma rota com corpo inválido devolve 401 (autenticação antes de tudo)")
    void autenticacaoVemPrimeiro() throws Exception {
        ambiente.chama(HttpMethod.POST, "/api/reservas", null, "{\"inicio\":\"lixo\"}")
                .andExpect(status().isUnauthorized());
    }
}
