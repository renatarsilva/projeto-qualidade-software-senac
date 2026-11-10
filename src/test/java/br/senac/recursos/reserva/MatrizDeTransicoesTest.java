package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CONCLUIDA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.NAO_COMPARECEU;
import static br.senac.recursos.reserva.EstadoDaReserva.REJEITADA;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** As 49 combinações de origem e destino: só as 7 da tabela passam (spec T-21, CA-06; TC-18). */
class MatrizDeTransicoesTest {

    private static final Map<EstadoDaReserva, Set<EstadoDaReserva>> PERMITIDAS = Map.of(
            SOLICITADA, Set.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, Set.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, Set.of(CONCLUIDA));

    static Stream<Arguments> combinacoes() {
        Stream.Builder<Arguments> casos = Stream.builder();
        for (EstadoDaReserva origem : EstadoDaReserva.values()) {
            for (EstadoDaReserva destino : EstadoDaReserva.values()) {
                boolean permitida = PERMITIDAS.getOrDefault(origem, Set.of()).contains(destino);
                casos.add(Arguments.of(origem, destino, permitida ? Veredito.ACEITA : Veredito.RECUSADA));
            }
        }
        return casos.build();
    }

    @ParameterizedTest(name = "TC-18: {0} para {1} é {2}")
    @MethodSource("combinacoes")
    @DisplayName("TC-18: cada combinação de origem e destino tem o veredito da tabela")
    void tabelaDeTransicoes(EstadoDaReserva origem, EstadoDaReserva destino, Veredito esperado) {
        assertThat(CicloDeVidaDaReserva.avaliaTransicao(origem, destino)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("TC-18: exatamente 7 das 49 combinações são aceitas")
    void exatamenteSeteAceitas() {
        long combinacoes = 0;
        long contadas = 0;
        for (EstadoDaReserva origem : EstadoDaReserva.values()) {
            for (EstadoDaReserva destino : EstadoDaReserva.values()) {
                combinacoes++;
                if (CicloDeVidaDaReserva.avaliaTransicao(origem, destino) == Veredito.ACEITA) {
                    contadas++;
                }
            }
        }
        assertThat(combinacoes).as("combinações").isEqualTo(49);
        assertThat(contadas).as("combinações aceitas").isEqualTo(7);
    }
}
