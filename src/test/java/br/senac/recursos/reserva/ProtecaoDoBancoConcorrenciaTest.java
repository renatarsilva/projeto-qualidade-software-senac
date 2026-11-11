package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.TestPropertySource;

/** O banco protege sozinho, sem passar pela aplicação (spec T-37, CA-11 e CA-13; TC-14 e TC-15). */
@TesteDeReservaApi
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
class ProtecaoDoBancoConcorrenciaTest extends BaseDeConcorrencia {

    /** O banco recusa uma inserção conflitante de duas formas: a violação do EXCLUDE ou o deadlock entre inserções que esperam uma pela outra. */
    private static final String VIOLACAO_DE_EXCLUSAO = "23P01";
    private static final String DEADLOCK = "40P01";

    private static String sqlStateDe(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof SQLException sql) {
                return sql.getSQLState();
            }
        }
        return null;
    }

    /**
     * TC-14. O TC-15 é o mesmo teste rodado com {@code -Ddupla-reserva.sem-protecao=true}, que remove o EXCLUDE só
     * do banco descartável: então mais de uma inserção passa e o teste falha por asserção (evidência do vermelho).
     */
    @Test
    @DisplayName("TC-14 e TC-15: 20 inserções diretas simultâneas, ativas, no mesmo recurso e período: 1 aceita e 19 recusadas pelo banco (23P01 ou 40P01)")
    void bancoRecusaAsDezenoveInsercoes() throws Exception {
        List<Long> usuarios = ambiente.novosSolicitantes(20);
        List<Long> reservas = new ArrayList<>();
        for (Long usuario : usuarios) {
            reservas.add(ambiente.reservaGravada(usuario, EstadoDaReserva.SOLICITADA, inicio, fim, AGORA));
        }
        String periodoInicio = OffsetDateTime.ofInstant(inicio, ZoneOffset.UTC).toString();
        String periodoFim = OffsetDateTime.ofInstant(fim, ZoneOffset.UTC).toString();

        List<Callable<DisparoSimultaneo.Resposta>> tarefas = new ArrayList<>();
        for (int i = 0; i < reservas.size(); i++) {
            int indice = i;
            long reservaId = reservas.get(i);
            tarefas.add(() -> {
                try {
                    jdbc.update("INSERT INTO reserva_recurso (reserva_id, recurso_id, periodo, ocupa) "
                            + "VALUES (?, ?, tstzrange(?::timestamptz, ?::timestamptz, '[)'), true)",
                            reservaId, sala.id(), periodoInicio, periodoFim);
                    return new DisparoSimultaneo.Resposta(indice, 201, "");
                } catch (DataAccessException erro) {
                    return new DisparoSimultaneo.Resposta(indice, 409, String.valueOf(sqlStateDe(erro)));
                }
            });
        }

        List<DisparoSimultaneo.Resposta> resultados = DisparoSimultaneo.dispara(tarefas);

        assertThat(DisparoSimultaneo.quantosCom(resultados, 201)).as("inserções aceitas").isEqualTo(1);
        assertThat(resultados.stream().filter(r -> r.status() == 409).map(DisparoSimultaneo.Resposta::corpo))
                .as("SQLState das 19 recusas (EXCLUDE ou deadlock)").hasSize(19)
                .allSatisfy(estado -> assertThat(estado).isIn(VIOLACAO_DE_EXCLUSAO, DEADLOCK));
        assertThat(ambiente.vinculosAtivosNo(sala, inicio, fim)).as("vínculos ativos no banco").isEqualTo(1);
    }
}
