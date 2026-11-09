package br.senac.recursos.reserva;

import java.time.Clock;

/**
 * Regras de intervalo e sobreposição da reserva (RN-01, RN-02, RN-15; spec T-20).
 * Assinaturas mínimas da T-20: devolvem o resultado neutro até a T-23.
 */
public class RegraDeReserva {

    private final Clock clock;
    private final AgendaDeReservas agenda;
    private final VerificadorDeSobreposicao sobreposicao;

    public RegraDeReserva(Clock clock, AgendaDeReservas agenda, VerificadorDeSobreposicao sobreposicao) {
        this.clock = clock;
        this.agenda = agenda;
        this.sobreposicao = sobreposicao;
    }

    /** Cria uma reserva para o dono. */
    public DecisaoDaReserva cria(long donoId, PedidoDeReserva pedido) {
        return DecisaoDaReserva.pendente();
    }

    /** Altera a reserva do dono: o pedido substitui intervalo e recursos por inteiro. */
    public DecisaoDaReserva altera(long donoId, long reservaId, PedidoDeReserva pedido) {
        return DecisaoDaReserva.pendente();
    }
}
