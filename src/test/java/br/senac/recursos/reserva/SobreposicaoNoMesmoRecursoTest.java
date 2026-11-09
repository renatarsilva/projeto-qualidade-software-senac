package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Sobreposição em sala, material e professor, com as fronteiras (spec T-20, CA-04 a CA-07;
 * TC-09 a TC-14, CT-01 V01 a V21; TC-27 e TC-28, RN-02-T12 e T13). Existente: terça, 10:00 às 11:00, APROVADA.
 */
class SobreposicaoNoMesmoRecursoTest {

    private CenarioDeReserva cenario;
    private RecursoPedido sala;
    private RecursoPedido material;
    private RecursoPedido professor;

    @BeforeEach
    void preparar() {
        cenario = new CenarioDeReserva();
        sala = cenario.sala(102);
        material = cenario.material(5);
        professor = cenario.professor(7);
    }

    static Stream<Arguments> formasDeSobreposicao() {
        return Stream.of(
                Arguments.of("começa dentro da existente", terca(10, 30), terca(11, 30)),
                Arguments.of("termina dentro da existente", terca(9, 30), terca(10, 30)),
                Arguments.of("está contida na existente", terca(10, 15), terca(10, 45)),
                Arguments.of("contém a existente", terca(9, 30), terca(11, 30)),
                Arguments.of("é idêntica à existente", terca(10, 0), terca(11, 0)));
    }

    static Stream<Arguments> fronteirasEncostadas() {
        return Stream.of(
                Arguments.of("começa quando a existente termina", terca(11, 0), terca(12, 0)),
                Arguments.of("termina quando a existente começa", terca(9, 0), terca(10, 0)));
    }

    private void recusaSobreposicao(RecursoPedido ocupado, Instant inicio, Instant termino, RecursoPedido... pedidos) {
        cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), ocupado);

        DecisaoDaReserva decisao = cenario.cria(DONO_B, inicio, termino, pedidos);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertNadaGravado(1);
    }

    private void aceitaEncostada(RecursoPedido ocupado, Instant inicio, Instant termino, RecursoPedido... pedidos) {
        cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), ocupado);

        assertAceita(cenario.cria(DONO_B, inicio, termino, pedidos));
    }

    @ParameterizedTest(name = "TC-09 (V01 a V05) sala: {0}")
    @MethodSource("formasDeSobreposicao")
    @DisplayName("TC-09: sobreposição na sala é recusada nas cinco formas")
    void recusaSobreposicaoNaSala(String forma, Instant inicio, Instant termino) {
        recusaSobreposicao(sala, inicio, termino, sala);
    }

    @ParameterizedTest(name = "TC-10 (V06 e V07) sala: {0}")
    @MethodSource("fronteirasEncostadas")
    @DisplayName("TC-10: reservas encostadas na sala são aceitas (S-01)")
    void aceitaEncostadasNaSala(String fronteira, Instant inicio, Instant termino) {
        aceitaEncostada(sala, inicio, termino, sala);
    }

    @ParameterizedTest(name = "TC-11 (V08 a V12) material: {0}")
    @MethodSource("formasDeSobreposicao")
    @DisplayName("TC-11: sobreposição no material é recusada nas cinco formas")
    void recusaSobreposicaoNoMaterial(String forma, Instant inicio, Instant termino) {
        recusaSobreposicao(material, inicio, termino, material);
    }

    @ParameterizedTest(name = "TC-12 (V13 e V14) material: {0}")
    @MethodSource("fronteirasEncostadas")
    @DisplayName("TC-12: reservas encostadas no material são aceitas (S-01)")
    void aceitaEncostadasNoMaterial(String fronteira, Instant inicio, Instant termino) {
        aceitaEncostada(material, inicio, termino, material);
    }

    @ParameterizedTest(name = "TC-13 (V15 a V19) professor, com sala e material livres: {0}")
    @MethodSource("formasDeSobreposicao")
    @DisplayName("TC-13: sobreposição na agenda do professor é recusada nas cinco formas")
    void recusaSobreposicaoNoProfessor(String forma, Instant inicio, Instant termino) {
        recusaSobreposicao(professor, inicio, termino, sala, material, professor);
    }

    @ParameterizedTest(name = "TC-14 (V20 e V21) professor: {0}")
    @MethodSource("fronteirasEncostadas")
    @DisplayName("TC-14: reservas encostadas na agenda do professor são aceitas (S-01)")
    void aceitaEncostadasNoProfessor(String fronteira, Instant inicio, Instant termino) {
        aceitaEncostada(professor, inicio, termino, sala, material, professor);
    }

    @Test
    @DisplayName("TC-27 (RN-02-T12): outra sala no mesmo horário é aceita")
    void aceitaOutraSalaNoMesmoHorario() {
        RecursoPedido outraSala = cenario.sala(101);
        cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);

        assertAceita(cenario.cria(DONO_B, terca(10, 0), terca(11, 0), outraSala));
    }

    @Test
    @DisplayName("TC-28 (RN-02-T13): a mesma sala em outro dia é aceita")
    void aceitaAMesmaSalaEmOutroDia() {
        cenario.reserva(DONO_A, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);

        assertAceita(cenario.cria(DONO_B, HorariosDeTeste.quarta(10, 0), HorariosDeTeste.quarta(11, 0), sala));
    }
}
