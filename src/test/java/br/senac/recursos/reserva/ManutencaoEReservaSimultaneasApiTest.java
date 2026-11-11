package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Manutenção e reserva do mesmo recurso ao mesmo tempo (spec T-35, CA-12; TC-18; CEN-04). */
@TesteDeReservaApi
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
class ManutencaoEReservaSimultaneasApiTest extends BaseDeConcorrencia {

    @Test
    @DisplayName("TC-18 (RN-04-T10): em 15 rodadas, toda reserva que existe no período da manutenção consta em reservasAfetadas")
    void nuncaExistemAsDuasSemAviso() throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(1);
        String bearerDoSolicitante = ambiente.bearerDoUsuario(ids.get(0));

        for (int rodada = 1; rodada <= 15; rodada++) {
            ambiente.limpaReservas();
            jdbc.update("DELETE FROM periodo_manutencao");

            List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                    pedido(0, bearerDoSolicitante, corpo(inicio, fim, sala)),
                    () -> {
                        var resposta = ambiente.manutencao(sala, inicio, fim, "Manutenção").andReturn().getResponse();
                        return new DisparoSimultaneo.Resposta(1, resposta.getStatus(), resposta.getContentAsString());
                    }));

            DisparoSimultaneo.Resposta manutencao = respostas.get(1);
            assertThat(manutencao.status()).as("rodada %d: manutenção", rodada).isEqualTo(201);
            List<Integer> afetadas = JsonPath.read(manutencao.corpo(), "$.reservasAfetadas");
            List<Long> existentes = jdbc.queryForList("SELECT id FROM reserva", Long.class);
            assertThat(existentes).as("rodada %d: reservas existentes no período", rodada)
                    .allSatisfy(id -> assertThat(afetadas).contains(id.intValue()));
        }
    }
}
