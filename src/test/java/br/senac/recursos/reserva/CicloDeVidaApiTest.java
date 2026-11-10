package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contrato do PATCH, do cancelamento e do DELETE (spec T-21, seção 4; TC-20, TC-25, TC-40 a TC-44, TC-56, TC-57, TC-61). */
@TesteDeReservaApi
class CicloDeVidaApiTest extends BaseDaApiDeReserva {

    @Test
    @DisplayName("TC-40: o Responsável aprova uma SOLICITADA no prazo e recebe 200 com a reserva APROVADA")
    void responsavelAprova() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROVADA"));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("TC-41: o Responsável pede APROVADA numa reserva CANCELADA e recebe 409")
    void aprovarCanceladaDevolve409() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.CANCELADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA")
                .andExpect(status().isConflict());
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("TC-42: o Responsável envia \"ABC\" e recebe 400 com o erro no campo estado")
    void estadoInexistenteDevolve400() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "ABC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("estado"));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("TC-43: o dono chama DELETE de uma reserva existente e recebe 409; a reserva continua")
    void apagarDevolve409() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.apaga(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Reservas não podem ser apagadas. Use o cancelamento."));
        assertThat(ambiente.reservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-20 (RN-13-T2): depois do cancelamento, outro usuário reserva a mesma sala e horário")
    void cancelamentoLiberaOHorarioParaOutroUsuario() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.cancela(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        ambiente.cria(ambiente.bearerDoOutro(),
                AmbienteDeReserva.corpo(terca(10, 0), terca(11, 0), sala)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-25 (RN-13-T7): depois do encerramento antecipado, outro usuário reserva o resto do horário")
    void encerramentoLiberaORestoDoHorario() throws Exception {
        // A spec cita 10:00-12:00 e o pedido das 11:00; a RN-15 (1 h de antecedência) recusaria o das 11:00 às 10:40.
        long id = ambiente.reservaGravada(dono, EstadoDaReserva.EM_USO, terca(10, 0), terca(14, 0),
                HorariosDeTeste.AGORA, sala);
        relogio.defineAgora(terca(10, 40));

        ambiente.cancela(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONCLUIDA"));

        ambiente.cria(ambiente.bearerDoOutro(),
                AmbienteDeReserva.corpo(terca(12, 0), terca(13, 0), sala)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-44: o dono cancela às 10:05 uma APROVADA sem check-in; vira NAO_COMPARECEU e o resto fica livre")
    void cancelarSemCheckinDepoisDoInicio() throws Exception {
        long id = ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(14, 0),
                HorariosDeTeste.AGORA, sala);
        relogio.defineAgora(terca(10, 5));

        ambiente.cancela(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NAO_COMPARECEU"));

        ambiente.cria(ambiente.bearerDoOutro(),
                AmbienteDeReserva.corpo(terca(12, 0), terca(13, 0), sala)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-56: o cancelamento por quem não é o dono devolve 403 e a reserva fica intacta")
    void naoDonoNaoCancela() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.cancela(ambiente.bearerDoOutro(), id).andExpect(status().isForbidden());
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("TC-57: o cancelamento de uma reserva que não existe devolve 404")
    void cancelarReservaInexistente() throws Exception {
        ambiente.cancela(ambiente.bearerDoSolicitante(), 999999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("TC-61: o cancelamento de uma APROVADA sem check-in, depois do início, devolve 200 NAO_COMPARECEU")
    void cancelamentoSemCheckinDevolveNaoCompareceu() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);
        relogio.defineAgora(terca(10, 30));

        ambiente.cancela(ambiente.bearerDoSolicitante(), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NAO_COMPARECEU"));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("NAO_COMPARECEU");
    }
}
