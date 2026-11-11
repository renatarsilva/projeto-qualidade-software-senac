package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Dois PUT para o mesmo horário livre ao mesmo tempo (spec T-37, CA-09; TC-13). */
@TesteDeReservaApi
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
class AlteracaoSimultaneaConcorrenciaTest extends BaseDeConcorrencia {

    private Callable<DisparoSimultaneo.Resposta> alteracao(int indice, String bearer, long reservaId, String json) {
        return () -> {
            var resposta = ambiente.altera(bearer, reservaId, json).andReturn().getResponse();
            return new DisparoSimultaneo.Resposta(indice, resposta.getStatus(), resposta.getContentAsString());
        };
    }

    @Test
    @DisplayName("TC-13: dois donos fazem PUT para o mesmo horário livre ao mesmo tempo: 1 × 200, 1 × 409 e a recusada mantém o horário")
    void alteracoesSimultaneasParaOMesmoHorario() throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(2);
        long reservaA = ambiente.reservaGravada(ids.get(0), EstadoDaReserva.SOLICITADA, terca(10, 0), terca(11, 0),
                AGORA, sala);
        long reservaB = ambiente.reservaGravada(ids.get(1), EstadoDaReserva.SOLICITADA, terca(14, 0), terca(15, 0),
                AGORA, sala);
        String json = corpo(quarta(10, 0), quarta(11, 0), sala);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                alteracao(0, ambiente.bearerDoUsuario(ids.get(0)), reservaA, json),
                alteracao(1, ambiente.bearerDoUsuario(ids.get(1)), reservaB, json)));

        assertThat(DisparoSimultaneo.quantosCom(respostas, 200)).as("respostas 200").isEqualTo(1);
        assertThat(DisparoSimultaneo.quantosCom(respostas, 409)).as("respostas 409").isEqualTo(1);
        assertThat(ambiente.vinculosAtivosNo(sala, quarta(10, 0), quarta(11, 0))).as("ocupando quarta 10:00").isEqualTo(1);
        boolean recusadaFoiA = respostas.get(0).status() == 409;
        long recusada = recusadaFoiA ? reservaA : reservaB;
        assertThat(ambiente.inicioNoBanco(recusada)).as("horário da reserva recusada")
                .isEqualTo(recusadaFoiA ? terca(10, 0) : terca(14, 0));
    }
}
