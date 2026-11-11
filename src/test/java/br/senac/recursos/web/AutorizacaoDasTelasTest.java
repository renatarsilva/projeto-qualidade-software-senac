package br.senac.recursos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Spec T-44, CA-05 e CA-06: a matriz de autorização das telas (rota × perfil), o negado por padrão e o CSRF. É o
 * espelho, para as telas, do teste da matriz da API (T-49): uma rota nova sem linha aqui quebra o teste de cobertura.
 */
@TesteDeTelas
@DisplayName("Telas: autorização por perfil e CSRF")
class AutorizacaoDasTelasTest extends BaseDasTelas {

    private static final Perfil S = Perfil.SOLICITANTE;
    private static final Perfil R = Perfil.RESPONSAVEL;
    private static final Perfil A = Perfil.ADMINISTRADOR;

    /** Cada linha: método, rota e os perfis que podem usar. Um id que não existe dá 404, nunca 403, a quem pode. */
    static Stream<Arguments> matriz() {
        return Stream.of(
                linha(HttpMethod.GET, "/", S, R, A),
                linha(HttpMethod.GET, "/recursos", S, R, A),
                linha(HttpMethod.GET, "/reservas", S, R, A),
                linha(HttpMethod.GET, "/reservas/999999", S, R, A),
                linha(HttpMethod.GET, "/reservas/nova", S),
                linha(HttpMethod.POST, "/reservas", S),
                linha(HttpMethod.GET, "/reservas/999999/editar", S),
                linha(HttpMethod.POST, "/reservas/999999/editar", S),
                linha(HttpMethod.POST, "/reservas/999999/cancelar", S),
                linha(HttpMethod.POST, "/reservas/999999/checkin", S),
                linha(HttpMethod.GET, "/aprovacoes", R),
                linha(HttpMethod.POST, "/reservas/999999/aprovar", R),
                linha(HttpMethod.POST, "/reservas/999999/rejeitar", R),
                linha(HttpMethod.POST, "/reservas/999999/retiradas", R),
                linha(HttpMethod.POST, "/reservas/999999/retiradas/1/devolucao", R),
                linha(HttpMethod.GET, "/relatorio", R, A),
                linha(HttpMethod.GET, "/admin/salas", A),
                linha(HttpMethod.POST, "/admin/salas", A),
                linha(HttpMethod.GET, "/admin/materiais", A),
                linha(HttpMethod.POST, "/admin/materiais", A),
                linha(HttpMethod.GET, "/admin/professores", A),
                linha(HttpMethod.POST, "/admin/professores", A),
                linha(HttpMethod.GET, "/admin/usuarios", A),
                linha(HttpMethod.POST, "/admin/usuarios", A),
                linha(HttpMethod.POST, "/admin/usuarios/1/ativo", A),
                linha(HttpMethod.POST, "/admin/usuarios/1/perfil", A),
                linha(HttpMethod.GET, "/admin/manutencoes", A),
                linha(HttpMethod.POST, "/admin/manutencoes", A),
                linha(HttpMethod.POST, "/admin/manutencoes/1/encerrar", A));
    }

    private static Arguments linha(HttpMethod metodo, String rota, Perfil... permitidos) {
        return Arguments.of(metodo, rota, List.of(permitidos));
    }

    @ParameterizedTest(name = "TC-08: {0} {1}")
    @MethodSource("matriz")
    @DisplayName("TC-08: cada rota só abre para os perfis dela")
    void matrizDeAutorizacao(HttpMethod metodo, String rota, List<Perfil> permitidos) throws Exception {
        for (Perfil perfil : Perfil.values()) {
            long id = switch (perfil) {
                case SOLICITANTE -> solicitante;
                case RESPONSAVEL -> responsavel;
                case ADMINISTRADOR -> administrador;
            };
            int codigo = mvc.perform(pedido(metodo, rota).with(como(id, perfil))).andReturn().getResponse().getStatus();
            if (permitidos.contains(perfil)) {
                assertThat(codigo).as("%s %s para %s deveria abrir", metodo, rota, perfil).isNotIn(302, 401, 403);
            } else {
                assertThat(codigo).as("%s %s para %s deveria ser recusada", metodo, rota, perfil).isEqualTo(403);
            }
        }
        // Anônimo: GET vai para o login; POST sem token e sem sessão também não passa.
        int anonimo = mvc.perform(pedido(metodo, rota)).andReturn().getResponse().getStatus();
        assertThat(anonimo).as("%s %s sem sessão", metodo, rota).isIn(302, 403);
    }

    @ParameterizedTest(name = "TC-10: rota sem regra para {0}")
    @org.junit.jupiter.params.provider.EnumSource(Perfil.class)
    @DisplayName("TC-10: o que nenhuma regra liberou é recusado, até para o Administrador")
    void negadoPorPadrao(Perfil perfil) throws Exception {
        long id = perfil == S ? solicitante : perfil == R ? responsavel : administrador;
        mvc.perform(get("/rota-que-nao-existe").with(como(id, perfil))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/outra-coisa").with(como(id, perfil))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-09: o Solicitante não cadastra sala nem aprova, e nada é gravado")
    void solicitanteNaoExecutaOutrosPerfis() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/admin/salas").with(comoSolicitante()).with(csrf())
                .param("nome", "sala-103").param("tipoSala", "Lab").param("capacidade", "20")
                .param("localizacao", "Bloco C")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sala", Integer.class)).isEqualTo(1);

        mvc.perform(post("/reservas").with(comoSolicitante()).with(csrf()).param("inicio", "2026-11-10T10:00")
                .param("termino", "2026-11-10T11:00").param("recursos", "SALA:" + sala));
        long reserva = ultimaReserva();
        mvc.perform(post("/reservas/" + reserva + "/aprovar").with(comoSolicitante()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/reservas/" + reserva + "/aprovar").with(comoAdministrador()).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(estadoDa(reserva)).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("TC-11: POST sem o token CSRF é recusado e nada muda")
    void semCsrf() throws Exception {
        long sala = sala("sala-102", 30, "Bloco B");
        mvc.perform(post("/reservas").with(comoSolicitante()).param("inicio", "2026-11-10T10:00")
                .param("termino", "2026-11-10T11:00").param("recursos", "SALA:" + sala))
                .andExpect(status().isForbidden());
        assertThat(totalDeReservas()).isZero();
    }

    private static MockHttpServletRequestBuilder pedido(HttpMethod metodo, String rota) {
        return metodo == HttpMethod.GET ? get(rota) : post(rota).with(csrf());
    }
}
