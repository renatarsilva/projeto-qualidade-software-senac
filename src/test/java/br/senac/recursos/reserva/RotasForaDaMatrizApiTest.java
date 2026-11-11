package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.seguranca.Perfil;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpMethod;

/** O que não está na matriz é negado por padrão (spec T-49, CA-06; TC-07; matriz X-01 a X-03). */
@TesteDeReservaApi
class RotasForaDaMatrizApiTest extends BaseDaApiDeReserva {

    static Stream<Arguments> rotasForaDaMatriz() {
        Stream.Builder<Arguments> casos = Stream.builder();
        String[][] rotas = {
            {"X-01", "DELETE", "/api/salas/1"},
            {"X-02", "GET", "/api/reservas"},
            {"X-02", "GET", "/api/reservas/1"},
            {"X-03", "GET", "/api/relatorios"},
            {"X-03", "POST", "/api/manutencoes"},
            {"X-03", "GET", "/api/qualquer-coisa"}};
        for (String[] rota : rotas) {
            for (Perfil perfil : Perfil.values()) {
                casos.add(Arguments.of(rota[0], HttpMethod.valueOf(rota[1]), rota[2], perfil));
            }
        }
        return casos.build();
    }

    @ParameterizedTest(name = "TC-07: {0} {1} {2} por {3} devolve 403")
    @MethodSource("rotasForaDaMatriz")
    @DisplayName("TC-07: toda rota fora da matriz devolve 403 para qualquer perfil autenticado")
    void rotaForaDaMatrizEhNegada(String id, HttpMethod metodo, String caminho, Perfil perfil) throws Exception {
        int status = ambiente.chama(metodo, caminho, ambiente.bearerDe(perfil), null).andReturn().getResponse()
                .getStatus();

        assertThat(status).as("%s %s por %s", metodo, caminho, perfil).isEqualTo(403);
    }

    @ParameterizedTest(name = "TC-07: {0} {1} {2} sem token devolve 401")
    @MethodSource("rotasForaDaMatriz")
    @DisplayName("TC-07: toda rota fora da matriz, sem token, devolve 401")
    void rotaForaDaMatrizSemTokenEh401(String id, HttpMethod metodo, String caminho, Perfil ignorado) throws Exception {
        int status = ambiente.chama(metodo, caminho, null, null).andReturn().getResponse().getStatus();

        assertThat(status).as("%s %s", metodo, caminho).isEqualTo(401);
    }
}
