package br.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Spec T-44, CA-11 e CA-12: fila de aprovação, retirada e devolução, e auditoria visível só a quem pode. */
@TesteDeTelas
@DisplayName("Telas do Responsável")
class TelasDoResponsavelTest extends BaseDasTelas {

    private long sala;
    private long material;

    @BeforeEach
    void recursosDaTela() {
        sala = sala("sala-102", 30, "Bloco B");
        material = material("Projetor", "PAT-1");
    }

    @Test
    @DisplayName("TC-20: o Responsável aprova uma e rejeita outra, e a fila esvazia")
    void filaDeAprovacao() throws Exception {
        long primeira = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala);
        long segunda = pedido("2026-11-10T14:00", "2026-11-10T15:00", "SALA:" + sala);
        mvc.perform(get("/aprovacoes").with(comoResponsavel())).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"reserva-" + primeira + "\"")))
                .andExpect(content().string(containsString("id=\"reserva-" + segunda + "\"")));
        mvc.perform(post("/reservas/" + primeira + "/aprovar").with(comoResponsavel()).with(csrf()))
                .andExpect(status().isFound());
        mvc.perform(post("/reservas/" + segunda + "/rejeitar").with(comoResponsavel()).with(csrf()))
                .andExpect(status().isFound());
        assertThat(estadoDa(primeira)).isEqualTo("APROVADA");
        assertThat(estadoDa(segunda)).isEqualTo("REJEITADA");
        mvc.perform(get("/aprovacoes").with(comoResponsavel()))
                .andExpect(content().string(not(containsString("id=\"reserva-" + primeira + "\""))))
                .andExpect(content().string(not(containsString("id=\"reserva-" + segunda + "\""))));
    }

    @Test
    @DisplayName("TC-21: Solicitante e Administrador não acessam a fila nem aprovam")
    void soOResponsavelAprova() throws Exception {
        long reserva = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala);
        for (RequestPostProcessor quem : new RequestPostProcessor[] {comoSolicitante(), comoAdministrador()}) {
            mvc.perform(get("/aprovacoes").with(quem)).andExpect(status().isForbidden());
            mvc.perform(post("/reservas/" + reserva + "/aprovar").with(quem).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/reservas/" + reserva + "/rejeitar").with(quem).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        assertThat(estadoDa(reserva)).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("TC-22: retirada e devolução aparecem na reserva, com o atraso quando houver")
    void retiradaEDevolucao() throws Exception {
        long reserva = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala, "MATERIAL:" + material);
        mvc.perform(post("/reservas/" + reserva + "/aprovar").with(comoResponsavel()).with(csrf()));
        mvc.perform(post("/reservas/" + reserva + "/retiradas").with(comoResponsavel()).with(csrf())
                .param("materialId", String.valueOf(material))).andExpect(status().isFound());
        mvc.perform(get("/reservas/" + reserva).with(comoResponsavel())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Projetor")))
                .andExpect(content().string(containsString("Retirado em")))
                .andExpect(content().string(containsString("Devolver")));
        long retirada = maiorId("retirada");
        mvc.perform(post("/reservas/" + reserva + "/retiradas/" + retirada + "/devolucao").with(comoResponsavel())
                .with(csrf())).andExpect(status().isFound());
        mvc.perform(get("/reservas/" + reserva).with(comoResponsavel()))
                .andExpect(content().string(containsString("Devolvido em")));
        assertThat(jdbc.queryForList("SELECT devolvida_em IS NOT NULL FROM retirada WHERE id = ?", Boolean.class,
                retirada)).containsExactly(true);
    }

    @Test
    @DisplayName("TC-22: retirar um material que não está na reserva mostra a regra, sem erro técnico")
    void retiradaInvalida() throws Exception {
        long reserva = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala);
        mvc.perform(post("/reservas/" + reserva + "/aprovar").with(comoResponsavel()).with(csrf()));
        mvc.perform(post("/reservas/" + reserva + "/retiradas").with(comoResponsavel()).with(csrf())
                .param("materialId", String.valueOf(material))).andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("não faz parte desta reserva")));
    }

    @Test
    @DisplayName("TC-23: a auditoria aparece para o Responsável e o Administrador, e não para o Solicitante")
    void auditoriaPorPerfil() throws Exception {
        long reserva = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala);
        mvc.perform(post("/reservas/" + reserva + "/aprovar").with(comoResponsavel()).with(csrf()));
        mvc.perform(get("/reservas/" + reserva).with(comoResponsavel()))
                .andExpect(content().string(containsString("Histórico de mudanças")))
                .andExpect(content().string(containsString("Aprovada")));
        mvc.perform(get("/reservas/" + reserva).with(comoAdministrador()))
                .andExpect(content().string(containsString("Histórico de mudanças")));
        mvc.perform(get("/reservas/" + reserva).with(comoSolicitante()))
                .andExpect(content().string(not(containsString("Histórico de mudanças"))));
    }

    @Test
    @DisplayName("O Responsável e o Administrador veem todas as reservas, com filtro de estado")
    void listaDeTodas() throws Exception {
        long deMarina = pedido("2026-11-10T10:00", "2026-11-10T11:00", "SALA:" + sala);
        mvc.perform(post("/reservas").with(comoOutroSolicitante()).with(csrf()).param("inicio", "2026-11-10T14:00")
                .param("termino", "2026-11-10T15:00").param("recursos", "SALA:" + sala));
        long deRafael = ultimaReserva();
        mvc.perform(post("/reservas/" + deRafael + "/aprovar").with(comoResponsavel()).with(csrf()));
        for (RequestPostProcessor quem : new RequestPostProcessor[] {comoResponsavel(), comoAdministrador()}) {
            mvc.perform(get("/reservas").with(quem)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("id=\"reserva-" + deMarina + "\"")))
                    .andExpect(content().string(containsString("id=\"reserva-" + deRafael + "\"")));
            mvc.perform(get("/reservas").with(quem).param("estado", "APROVADA"))
                    .andExpect(content().string(not(containsString("id=\"reserva-" + deMarina + "\""))))
                    .andExpect(content().string(containsString("id=\"reserva-" + deRafael + "\"")));
        }
    }

    private long pedido(String inicio, String termino, String... recursos) throws Exception {
        var req = post("/reservas").with(comoSolicitante()).with(csrf()).param("inicio", inicio)
                .param("termino", termino);
        req.param("recursos", recursos);
        mvc.perform(req);
        return ultimaReserva();
    }
}
