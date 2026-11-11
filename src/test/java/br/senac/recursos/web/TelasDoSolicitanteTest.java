package br.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** Spec T-44, CA-07 a CA-10: pesquisa, criação, erros de regra em linguagem clara e gestão da própria reserva. */
@TesteDeTelas
@DisplayName("Telas do Solicitante")
class TelasDoSolicitanteTest extends BaseDasTelas {

    private long sala;

    @BeforeEach
    void salaDaTela() {
        sala = sala("sala-102", 30, "Bloco B");
    }

    @Test
    @DisplayName("TC-12: a pesquisa por tipo, capacidade e intervalo livre lista só o que atende")
    void pesquisa() throws Exception {
        sala("sala-103", 10, "Anexo");
        material("Projetor", "PAT-1");
        mvc.perform(get("/recursos").with(comoSolicitante()).param("tipo", "SALA").param("capacidadeMinima", "30")
                .param("de", "2026-11-10T10:00").param("ate", "2026-11-10T11:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("sala-102")))
                .andExpect(content().string(not(containsString("sala-103"))))
                .andExpect(content().string(not(containsString("Projetor"))));
    }

    @Test
    @DisplayName("TC-13: a sala ocupada some da pesquisa por disponibilidade")
    void pesquisaNaoMostraOcupada() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        mvc.perform(get("/recursos").with(comoSolicitante()).param("de", "2026-11-10T10:30")
                .param("ate", "2026-11-10T11:30")).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sala-102"))));
        mvc.perform(get("/recursos").with(comoSolicitante()).param("de", "2026-11-10T11:00")
                .param("ate", "2026-11-10T12:00")).andExpect(content().string(containsString("sala-102")));
    }

    @Test
    @DisplayName("TC-14: criar a reserva pelo formulário a grava SOLICITADA e a mostra em minhas reservas")
    void criaReserva() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00").andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("/reservas/*"));
        long id = ultimaReserva();
        assertThat(estadoDa(id)).isEqualTo("SOLICITADA");
        mvc.perform(get("/reservas").with(comoSolicitante())).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"reserva-" + id + "\"")))
                .andExpect(content().string(containsString("Solicitada")));
    }

    @Test
    @DisplayName("TC-15: o horário digitado é o de Brasília: 10:00 vira 13:00 UTC e volta como 10:00")
    void fusoHorario() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        long id = ultimaReserva();
        List<Timestamp> inicios = jdbc.queryForList("SELECT inicio FROM reserva WHERE id = ?", Timestamp.class, id);
        assertThat(inicios).extracting(Timestamp::toInstant).containsExactly(Instant.parse("2026-11-10T13:00:00Z"));
        mvc.perform(get("/reservas/" + id).with(comoSolicitante())).andExpect(status().isOk())
                .andExpect(content().string(containsString("10/11/2026 10:00")));
    }

    @Test
    @DisplayName("TC-16: sala ocupada mostra a mensagem da regra, sem detalhe interno, e mantém os dados digitados")
    void conflitoMostraMensagemClara() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        reserva(comoOutroSolicitante(), "2026-11-10T10:30", "2026-11-10T11:30").andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("já está reservada")))
                .andExpect(content().string(containsString("value=\"2026-11-10T10:30\"")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("br.senac"))));
        assertThat(totalDeReservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-17: término igual ao início e pedido sem antecedência têm mensagem clara")
    void validacaoEAntecedencia() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T10:00").andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("posterior")));
        // Relógio fixo: segunda 05:00 em Brasília. Menos de 1 hora de antecedência é recusado (RN-15).
        reserva(comoSolicitante(), "2026-11-09T05:30", "2026-11-09T06:30").andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("antecedência")));
        assertThat(totalDeReservas()).isZero();
    }

    @Test
    @DisplayName("TC-18: o dono abre, altera, faz o check-in e cancela")
    void donoGerenciaAReserva() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        long id = ultimaReserva();
        mvc.perform(get("/reservas/" + id).with(comoSolicitante())).andExpect(status().isOk());
        mvc.perform(get("/reservas/" + id + "/editar").with(comoSolicitante())).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2026-11-10T10:00\"")));
        mvc.perform(post("/reservas/" + id + "/editar").with(comoSolicitante()).with(csrf())
                .param("inicio", "2026-11-10T14:00").param("termino", "2026-11-10T15:00")
                .param("recursos", "SALA:" + sala)).andExpect(status().isFound());
        mvc.perform(get("/reservas/" + id).with(comoSolicitante()))
                .andExpect(content().string(containsString("10/11/2026 14:00")));
        mvc.perform(post("/reservas/" + id + "/cancelar").with(comoSolicitante()).with(csrf()))
                .andExpect(status().isFound());
        assertThat(estadoDa(id)).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("TC-18: o check-in fora da janela é recusado com a mensagem da regra")
    void checkin() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        long id = ultimaReserva();
        mvc.perform(post("/reservas/" + id + "/aprovar").with(comoResponsavel()).with(csrf()))
                .andExpect(status().isFound());
        // O relógio do teste está antes do início: o check-in é recusado pela regra e a tela mostra a mensagem.
        mvc.perform(post("/reservas/" + id + "/checkin").with(comoSolicitante()).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("alert-danger")));
        assertThat(estadoDa(id)).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("TC-19: outro Solicitante não vê nem cancela a reserva alheia")
    void outroSolicitanteRecebe403() throws Exception {
        reserva(comoSolicitante(), "2026-11-10T10:00", "2026-11-10T11:00");
        long id = ultimaReserva();
        mvc.perform(get("/reservas/" + id).with(comoOutroSolicitante())).andExpect(status().isForbidden());
        mvc.perform(post("/reservas/" + id + "/cancelar").with(comoOutroSolicitante()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/reservas/" + id + "/editar").with(comoOutroSolicitante()).with(csrf())
                .param("inicio", "2026-11-10T14:00").param("termino", "2026-11-10T15:00")
                .param("recursos", "SALA:" + sala)).andExpect(status().isForbidden());
        assertThat(estadoDa(id)).isEqualTo("SOLICITADA");
        mvc.perform(get("/reservas").with(comoOutroSolicitante()))
                .andExpect(content().string(not(containsString("id=\"reserva-" + id + "\""))));
    }

    @Test
    @DisplayName("A tela de nova reserva lista os recursos e pré-seleciona o que veio da pesquisa")
    void novaReservaComRecursoDaPesquisa() throws Exception {
        mvc.perform(get("/reservas/nova").with(comoSolicitante()).param("recurso", "SALA:" + sala)
                .param("inicio", "2026-11-10T10:00").param("termino", "2026-11-10T11:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"SALA:" + sala + "\" checked")))
                .andExpect(content().string(containsString("value=\"2026-11-10T10:00\"")));
    }

    @Test
    @DisplayName("Data digitada fora do formato mostra mensagem clara")
    void dataInvalida() throws Exception {
        reserva(comoSolicitante(), "amanhã", "2026-11-10T11:00").andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")));
    }

    private ResultActions reserva(org.springframework.test.web.servlet.request.RequestPostProcessor quem, String inicio,
            String termino) throws Exception {
        return mvc.perform(post("/reservas").with(quem).with(csrf()).param("inicio", inicio)
                .param("termino", termino).param("recursos", "SALA:" + sala));
    }
}
