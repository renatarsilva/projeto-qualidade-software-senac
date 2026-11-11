package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Toda rota mapeada na aplicação está na matriz (spec T-49, CA-08; TC-10). */
@TesteDeReservaApi
class CompletudeDaMatrizTest extends BaseDaApiDeReserva {

    @Autowired
    private RequestMappingHandlerMapping mapeamentos;

    private static String chave(String metodo, String caminho) {
        return metodo + " " + caminho;
    }

    @Test
    @DisplayName("TC-10: toda rota /api mapeada na aplicação tem uma linha na matriz de autorização")
    void todaRotaMapeadaEstaNaMatriz() {
        Set<String> daMatriz = new TreeSet<>();
        MatrizDeAutorizacao.OPERACOES.forEach(o -> daMatriz.add(chave(o.metodo().name(), o.modelo())));
        daMatriz.add(chave("POST", MatrizDeAutorizacao.LOGIN));

        Set<String> mapeadas = new TreeSet<>();
        for (RequestMappingInfo info : mapeamentos.getHandlerMethods().keySet()) {
            Set<String> caminhos = info.getPathPatternsCondition() == null ? Set.of()
                    : info.getPathPatternsCondition().getPatternValues();
            for (String caminho : caminhos) {
                // As rotas de teste ficam em src/test e não são da API.
                boolean deTeste = caminho.startsWith("/api/teste/") || caminho.startsWith("/api/testes-de-erro/");
                if (caminho.startsWith("/api/") && !deTeste) {
                    info.getMethodsCondition().getMethods().forEach(m -> mapeadas.add(chave(m.name(), caminho)));
                }
            }
        }

        assertThat(mapeadas).as("rotas /api mapeadas na aplicação").isNotEmpty();
        assertThat(mapeadas).as("rotas mapeadas que não estão na matriz (toda rota precisa de uma linha)")
                .isSubsetOf(daMatriz);
    }
}
