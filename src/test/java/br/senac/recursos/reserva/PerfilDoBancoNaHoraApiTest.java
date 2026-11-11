package br.senac.recursos.reserva;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/** O perfil vem do banco a cada chamada, e não do token (spec T-49, CA-09; TC-11). */
@TesteDeReservaApi
class PerfilDoBancoNaHoraApiTest extends BaseDaApiDeReserva {

    private static final String SALA_VALIDA = "{\"nome\":\"sala-150\",\"tipoSala\":\"Laboratório\",\"capacidade\":40,"
            + "\"localizacao\":\"Bloco B\",\"restrito\":false}";

    @Test
    @DisplayName("TC-11: o mesmo token recebe a resposta do perfil que o banco tem na hora; desativado, 401")
    void perfilEConferidoNoBancoACadaChamada() throws Exception {
        String token = ambiente.bearerDoSolicitante();

        ambiente.chama(HttpMethod.POST, "/api/salas", token, SALA_VALIDA).andExpect(status().isForbidden());

        jdbc.update("UPDATE usuario SET perfil = 'ADMINISTRADOR' WHERE id = ?", dono);
        ambiente.chama(HttpMethod.POST, "/api/salas", token, SALA_VALIDA).andExpect(status().isCreated());

        jdbc.update("UPDATE usuario SET perfil = ? WHERE id = ?", Perfil.SOLICITANTE.name(), dono);
        ambiente.chama(HttpMethod.POST, "/api/salas", token, SALA_VALIDA.replace("sala-150", "sala-151"))
                .andExpect(status().isForbidden());

        jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", dono);
        ambiente.chama(HttpMethod.GET, "/api/salas", token, null).andExpect(status().isUnauthorized());
    }
}
