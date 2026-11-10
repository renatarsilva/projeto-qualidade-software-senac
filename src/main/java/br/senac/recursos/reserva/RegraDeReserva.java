package br.senac.recursos.reserva;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Regras de intervalo e sobreposição da reserva (RN-01, RN-02, RN-15; spec T-20).
 * A ordem das verificações é a da spec, seção 3 (G9): 404, 403 por dono, 400 e, por último, os 409
 * (não alterável, antecedência, sobreposição).
 */
@Component
public class RegraDeReserva {

    static final String MENSAGEM_ANTECEDENCIA = "A reserva precisa ser pedida com pelo menos 1 hora de antecedência";
    static final String MENSAGEM_NAO_ALTERAVEL = "Esta reserva não pode mais ser alterada";
    static final String AVISO_NOVA_APROVACAO = "A reserva precisa ser aprovada de novo";

    private static final Duration ANTECEDENCIA_MINIMA = Duration.ofHours(1);
    private static final Duration PRAZO_DE_APROVACAO = Duration.ofHours(24);

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
        Optional<DecisaoDaReserva> recusa = recursosInexistentes(pedido)
                .or(() -> entradaInvalida(pedido))
                .or(() -> antecedenciaInsuficiente(pedido))
                .or(() -> sobreposto(pedido, null));
        if (recusa.isPresent()) {
            return recusa.get();
        }
        Instant agora = clock.instant();
        ReservaNaAgenda nova = new ReservaNaAgenda(0, donoId, EstadoDaReserva.SOLICITADA, pedido.inicio(),
                pedido.termino(), pedido.recursos(), agora, prazoDeAprovacao(agora, pedido.inicio()));
        return aceita(agenda.grava(nova), null);
    }

    /** Altera a reserva do dono: o pedido substitui intervalo e recursos por inteiro. */
    public DecisaoDaReserva altera(long donoId, long reservaId, PedidoDeReserva pedido) {
        Optional<ReservaNaAgenda> existente = agenda.busca(reservaId);
        if (existente.isEmpty()) {
            return recusa(MotivoDeRecusa.RESERVA_INEXISTENTE, null, "Recurso não encontrado");
        }
        ReservaNaAgenda atual = existente.get();
        Optional<DecisaoDaReserva> recusa = recursosInexistentes(pedido)
                .or(() -> atual.donoId() != donoId
                        ? Optional.of(recusa(MotivoDeRecusa.NAO_DONO, null, "Acesso negado")) : Optional.empty())
                .or(() -> entradaInvalida(pedido))
                .or(() -> naoAlteravel(atual))
                .or(() -> antecedenciaInsuficiente(pedido))
                .or(() -> sobreposto(pedido, reservaId));
        if (recusa.isPresent()) {
            return recusa.get();
        }
        boolean voltaParaAprovacao = atual.estado() == EstadoDaReserva.APROVADA;
        ReservaNaAgenda alterada = new ReservaNaAgenda(atual.id(), atual.donoId(), EstadoDaReserva.SOLICITADA,
                pedido.inicio(), pedido.termino(), pedido.recursos(), atual.criadaEm(),
                prazoDeAprovacao(clock.instant(), pedido.inicio()));
        return aceita(agenda.grava(alterada), voltaParaAprovacao ? AVISO_NOVA_APROVACAO : null);
    }

    private Optional<DecisaoDaReserva> recursosInexistentes(PedidoDeReserva pedido) {
        return recursosDe(pedido).stream().anyMatch(r -> !agenda.recursoExiste(r))
                ? Optional.of(recusa(MotivoDeRecusa.RECURSO_INEXISTENTE, null, "Recurso não encontrado"))
                : Optional.empty();
    }

    private Optional<DecisaoDaReserva> entradaInvalida(PedidoDeReserva pedido) {
        if (pedido.inicio() == null) {
            return Optional.of(recusa(MotivoDeRecusa.DADO_AUSENTE, "inicio", "deve ser informado"));
        }
        if (pedido.termino() == null) {
            return Optional.of(recusa(MotivoDeRecusa.DADO_AUSENTE, "termino", "deve ser informado"));
        }
        if (!pedido.termino().isAfter(pedido.inicio())) {
            return Optional.of(recusa(MotivoDeRecusa.INTERVALO_INVALIDO, "termino", "deve ser posterior ao início"));
        }
        if (recursosDe(pedido).isEmpty()) {
            return Optional.of(recusa(MotivoDeRecusa.SEM_RECURSO, "recursos", "deve ter ao menos um recurso"));
        }
        return Optional.empty();
    }

    private Optional<DecisaoDaReserva> naoAlteravel(ReservaNaAgenda atual) {
        boolean alteravel = atual.estado() == EstadoDaReserva.SOLICITADA || atual.estado() == EstadoDaReserva.APROVADA;
        return alteravel ? Optional.empty()
                : Optional.of(recusa(MotivoDeRecusa.NAO_ALTERAVEL, null, MENSAGEM_NAO_ALTERAVEL));
    }

    /** RN-15: o início precisa estar pelo menos 1 h depois do Clock (exatamente 1 h vale). */
    private Optional<DecisaoDaReserva> antecedenciaInsuficiente(PedidoDeReserva pedido) {
        Instant limite = clock.instant().plus(ANTECEDENCIA_MINIMA);
        return pedido.inicio().isBefore(limite)
                ? Optional.of(recusa(MotivoDeRecusa.ANTECEDENCIA_INSUFICIENTE, null, MENSAGEM_ANTECEDENCIA))
                : Optional.empty();
    }

    /** RN-02: basta um recurso ocupado para recusar o pedido inteiro; nada é gravado. */
    private Optional<DecisaoDaReserva> sobreposto(PedidoDeReserva pedido, Long reservaIgnorada) {
        return recursosDe(pedido).stream()
                .filter(r -> sobreposicao.ocupado(r, pedido.inicio(), pedido.termino(), reservaIgnorada))
                .findFirst()
                .map(r -> recusa(MotivoDeRecusa.SOBREPOSICAO, null, mensagemDeOcupado(r.tipo())));
    }

    private static String mensagemDeOcupado(TipoDeRecurso tipo) {
        return switch (tipo) {
            case SALA -> "A sala já está reservada neste horário";
            case PROFESSOR -> "O professor já está ocupado neste horário";
            case MATERIAL -> "O material já está reservado neste horário";
        };
    }

    /** RN-14: 24 h antes do início ou, com menos de 24 h de antecedência, 1 h antes. */
    private static Instant prazoDeAprovacao(Instant agora, Instant inicio) {
        boolean temVinteEQuatroHoras = !inicio.minus(PRAZO_DE_APROVACAO).isBefore(agora);
        return temVinteEQuatroHoras ? inicio.minus(PRAZO_DE_APROVACAO) : inicio.minus(ANTECEDENCIA_MINIMA);
    }

    private static List<RecursoPedido> recursosDe(PedidoDeReserva pedido) {
        return pedido.recursos() == null ? List.of() : pedido.recursos();
    }

    private static DecisaoDaReserva recusa(MotivoDeRecusa motivo, String campo, String mensagem) {
        return new DecisaoDaReserva(Veredito.RECUSADA, motivo, campo, mensagem, null, null);
    }

    private static DecisaoDaReserva aceita(ReservaNaAgenda reserva, String aviso) {
        return new DecisaoDaReserva(Veredito.ACEITA, MotivoDeRecusa.NENHUM, null, null, reserva, aviso);
    }
}
