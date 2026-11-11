package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;

/** Auditoria pela API e consulta do histórico (spec T-40, CA-01 a CA-03, CA-06 a CA-08, CA-10; TC-01, TC-02, TC-06, TC-07, TC-09, TC-12, TC-14 a TC-16, TC-18). */
@TesteDeReservaApi
class AuditoriaApiTest extends BaseDaApiDeReserva {

    private long criaPelaApi() throws Exception {
        String resposta = ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private long responsavel() {
        return IdentidadesDeTeste.garanteUsuario(Perfil.RESPONSAVEL);
    }

    @Test
    @DisplayName("TC-01 e TC-12: criar a reserva pela API grava 1 registro: anterior vazio, novo SOLICITADA, dono e instante do Clock")
    void criacaoGravaORegistro() throws Exception {
        long id = criaPelaApi();

        List<Map<String, Object>> registros = ambiente.auditoria(id);
        assertThat(registros).as("registros").hasSize(1);
        assertThat(registros.get(0).get("estado_anterior")).isNull();
        assertThat(registros.get(0).get("estado_novo")).isEqualTo("SOLICITADA");
        assertThat(((Number) registros.get(0).get("usuario_id")).longValue()).isEqualTo(dono);
        assertThat(((java.sql.Timestamp) registros.get(0).get("ocorrida_em")).toInstant()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("TC-02 (RN-09-T2): o Responsável aprova pela API: 1 registro SOLICITADA para APROVADA com o Responsável")
    void aprovacaoGravaORegistro() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isOk());

        List<Map<String, Object>> registros = ambiente.auditoria(id);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).get("estado_anterior")).isEqualTo("SOLICITADA");
        assertThat(registros.get(0).get("estado_novo")).isEqualTo("APROVADA");
        assertThat(((Number) registros.get(0).get("usuario_id")).longValue()).isEqualTo(responsavel());
    }

    @Test
    @DisplayName("TC-06: o dono altera uma APROVADA pela API: 1 registro APROVADA para SOLICITADA")
    void alteracaoDeAprovada() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(HorariosDeTeste.quarta(10, 0),
                HorariosDeTeste.quarta(11, 0), sala)).andExpect(status().isOk());

        assertThat(ambiente.auditoria(id)).hasSize(1);
        assertThat(ambiente.auditoria(id).get(0).get("motivo")).isEqualTo("alteração da reserva aprovada");
    }

    @Test
    @DisplayName("TC-07: o dono altera o horário de uma SOLICITADA pela API: nenhum registro")
    void alteracaoDeSolicitada() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(HorariosDeTeste.quarta(10, 0),
                HorariosDeTeste.quarta(11, 0), sala)).andExpect(status().isOk());

        assertThat(ambiente.auditoria(id)).isEmpty();
    }

    @Test
    @DisplayName("TC-09: um pedido recusado por conflito de horário não grava nenhum registro")
    void pedidoRecusadoNaoGrava() throws Exception {
        ambiente.reservaGravada(ambiente.outroSolicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                AGORA, sala);

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().isConflict());

        assertThat(ambiente.totalDeAuditoria()).isZero();
    }

    @ParameterizedTest(name = "TC-14: {0} em /api/reservas/1/auditoria devolve 403")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("TC-14: não existe endpoint de escrita na auditoria: 403 com token e 401 sem token")
    void naoHaEscritaNaAuditoria(String metodo) throws Exception {
        HttpMethod http = HttpMethod.valueOf(metodo);

        ambiente.chama(http, "/api/reservas/1/auditoria", ambiente.bearerDe(Perfil.ADMINISTRADOR), "{}")
                .andExpect(status().isForbidden());
        ambiente.chama(http, "/api/reservas/1/auditoria", null, "{}").andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "TC-15: {0} consulta o histórico")
    @EnumSource(value = Perfil.class, names = {"RESPONSAVEL", "ADMINISTRADOR"})
    @DisplayName("TC-15: o Responsável e o Administrador consultam o histórico: 200 com os registros em ordem")
    void consultaDoHistorico(Perfil perfil) throws Exception {
        long id = criaPelaApi();
        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isOk());

        ambiente.chama(HttpMethod.GET, "/api/reservas/" + id + "/auditoria", ambiente.bearerDe(perfil), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estadoNovo").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].estadoNovo").value("APROVADA"));
    }

    @Test
    @DisplayName("TC-16: o Solicitante dono recebe 403, sem token 401 e uma reserva inexistente 404")
    void acessoAoHistorico() throws Exception {
        long id = criaPelaApi();

        ambiente.chama(HttpMethod.GET, "/api/reservas/" + id + "/auditoria", ambiente.bearerDoSolicitante(), null)
                .andExpect(status().isForbidden());
        ambiente.chama(HttpMethod.GET, "/api/reservas/" + id + "/auditoria", null, null)
                .andExpect(status().isUnauthorized());
        ambiente.chama(HttpMethod.GET, "/api/reservas/999999/auditoria", ambiente.bearerDe(Perfil.ADMINISTRADOR), null)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-18: reserva criada, aprovada e cancelada: o histórico tem 3 registros em ordem, com os usuários certos")
    void historicoDeUmaReservaCancelada() throws Exception {
        long id = criaPelaApi();
        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA").andExpect(status().isOk());
        ambiente.cancela(ambiente.bearerDoSolicitante(), id).andExpect(status().isOk());

        String corpo = ambiente.chama(HttpMethod.GET, "/api/reservas/" + id + "/auditoria",
                ambiente.bearerDe(Perfil.RESPONSAVEL), null).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();

        List<String> estados = JsonPath.read(corpo, "$[*].estadoNovo");
        List<Integer> usuarios = JsonPath.read(corpo, "$[*].usuarioId");
        assertThat(estados).containsExactly("SOLICITADA", "APROVADA", "CANCELADA");
        assertThat(usuarios).containsExactly((int) dono, (int) responsavel(), (int) dono);
    }
}
