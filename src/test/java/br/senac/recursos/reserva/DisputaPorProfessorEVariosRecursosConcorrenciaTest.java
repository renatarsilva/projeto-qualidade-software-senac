package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.DisparoSimultaneo.quantosCom;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Agenda do professor e pedidos com vários recursos, inclusive em ordem invertida
 * (spec T-37, CA-07, CA-08 e CA-14; TC-07, TC-10 e TC-17).
 */
@TesteDeReservaApi
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=25")
class DisputaPorProfessorEVariosRecursosConcorrenciaTest extends BaseDeConcorrencia {

    @Test
    @DisplayName("TC-10: 20 solicitantes, 20 salas diferentes e o mesmo professor: 1 × 201, 19 × 409 e 1 vínculo do professor")
    void agendaDoProfessorSobConcorrencia() throws Exception {
        RecursoPedido professor = ambiente.professor("renata-silva-12345");
        List<Long> ids = ambiente.novosSolicitantes(20);
        List<RecursoPedido> salas = new ArrayList<>();
        List<Callable<DisparoSimultaneo.Resposta>> tarefas = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            RecursoPedido sala = ambiente.sala(String.format("sala-%d", 201 + i));
            salas.add(sala);
            tarefas.add(pedido(i, ambiente.bearerDoUsuario(ids.get(i)), corpo(inicio, fim, sala, professor)));
        }

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(tarefas);

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.vinculosAtivosNo(professor, inicio, fim)).as("vínculos do professor").isEqualTo(1);
        int salasOcupadas = salas.stream().mapToInt(s -> ambiente.vinculosAtivosNo(s, inicio, fim)).sum();
        assertThat(salasOcupadas).as("salas ocupadas (as dos pedidos recusados continuam livres)").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-07 (RN-03-T7): A pede sala-102 e projetor, B pede sala-103 e o mesmo projetor: 1 aceito, 1 × 409 e nada parcial")
    void tudoOuNadaComProjetorEmComum() throws Exception {
        RecursoPedido sala103 = ambiente.sala("sala-103");
        RecursoPedido projetor = ambiente.material("Projetor", "PAT-1");
        List<Long> ids = ambiente.novosSolicitantes(2);

        List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                pedido(0, ambiente.bearerDoUsuario(ids.get(0)), corpo(inicio, fim, sala, projetor)),
                pedido(1, ambiente.bearerDoUsuario(ids.get(1)), corpo(inicio, fim, sala103, projetor))));

        assertUmaAceitaEOutrasRecusadas(respostas);
        assertThat(ambiente.vinculos()).as("vínculos gravados (sala e projetor do vencedor)").isEqualTo(2);
        boolean venceuA = respostas.get(0).status() == 201;
        RecursoPedido salaDoRecusado = venceuA ? sala103 : sala;
        assertThat(ambiente.vinculosAtivosNo(salaDoRecusado, inicio, fim))
                .as("a sala do pedido recusado não fica gravada").isZero();
    }

    @Test
    @DisplayName("TC-17: [sala-102, projetor] e [projetor, sala-102] ao mesmo tempo: 1 × 201 e 1 × 409, nunca 500, sem gravação parcial")
    void recursosEmOrdemInvertidaNaoTravam() throws Exception {
        RecursoPedido projetor = ambiente.material("Projetor", "PAT-1");
        List<Long> ids = ambiente.novosSolicitantes(2);
        String bearerA = ambiente.bearerDoUsuario(ids.get(0));
        String bearerB = ambiente.bearerDoUsuario(ids.get(1));

        // O deadlock depende do cruzamento exato das gravações; várias rodadas aumentam a chance de ele aparecer.
        for (int rodada = 1; rodada <= 15; rodada++) {
            ambiente.limpaReservas();

            List<DisparoSimultaneo.Resposta> respostas = DisparoSimultaneo.dispara(List.of(
                    pedido(0, bearerA, corpo(inicio, fim, sala, projetor)),
                    pedido(1, bearerB, corpo(inicio, fim, projetor, sala))));

            assertThat(quantosCom(respostas, 201)).as("rodada %d: respostas 201", rodada).isEqualTo(1);
            assertThat(quantosCom(respostas, 409)).as("rodada %d: respostas 409", rodada).isEqualTo(1);
            assertThat(ambiente.reservas()).as("rodada %d: linhas em reserva", rodada).isEqualTo(1);
            assertThat(ambiente.vinculos()).as("rodada %d: vínculos (sem gravação parcial)", rodada).isEqualTo(2);
        }
    }
}
