package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Quem pede cada destino no PATCH /api/reservas/{id}/estado e a ordem dos erros
 * (spec T-21, CA-14, CA-15 e CA-17; TC-54, TC-55, TC-67 a TC-79).
 */
@TesteDeReservaApi
class AutorizacaoDoPatchApiTest extends BaseDaApiDeReserva {

    private void assertEstadoNoBanco(long id, String esperado) {
        assertThat(ambiente.estadoNoBanco(id)).as("estado da reserva").isEqualTo(esperado);
    }

    @Test
    @DisplayName("TC-54: o Solicitante pede APROVADA pelo PATCH e recebe 403")
    void solicitanteNaoAprova() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), id, "APROVADA").andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "SOLICITADA");
    }

    @Test
    @DisplayName("TC-55: o Responsável pede EM_USO pelo PATCH e recebe 403; o check-in é pelo endpoint próprio")
    void responsavelNaoPedeEmUso() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "EM_USO").andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "APROVADA");
    }

    @Test
    @DisplayName("TC-67: o Solicitante dono pede EM_USO pelo PATCH e recebe 403")
    void donoNaoPedeEmUsoPeloPatch() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), id, "EM_USO").andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "APROVADA");
    }

    @Test
    @DisplayName("TC-68: o Responsável rejeita uma SOLICITADA e recebe 200 com a reserva REJEITADA")
    void responsavelRejeita() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "REJEITADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REJEITADA"));
        assertEstadoNoBanco(id, "REJEITADA");
    }

    @Test
    @DisplayName("TC-69: o Administrador pede APROVADA numa reserva de recurso restrito e recebe 403")
    void administradorNaoAprovaRecursoRestrito() throws Exception {
        RecursoPedido restrita = ambiente.salaRestrita("sala-201");
        long id = ambiente.reservaGravada(dono, EstadoDaReserva.SOLICITADA, HorariosDeTeste.terca(10, 0),
                HorariosDeTeste.terca(11, 0), HorariosDeTeste.AGORA, restrita);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.ADMINISTRADOR), id, "APROVADA")
                .andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "SOLICITADA");
    }

    @Test
    @DisplayName("TC-70: o Administrador pede APROVADA numa reserva de recurso não restrito e recebe 403")
    void administradorNaoAprovaRecursoComum() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.ADMINISTRADOR), id, "APROVADA")
                .andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "SOLICITADA");
    }

    @Test
    @DisplayName("TC-71: o Administrador pede REJEITADA e recebe 403")
    void administradorNaoRejeita() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.ADMINISTRADOR), id, "REJEITADA")
                .andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "SOLICITADA");
    }

    @Test
    @DisplayName("TC-72: o Solicitante pede REJEITADA e recebe 403")
    void solicitanteNaoRejeita() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), id, "REJEITADA").andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "SOLICITADA");
    }

    @ParameterizedTest(name = "TC-73: o Responsável pede {0} e recebe 403")
    @ValueSource(strings = {"CANCELADA", "CONCLUIDA", "NAO_COMPARECEU", "SOLICITADA"})
    @DisplayName("TC-73: o Responsável pede um destino do sistema e recebe 403; o estado não muda")
    void responsavelNaoPedeDestinosDoSistema(String destino) throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, destino).andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "APROVADA");
    }

    @ParameterizedTest(name = "TC-74: {0} pede EM_USO e recebe 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "ADMINISTRADOR"})
    @DisplayName("TC-74: Solicitante e Administrador pedem EM_USO e recebem 403")
    void ninguemMaisPedeEmUso(Perfil perfil) throws Exception {
        long id = reservaDoDono(EstadoDaReserva.APROVADA);

        ambiente.mudaEstado(ambiente.bearerDe(perfil), id, "EM_USO").andExpect(status().isForbidden());
        assertEstadoNoBanco(id, "APROVADA");
    }

    @Test
    @DisplayName("TC-75: o Solicitante envia \"ABC\" e recebe 403, não 400")
    void perfilVemAntesDaValidacao() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), id, "ABC").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-76: o Solicitante pede o PATCH de um id que não existe e recebe 403, não 404")
    void perfilVemAntesDaExistencia() throws Exception {
        ambiente.mudaEstado(ambiente.bearerDoSolicitante(), 999999, "APROVADA").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-77: o Responsável pede o PATCH de um id que não existe e recebe 404")
    void responsavelRecebe404() throws Exception {
        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), 999999, "APROVADA")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("TC-78: o Responsável envia \"ABC\" numa reserva que existe e recebe 400 no campo estado")
    void responsavelRecebe400() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "ABC")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("estado"));
    }

    @Test
    @DisplayName("TC-79: aprovar depois de prazo_aprovacao_em devolve 409 com a mensagem do prazo")
    void aprovarDepoisDoPrazo() throws Exception {
        long id = reservaDoDono(EstadoDaReserva.SOLICITADA);
        relogio.defineAgora(HorariosDeTeste.segunda(10, 0).plusSeconds(1));

        ambiente.mudaEstado(ambiente.bearerDe(Perfil.RESPONSAVEL), id, "APROVADA")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O prazo de aprovação desta reserva terminou"));
        assertEstadoNoBanco(id, "SOLICITADA");
    }
}
