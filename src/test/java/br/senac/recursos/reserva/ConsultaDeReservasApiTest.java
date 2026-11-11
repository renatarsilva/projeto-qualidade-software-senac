package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;

/** Consulta de reservas, com a posse respeitada (spec T-43, CA-08 a CA-10; TC-14 a TC-19). */
@TesteDeReservaApi
class ConsultaDeReservasApiTest extends BaseDaApiDeReserva {

    private List<Integer> ids(String bearer, String consulta) throws Exception {
        MockHttpServletResponse resposta = ambiente.chama(HttpMethod.GET, "/api/reservas" + consulta, bearer, null)
                .andReturn().getResponse();
        assertThat(resposta.getStatus()).as("status da lista %s", consulta).isEqualTo(200);
        return JsonPath.read(resposta.getContentAsString(), "$[*].id");
    }

    private long reserva(long donoId, EstadoDaReserva estado, java.time.Instant inicio, java.time.Instant fim) {
        return ambiente.reservaGravada(donoId, estado, inicio, fim, AGORA, sala);
    }

    @Test
    @DisplayName("TC-14: o Solicitante lista só as próprias reservas, ordenadas pelo início")
    void solicitanteVeSoAsProprias() throws Exception {
        long outro = ambiente.outroSolicitante();
        long tarde = reserva(dono, EstadoDaReserva.SOLICITADA, quarta(14, 0), quarta(15, 0));
        long cedo = reserva(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0));
        reserva(outro, EstadoDaReserva.APROVADA, terca(12, 0), terca(13, 0));

        assertThat(ids(ambiente.bearerDoSolicitante(), "")).containsExactly((int) cedo, (int) tarde);
    }

    @Test
    @DisplayName("TC-15: o filtro estado=APROVADA restringe a lista; estado=ABC devolve 400")
    void filtroPorEstado() throws Exception {
        long aprovada = reserva(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0));
        reserva(dono, EstadoDaReserva.SOLICITADA, terca(12, 0), terca(13, 0));

        assertThat(ids(ambiente.bearerDoSolicitante(), "?estado=APROVADA")).containsExactly((int) aprovada);
        assertThat(ambiente.chama(HttpMethod.GET, "/api/reservas?estado=ABC", ambiente.bearerDoSolicitante(), null)
                .andReturn().getResponse().getStatus()).isEqualTo(400);
    }

    @ParameterizedTest(name = "TC-16: {0} vê as reservas de todos")
    @EnumSource(value = Perfil.class, names = {"RESPONSAVEL", "ADMINISTRADOR"})
    @DisplayName("TC-16: o Responsável e o Administrador listam as reservas de todos os Solicitantes")
    void responsavelEAdministradorVeemTodas(Perfil perfil) throws Exception {
        long outro = ambiente.outroSolicitante();
        long minha = reserva(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0));
        long dele = reserva(outro, EstadoDaReserva.APROVADA, terca(12, 0), terca(13, 0));

        assertThat(ids(ambiente.bearerDe(perfil), "")).containsExactly((int) minha, (int) dele);
    }

    @ParameterizedTest(name = "TC-17: {0} consulta a reserva por id")
    @EnumSource(value = Perfil.class)
    @DisplayName("TC-17: o dono, o Responsável e o Administrador consultam a reserva por id e recebem 200")
    void consultaPorId(Perfil perfil) throws Exception {
        long id = reserva(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0));
        String bearer = perfil == Perfil.SOLICITANTE ? ambiente.bearerDoSolicitante() : ambiente.bearerDe(perfil);

        MockHttpServletResponse resposta = ambiente.chama(HttpMethod.GET, "/api/reservas/" + id, bearer, null)
                .andReturn().getResponse();

        assertThat(resposta.getStatus()).as("status").isEqualTo(200);
        assertThat((Integer) JsonPath.read(resposta.getContentAsString(), "$.id")).isEqualTo((int) id);
    }

    @Test
    @DisplayName("TC-18: o Solicitante B consulta a reserva de A por id e recebe 403")
    void naoDonoNaoConsulta() throws Exception {
        long id = reserva(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0));

        int status = ambiente.chama(HttpMethod.GET, "/api/reservas/" + id, ambiente.bearerDoOutro(), null)
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(403);
    }

    @Test
    @DisplayName("TC-19: consultar uma reserva que não existe devolve 404")
    void reservaInexistente() throws Exception {
        int status = ambiente.chama(HttpMethod.GET, "/api/reservas/999999", ambiente.bearerDoSolicitante(), null)
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(404);
    }

    @Test
    @DisplayName("TC-20: recurso em manutenção no intervalo não aparece na disponibilidade (T-35)")
    void manutencaoTiraODisponivel() throws Exception {
        ambiente.manutencao(sala, terca(10, 0), terca(12, 0), "Manutenção").andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        MockHttpServletResponse resposta = ambiente.chama(HttpMethod.GET, "/api/recursos?tipo=SALA&disponivelDe="
                + terca(10, 30) + "&disponivelAte=" + terca(11, 30), ambiente.bearerDoSolicitante(), null)
                .andReturn().getResponse();

        assertThat((List<String>) JsonPath.read(resposta.getContentAsString(), "$[*].nome")).isEmpty();
    }
}
