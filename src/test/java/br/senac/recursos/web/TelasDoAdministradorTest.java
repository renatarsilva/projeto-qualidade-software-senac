package br.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Spec T-44, CA-13 e CA-14: cadastros, usuários, manutenção e relatório. */
@TesteDeTelas
@DisplayName("Telas do Administrador")
class TelasDoAdministradorTest extends BaseDasTelas {

    @Test
    @DisplayName("TC-24: o Administrador cadastra sala, material, professor e usuário, e eles aparecem nas listas")
    void cadastros() throws Exception {
        mvc.perform(post("/admin/salas").with(comoAdministrador()).with(csrf()).param("nome", "sala-201")
                .param("tipoSala", "Laboratório").param("capacidade", "25").param("localizacao", "Bloco C")
                .param("restrito", "true")).andExpect(status().isFound());
        mvc.perform(post("/admin/materiais").with(comoAdministrador()).with(csrf()).param("nome", "Notebook")
                .param("tipoMaterial", "Computador").param("patrimonio", "PAT-9")).andExpect(status().isFound());
        mvc.perform(post("/admin/professores").with(comoAdministrador()).with(csrf())
                .param("nome", "maria-souza-12345").param("competencias", "Java, Banco de Dados"))
                .andExpect(status().isFound());
        mvc.perform(post("/admin/usuarios").with(comoAdministrador()).with(csrf()).param("nome", "Nova Pessoa")
                .param("email", "nova" + SUFIXO).param("senha", SENHA).param("perfil", "SOLICITANTE"))
                .andExpect(status().isFound());
        mvc.perform(get("/admin/salas").with(comoAdministrador())).andExpect(content().string(containsString("sala-201")));
        mvc.perform(get("/admin/materiais").with(comoAdministrador()))
                .andExpect(content().string(containsString("Notebook")));
        mvc.perform(get("/admin/professores").with(comoAdministrador()))
                .andExpect(content().string(containsString("maria-souza-12345")))
                .andExpect(content().string(containsString("Banco de Dados")));
        mvc.perform(get("/admin/usuarios").with(comoAdministrador()))
                .andExpect(content().string(containsString("nova" + SUFIXO)));
    }

    @Test
    @DisplayName("TC-25: nome fora do padrão, repetido e senha curta mostram mensagem clara e não gravam")
    void validacaoDosCadastros() throws Exception {
        mvc.perform(post("/admin/salas").with(comoAdministrador()).with(csrf()).param("nome", "sala x")
                .param("tipoSala", "Laboratório").param("capacidade", "25").param("localizacao", "Bloco C"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("sala-NN")))
                .andExpect(content().string(containsString("value=\"sala x\"")));
        sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/admin/salas").with(comoAdministrador()).with(csrf()).param("nome", "sala-102")
                .param("tipoSala", "Laboratório").param("capacidade", "25").param("localizacao", "Bloco C"))
                .andExpect(content().string(containsString("Já existe uma sala com esse nome")));
        mvc.perform(post("/admin/usuarios").with(comoAdministrador()).with(csrf()).param("nome", "Outra Pessoa")
                .param("email", "outra" + SUFIXO).param("senha", "curta").param("perfil", "SOLICITANTE"))
                .andExpect(content().string(containsString("A senha deve ter de 8 a 72")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sala", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario WHERE email = ?", Integer.class,
                "outra" + SUFIXO)).isZero();
    }

    @Test
    @DisplayName("TC-26: desativar o único administrador é recusado com a mensagem da regra")
    void unicoAdministrador() throws Exception {
        jdbc.update("UPDATE usuario SET ativo = false WHERE perfil = 'ADMINISTRADOR' AND id <> ?", administrador);
        mvc.perform(post("/admin/usuarios/" + administrador + "/ativo").with(comoAdministrador()).with(csrf())
                .param("ativo", "false")).andExpect(status().isOk())
                .andExpect(content().string(containsString("pelo menos um administrador")));
        assertThat(jdbc.queryForObject("SELECT ativo FROM usuario WHERE id = ?", Boolean.class, administrador)).isTrue();
        jdbc.update("UPDATE usuario SET ativo = true WHERE perfil = 'ADMINISTRADOR'");
    }

    @Test
    @DisplayName("TC-27: ativar, desativar e trocar o perfil de um usuário")
    void gestaoDeUsuarios() throws Exception {
        mvc.perform(post("/admin/usuarios/" + solicitante + "/ativo").with(comoAdministrador()).with(csrf())
                .param("ativo", "false")).andExpect(status().isFound());
        assertThat(jdbc.queryForObject("SELECT ativo FROM usuario WHERE id = ?", Boolean.class, solicitante)).isFalse();
        mvc.perform(post("/admin/usuarios/" + solicitante + "/ativo").with(comoAdministrador()).with(csrf())
                .param("ativo", "true")).andExpect(status().isFound());
        mvc.perform(post("/admin/usuarios/" + solicitante + "/perfil").with(comoAdministrador()).with(csrf())
                .param("perfil", "RESPONSAVEL")).andExpect(status().isFound());
        assertThat(jdbc.queryForObject("SELECT perfil FROM usuario WHERE id = ?", String.class, solicitante))
                .isEqualTo(Perfil.RESPONSAVEL.name());
    }

    @Test
    @DisplayName("TC-28: a manutenção lista as reservas afetadas e o encerramento libera o recurso")
    void manutencao() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/reservas").with(comoSolicitante()).with(csrf()).param("inicio", "2026-11-10T10:00")
                .param("termino", "2026-11-10T11:00").param("recursos", "SALA:" + sala));
        long reserva = ultimaReserva();
        mvc.perform(post("/admin/manutencoes").with(comoAdministrador()).with(csrf())
                .param("recursoId", String.valueOf(sala)).param("inicio", "2026-11-10T09:00")
                .param("termino", "2026-11-10T12:00").param("motivo", "Troca do projetor"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/manutencoes?recurso=" + sala + "&afetadas=" + reserva));
        mvc.perform(get("/admin/manutencoes").with(comoAdministrador()).param("recurso", String.valueOf(sala))
                .param("afetadas", String.valueOf(reserva)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Troca do projetor")))
                .andExpect(content().string(containsString("reserva #" + reserva)));
        long manutencao = maiorId("periodo_manutencao");
        mvc.perform(post("/admin/manutencoes/" + manutencao + "/encerrar").with(comoAdministrador()).with(csrf())
                .param("recursoId", String.valueOf(sala))).andExpect(status().isFound());
        mvc.perform(get("/admin/manutencoes").with(comoAdministrador()).param("recurso", String.valueOf(sala)))
                .andExpect(content().string(not(containsString("Troca do projetor"))));
    }

    @Test
    @DisplayName("TC-28: manutenção sem motivo mostra a mensagem clara")
    void manutencaoInvalida() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/admin/manutencoes").with(comoAdministrador()).with(csrf())
                .param("recursoId", String.valueOf(sala)).param("inicio", "2026-11-10T09:00")
                .param("termino", "2026-11-10T12:00").param("motivo", ""))
                .andExpect(status().isOk()).andExpect(content().string(containsString("alert-danger")))
                .andExpect(content().string(containsString("Motivo")));
    }

    @Test
    @DisplayName("TC-29: o relatório mostra 1,00 hora da sala e 1 conflito evitado")
    void relatorio() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/reservas").with(comoSolicitante()).with(csrf()).param("inicio", "2026-11-10T10:00")
                .param("termino", "2026-11-10T11:00").param("recursos", "SALA:" + sala));
        mvc.perform(post("/reservas").with(comoOutroSolicitante()).with(csrf()).param("inicio", "2026-11-10T10:30")
                .param("termino", "2026-11-10T11:30").param("recursos", "SALA:" + sala));
        for (var quem : new org.springframework.test.web.servlet.request.RequestPostProcessor[] {comoResponsavel(),
                comoAdministrador()}) {
            mvc.perform(get("/relatorio").with(quem).param("inicio", "2026-11-01").param("fim", "2026-11-30"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("sala-102")))
                    .andExpect(content().string(containsString("1,00")))
                    .andExpect(content().string(containsString("Conflitos evitados: 1")));
        }
    }

    @Test
    @DisplayName("TC-30: relatório com fim antes do início mostra a mensagem clara")
    void relatorioInvalido() throws Exception {
        mvc.perform(get("/relatorio").with(comoAdministrador()).param("inicio", "2026-11-30")
                .param("fim", "2026-11-01")).andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")));
    }
}
