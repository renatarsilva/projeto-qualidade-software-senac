package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** O banco protege a auditoria (spec T-40, CA-07 e CA-09; TC-13 e TC-17). */
@TesteDeReservaApi
class ImutabilidadeDaAuditoriaTest extends BaseDaApiDeReserva {

    private long reservaComRegistro() {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        jdbc.update("INSERT INTO auditoria (reserva_id, usuario_id, estado_anterior, estado_novo, ocorrida_em) "
                + "VALUES (?, ?, NULL, 'SOLICITADA', ?::timestamptz)", id, dono, AGORA.toString());
        return id;
    }

    @Test
    @DisplayName("TC-13 (RN-09-T10): o banco recusa UPDATE em um registro de auditoria")
    void bancoRecusaUpdate() {
        long id = reservaComRegistro();

        assertThatThrownBy(() -> jdbc.update("UPDATE auditoria SET motivo = 'adulterado' WHERE reserva_id = ?", id))
                .as("UPDATE na auditoria").isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("TC-13 (RN-09-T10): o banco recusa DELETE em um registro de auditoria")
    void bancoRecusaDelete() {
        long id = reservaComRegistro();

        assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria WHERE reserva_id = ?", id))
                .as("DELETE na auditoria").isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("TC-17 (RN-08-T9): o banco recusa apagar uma reserva que tem auditoria")
    void bancoRecusaApagarReservaComAuditoria() {
        long id = reservaComRegistro();
        // Sem os vínculos, só a auditoria pode impedir o DELETE da reserva.
        jdbc.update("DELETE FROM reserva_recurso WHERE reserva_id = ?", id);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM reserva WHERE id = ?", id))
                .as("DELETE da reserva com auditoria").isInstanceOf(DataAccessException.class);
    }
}
