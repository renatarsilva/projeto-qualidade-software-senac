package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.erro.TermosProibidos;
import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Entrada inválida devolve 400 com o campo e um motivo compreensível, sem vazar detalhes internos
 * (spec T-49, CA-07; TC-08 e TC-09).
 */
@TesteDeReservaApi
class EntradaInvalidaSemVazamentoApiTest extends BaseDaApiDeReserva {

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
                Arguments.of("reserva com término antes do início", HttpMethod.POST, "/api/reservas", Perfil.SOLICITANTE,
                        "termino", null),
                Arguments.of("sala com capacidade negativa", HttpMethod.POST, "/api/salas", Perfil.ADMINISTRADOR,
                        "capacidade", "{\"nome\":\"sala-150\",\"tipoSala\":\"Laboratório\",\"capacidade\":-5,"
                                + "\"localizacao\":\"Bloco B\",\"restrito\":false}"),
                Arguments.of("sala sem nome", HttpMethod.POST, "/api/salas", Perfil.ADMINISTRADOR, "nome",
                        "{\"tipoSala\":\"Laboratório\",\"capacidade\":40,\"localizacao\":\"Bloco B\","
                                + "\"restrito\":false}"),
                Arguments.of("professor sem nome", HttpMethod.POST, "/api/professores", Perfil.ADMINISTRADOR, "nome",
                        "{\"restrito\":false,\"competencias\":[\"Java\"]}"),
                Arguments.of("material sem patrimônio", HttpMethod.POST, "/api/materiais", Perfil.ADMINISTRADOR,
                        "patrimonio", "{\"nome\":\"Microfone\",\"tipoMaterial\":\"Projetor\",\"restrito\":false}"),
                Arguments.of("usuário sem e-mail", HttpMethod.POST, "/api/usuarios", Perfil.ADMINISTRADOR, "email",
                        "{\"nome\":\"Sem email\",\"senha\":\"senha-segura-123\",\"perfil\":\"SOLICITANTE\"}"));
    }

    @ParameterizedTest(name = "TC-08 e TC-09: {0} devolve 400 no campo {4}, sem vazamento")
    @MethodSource("entradasInvalidas")
    @DisplayName("TC-08 e TC-09: entrada inválida devolve 400 com o campo e um motivo, e a resposta não vaza detalhes internos")
    void entradaInvalida(String descricao, HttpMethod metodo, String caminho, Perfil perfil, String campo,
            String json) throws Exception {
        String corpoEnviado = json != null ? json : corpo(terca(11, 0), terca(10, 0), sala);
        MockHttpServletResponse resposta = ambiente.chama(metodo, caminho, ambiente.bearerDe(perfil), corpoEnviado)
                .andReturn().getResponse();
        String corpo = resposta.getContentAsString();

        assertThat(resposta.getStatus()).as("status").isEqualTo(400);
        List<String> campos = JsonPath.read(corpo, "$.erros[*].campo");
        assertThat(campos).as("campos com erro").contains(campo);
        List<String> motivos = JsonPath.read(corpo, "$.erros[*].motivo");
        assertThat(motivos).as("motivos").allSatisfy(motivo -> assertThat(motivo).isNotBlank());
        TermosProibidos.naoTem(corpo);
    }
}
