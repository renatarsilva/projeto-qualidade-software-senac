package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.CenarioDeReserva.DONO_A;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_B;
import static br.senac.recursos.reserva.CenarioDeReserva.DONO_C;
import static br.senac.recursos.reserva.CenarioDeReserva.assertAceita;
import static br.senac.recursos.reserva.CenarioDeReserva.assertRecusada;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pedido com vários recursos: tudo ou nada; recurso inexistente e lista vazia (spec T-20, CA-10 e CA-12; TC-19, TC-20, TC-36, TC-37). */
class PedidoComVariosRecursosTest {

    private CenarioDeReserva cenario;
    private RecursoPedido sala;
    private RecursoPedido projetor;
    private RecursoPedido professor;

    @BeforeEach
    void preparar() {
        cenario = new CenarioDeReserva();
        sala = cenario.sala(102);
        projetor = cenario.material(5);
        professor = cenario.professor(7);
    }

    @Test
    @DisplayName("TC-19 (RN-02-T5): só o projetor ocupado recusa o pedido por inteiro; sala e professor continuam livres")
    void recusaPorInteiroQuandoUmRecursoEstaOcupado() {
        cenario.reserva(DONO_B, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), projetor);

        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(11, 0), sala, projetor, professor);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertNadaGravado(1);
        assertAceita(cenario.cria(DONO_C, terca(10, 0), terca(11, 0), sala, professor));
    }

    @Test
    @DisplayName("TC-20 (RN-02-T6): sala e professor em conflito ao mesmo tempo recusam o pedido e nada é gravado")
    void recusaQuandoDoisRecursosConflitam() {
        cenario.reserva(DONO_B, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        cenario.reserva(DONO_C, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), professor);

        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(11, 0), sala, professor);

        assertRecusada(decisao, MotivoDeRecusa.SOBREPOSICAO);
        cenario.assertNadaGravado(2);
    }

    @Test
    @DisplayName("TC-36 (RN-01-T9): a sala 999, que não existe, é recusada como não encontrada e nada é gravado")
    void recusaRecursoInexistente() {
        DecisaoDaReserva decisao = cenario.cria(DONO_A, terca(10, 0), terca(11, 0),
                new RecursoPedido(TipoDeRecurso.SALA, 999));

        assertRecusada(decisao, MotivoDeRecusa.RECURSO_INEXISTENTE);
        cenario.assertNadaGravado(0);
    }

    @Test
    @DisplayName("TC-37 (RN-01-T10): lista de recursos vazia é recusada com o erro no campo recursos")
    void recusaListaDeRecursosVazia() {
        DecisaoDaReserva decisao = cenario.regra.cria(DONO_A,
                new PedidoDeReserva(terca(10, 0), terca(11, 0), List.of()));

        assertRecusada(decisao, MotivoDeRecusa.SEM_RECURSO);
        assertThat(decisao.campo()).isEqualTo("recursos");
        assertThat(decisao.mensagem()).isEqualTo("deve ter ao menos um recurso");
        cenario.assertNadaGravado(0);
    }
}
