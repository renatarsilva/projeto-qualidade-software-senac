package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.auditoria.AuditoriaNoBanco;
import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** A auditoria e a mudança vivem na mesma transação (spec T-40, CA-05; TC-10 e TC-11). */
@TesteDeReservaApi
class RollbackDaAuditoriaApiTest extends BaseDaApiDeReserva {

    @MockitoSpyBean
    private AuditoriaNoBanco auditoria;

    private void falhaAoGravarAuditoria() {
        doThrow(new IllegalStateException("falha simulada na auditoria")).when(auditoria)
                .registra(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-10 (RN-09-T8): a auditoria falha ao aprovar: a reserva continua SOLICITADA e não há registro")
    void falhaDaAuditoriaDesfazAAprovacao() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        falhaAoGravarAuditoria();

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA")
                .andExpect(status().is5xxServerError());

        assertThat(ambiente.estadoNoBanco(id)).as("estado da reserva").isEqualTo("SOLICITADA");
        assertThat(ambiente.totalDeAuditoria()).as("registros de auditoria").isZero();
    }

    @Test
    @DisplayName("TC-11: a auditoria falha ao criar: nenhuma reserva e nenhum registro ficam gravados")
    void falhaDaAuditoriaDesfazACriacao() throws Exception {
        falhaAoGravarAuditoria();

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 0), terca(11, 0), sala))
                .andExpect(status().is5xxServerError());

        assertThat(ambiente.reservas()).as("reservas").isZero();
        assertThat(ambiente.totalDeAuditoria()).as("registros de auditoria").isZero();
    }
}
