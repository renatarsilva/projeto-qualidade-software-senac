package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.DisparoSimultaneo.quantosCom;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Solicitações simultâneas à mesma sala (spec T-37, CA-01 a CA-06, CA-10 e CA-12; TC-01 a TC-06, TC-08, TC-09,
 * TC-11, TC-12 e TC-16). A contagem sempre vem do banco. As 20 threads usam um pool de 25 conexões.
 */
@TesteDeReservaApi
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
class DisputaPelaMesmaSalaConcorrenciaTest extends BaseDeConcorrencia {

    private static final Set<String> MENSAGENS_PERMITIDAS = Set.of(
            "A sala já está reservada neste horário",
            "O professor já está ocupado neste horário",
            "O material já está reservado neste horário",
            "Esta operação conflita com outro registro");

    @RepeatedTest(value = 5, name = "TC-01 e TC-11 (RN-03-T1, CT-02): 20 solicitantes, repetição {currentRepetition} de {totalRepetitions}")
    @DisplayName("TC-01 e TC-11: 20 solicitantes pedem a mesma sala e o mesmo intervalo: 1 × 201, 19 × 409 e 1 reserva ativa")
    void vinteSolicitantesGeramUmaReserva() throws Exception {
        List<DisparoSimultaneo.Resposta> respostas = disputa(20, inicio, fim, sala);

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.vinculosAtivosNo(sala, inicio, fim)).as("reservas ativas no banco").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-16 (CT-02): depois da disputa, o banco tem 1 linha em reserva e 1 em reserva_recurso, nenhuma das recusadas")
    void recusadasNaoGravamNada() throws Exception {
        List<DisparoSimultaneo.Resposta> respostas = disputa(20, inicio, fim, sala);

        assertThat(quantosCom(respostas, 201)).as("respostas 201").isEqualTo(1);
        assertThat(ambiente.reservas()).as("linhas em reserva").isEqualTo(1);
        assertThat(ambiente.vinculos()).as("linhas em reserva_recurso").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-12: toda resposta perdedora é 409 com uma das mensagens do contrato, sem 500 e sem vazamento")
    void recusasSaoSempre409SemVazamento() throws Exception {
        List<DisparoSimultaneo.Resposta> respostas = disputa(20, inicio, fim, sala);

        assertThat(respostas).as("respostas").allSatisfy(r -> assertThat(r.status()).isIn(201, 409));
        respostas.stream().filter(r -> r.status() == 409).forEach(r -> {
            assertThat(MENSAGENS_PERMITIDAS.stream().anyMatch(r.corpo()::contains))
                    .as("mensagem de uma recusa: " + r.corpo()).isTrue();
            assertThat(r.corpo()).doesNotContain("Exception", "SQL", "org.springframework", "at br.senac",
                    "Solicitante concorrente", AmbienteDeReserva.DOMINIO_DOS_CONCORRENTES);
        });
        assertThat(quantosCom(respostas, 409)).as("respostas 409").isEqualTo(19);
    }

    @RepeatedTest(value = 5, name = "TC-02 e TC-08 (RN-03-T2 e T8): 2 solicitantes, repetição {currentRepetition} de {totalRepetitions}")
    @DisplayName("TC-02 e TC-08: a menor disputa dá 1 × 201, 1 × 409 e 1 reserva ativa, nas 5 repetições")
    void menorDisputa() throws Exception {
        List<DisparoSimultaneo.Resposta> respostas = disputa(2, inicio, fim, sala);

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.vinculosAtivosNo(sala, inicio, fim)).as("reservas ativas no banco").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-03 (RN-03-T3): 10:00-11:00 e 10:30-11:30 ao mesmo tempo: 1 aceita e 1 recusada")
    void sobreposicaoParcial() throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(2);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, ambiente.bearerDoUsuario(ids.get(0)), corpo(quarta(10, 0), quarta(11, 0), sala)),
                pedido(1, ambiente.bearerDoUsuario(ids.get(1)), corpo(quarta(10, 30), quarta(11, 30), sala))));

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.vinculosAtivosNo(sala, quarta(10, 0), quarta(11, 30))).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-04 (RN-03-T4): 10:00-11:00 e 11:00-12:00 na mesma sala ao mesmo tempo: as duas são aceitas")
    void intervalosEncostadosConvivem() throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(2);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, ambiente.bearerDoUsuario(ids.get(0)), corpo(quarta(10, 0), quarta(11, 0), sala)),
                pedido(1, ambiente.bearerDoUsuario(ids.get(1)), corpo(quarta(11, 0), quarta(12, 0), sala))));

        assertThat(quantosCom(respostas, 201)).as("respostas 201").isEqualTo(2);
        assertThat(ambiente.vinculosAtivosNo(sala, quarta(10, 0), quarta(12, 0))).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-05 (RN-03-T5): salas diferentes no mesmo horário, ao mesmo tempo: as duas são aceitas")
    void salasDiferentesConvivem() throws Exception {
        RecursoPedido outraSala = ambiente.sala("sala-103");
        List<Long> ids = ambiente.novosSolicitantes(2);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, ambiente.bearerDoUsuario(ids.get(0)), corpo(inicio, fim, sala)),
                pedido(1, ambiente.bearerDoUsuario(ids.get(1)), corpo(inicio, fim, outraSala))));

        assertThat(quantosCom(respostas, 201)).as("respostas 201").isEqualTo(2);
        assertThat(ambiente.reservas()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-06 (RN-03-T6): o mesmo usuário envia o mesmo pedido 2 vezes ao mesmo tempo: 1 reserva e 1 recusa")
    void duploCliqueDoMesmoUsuario() throws Exception {
        String bearer = ambiente.bearerDoSolicitante();
        String json = corpo(inicio, fim, sala);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, bearer, json), pedido(1, bearer, json)));

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.reservas()).as("linhas em reserva").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-09 (RN-03-T9): depois da disputa, quem foi recusado pede outro horário livre e é aceito")
    void recusadoPedeOutroHorario() throws Exception {
        List<Long> ids = ambiente.novosSolicitantes(2);
        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, ambiente.bearerDoUsuario(ids.get(0)), corpo(inicio, fim, sala)),
                pedido(1, ambiente.bearerDoUsuario(ids.get(1)), corpo(inicio, fim, sala))));
        assertUmaAceitaEOutrasRecusadas(respostas);
        int recusado = respostas.stream().filter(r -> r.status() == 409).findFirst().orElseThrow().indice();

        ambiente.cria(ambiente.bearerDoUsuario(ids.get(recusado)), corpo(quarta(12, 0), quarta(13, 0), sala))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
    }
}
