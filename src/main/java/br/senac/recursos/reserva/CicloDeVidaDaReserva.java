package br.senac.recursos.reserva;

import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Ciclo de vida da reserva: tabela de transições, cancelamento, check-in e a recusa de apagar (spec T-21).
 * Assinaturas mínimas da T-21: devolvem o resultado neutro até a T-24.
 */
@Component
public class CicloDeVidaDaReserva {

    private final Clock clock;
    private final AgendaDeReservas agenda;

    public CicloDeVidaDaReserva(Clock clock, AgendaDeReservas agenda) {
        this.clock = clock;
        this.agenda = agenda;
    }

    /** A tabela de transições da spec (seção 3): ACEITA ou RECUSADA para cada par origem e destino. */
    public static Veredito avaliaTransicao(EstadoDaReserva origem, EstadoDaReserva destino) {
        return Veredito.PENDENTE;
    }

    /** Aplica a transição, com as condições de estado e de tempo (Responsável aprova; o sistema encerra e cancela). */
    public DecisaoDaReserva muda(long reservaId, EstadoDaReserva destino) {
        return DecisaoDaReserva.pendente();
    }

    /** Destino vindo do cliente como texto: um estado que não existe é recusado. */
    public DecisaoDaReserva muda(long reservaId, String destino) {
        return DecisaoDaReserva.pendente();
    }

    /** O dono cancela; o resultado depende do instante e do check-in (RN-13). */
    public DecisaoDaReserva cancela(long donoId, long reservaId) {
        return DecisaoDaReserva.pendente();
    }

    /** O dono registra a chegada: {@code APROVADA → EM_USO} em {@code [início, fim)}. */
    public DecisaoDaReserva checkin(long donoId, long reservaId) {
        return DecisaoDaReserva.pendente();
    }

    /** Reserva nunca é apagada (RN-08): sempre recusa. */
    public DecisaoDaReserva apaga(long reservaId) {
        return DecisaoDaReserva.pendente();
    }
}
