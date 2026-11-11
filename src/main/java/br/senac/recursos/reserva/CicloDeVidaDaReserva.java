package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.EstadoDaReserva.APROVADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CANCELADA;
import static br.senac.recursos.reserva.EstadoDaReserva.CONCLUIDA;
import static br.senac.recursos.reserva.EstadoDaReserva.EM_USO;
import static br.senac.recursos.reserva.EstadoDaReserva.NAO_COMPARECEU;
import static br.senac.recursos.reserva.EstadoDaReserva.REJEITADA;
import static br.senac.recursos.reserva.EstadoDaReserva.SOLICITADA;

import br.senac.recursos.auditoria.AuditoriaDeReservas;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Ciclo de vida da reserva: tabela de transições, cancelamento, check-in e a recusa de apagar (spec T-21).
 * A ordem das verificações é 404, 403 por dono e 409. O perfil (401 e 403) é conferido antes, na API.
 */
@Component
public class CicloDeVidaDaReserva {

    static final String MENSAGEM_TRANSICAO = "Esta mudança de estado não é permitida";
    static final String MENSAGEM_PRAZO_DE_APROVACAO = "O prazo de aprovação desta reserva terminou";
    static final String MENSAGEM_NAO_CANCELAVEL = "Esta reserva não pode mais ser cancelada";
    static final String MENSAGEM_NAO_APAGAVEL = "Reservas não podem ser apagadas. Use o cancelamento.";
    static final String MENSAGEM_CHECKIN = "O check-in só pode ser feito durante o horário reservado";

    /** As 7 transições da tabela da spec (seção 3). Qualquer outra combinação é recusada. */
    private static final Map<EstadoDaReserva, Set<EstadoDaReserva>> TRANSICOES = Map.of(
            SOLICITADA, Set.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, Set.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, Set.of(CONCLUIDA));

    private final Clock clock;
    private final AgendaDeReservas agenda;
    private final AuditoriaDeReservas auditoria;

    public CicloDeVidaDaReserva(Clock clock, AgendaDeReservas agenda, AuditoriaDeReservas auditoria) {
        this.auditoria = auditoria;
        this.clock = clock;
        this.agenda = agenda;
    }

    /** A tabela de transições da spec: ACEITA ou RECUSADA para cada par origem e destino. */
    public static Veredito avaliaTransicao(EstadoDaReserva origem, EstadoDaReserva destino) {
        return TRANSICOES.getOrDefault(origem, Set.of()).contains(destino) ? Veredito.ACEITA : Veredito.RECUSADA;
    }

    /**
     * Aplica a transição da tabela com as condições de estado e de tempo. Aprovar e rejeitar são do Responsável;
     * concluir, marcar {@code NAO_COMPARECEU} e cancelar por prazo são do sistema (job da T-60). O check-in e o
     * cancelamento do dono têm métodos próprios.
     */
    public DecisaoDaReserva muda(long reservaId, EstadoDaReserva destino) {
        Optional<ReservaNaAgenda> existente = agenda.busca(reservaId);
        if (existente.isEmpty()) {
            return naoEncontrada();
        }
        ReservaNaAgenda reserva = existente.get();
        if (avaliaTransicao(reserva.estado(), destino) != Veredito.ACEITA) {
            return transicaoProibida();
        }
        Instant agora = clock.instant();
        switch (destino) {
            case APROVADA:
                if (agora.isAfter(reserva.prazoAprovacaoEm())) {
                    return DecisaoDaReserva.recusada(MotivoDeRecusa.PRAZO_DE_APROVACAO_VENCIDO, null,
                            MENSAGEM_PRAZO_DE_APROVACAO);
                }
                return grava(reserva, APROVADA);
            case REJEITADA:
                return grava(reserva, REJEITADA);
            case CANCELADA:
                // O sistema só cancela depois do prazo (instante > prazo). O dono cancela por cancela().
                return reserva.estado() == SOLICITADA && agora.isAfter(reserva.prazoAprovacaoEm())
                        ? grava(reserva, CANCELADA) : transicaoProibida();
            case NAO_COMPARECEU, CONCLUIDA:
                // O sistema só encerra a partir do fim do horário; antes disso o dono usa o cancelamento.
                return !agora.isBefore(reserva.termino()) ? grava(reserva, destino) : transicaoProibida();
            default:
                // EM_USO só pelo check-in do dono.
                return transicaoProibida();
        }
    }

    /** Como {@link #muda(long, EstadoDaReserva)}, informando quem muda (o Responsável); vazio significa sistema. */
    public DecisaoDaReserva muda(long reservaId, EstadoDaReserva destino, Long usuarioId) {
        return muda(reservaId, destino);
    }

    /** Destino vindo do cliente como texto: um estado que não existe é recusado. */
    public DecisaoDaReserva muda(long reservaId, String destino) {
        Optional<EstadoDaReserva> estado = estadoDe(destino);
        if (estado.isEmpty()) {
            return DecisaoDaReserva.recusada(MotivoDeRecusa.ESTADO_INEXISTENTE, "estado", "valor inválido");
        }
        return muda(reservaId, estado.get());
    }

    /** O destino é um estado que existe? */
    public static Optional<EstadoDaReserva> estadoDe(String texto) {
        try {
            return texto == null ? Optional.empty() : Optional.of(EstadoDaReserva.valueOf(texto));
        } catch (IllegalArgumentException erro) {
            return Optional.empty();
        }
    }

    /** O dono cancela; o resultado depende do instante e do check-in (RN-13). */
    public DecisaoDaReserva cancela(long donoId, long reservaId) {
        Optional<ReservaNaAgenda> existente = agenda.busca(reservaId);
        if (existente.isEmpty()) {
            return naoEncontrada();
        }
        ReservaNaAgenda reserva = existente.get();
        if (reserva.donoId() != donoId) {
            return naoEDono();
        }
        Instant agora = clock.instant();
        boolean antesDoFim = agora.isBefore(reserva.termino());
        boolean ateOInicio = !agora.isAfter(reserva.inicio());
        EstadoDaReserva resultado = switch (reserva.estado()) {
            case SOLICITADA -> ateOInicio ? CANCELADA : null;
            case APROVADA -> ateOInicio ? CANCELADA : (antesDoFim ? NAO_COMPARECEU : null);
            case EM_USO -> antesDoFim ? CONCLUIDA : null;
            default -> null;
        };
        if (resultado == null) {
            return DecisaoDaReserva.recusada(MotivoDeRecusa.NAO_CANCELAVEL, null, MENSAGEM_NAO_CANCELAVEL);
        }
        return grava(reserva, resultado);
    }

    /** O dono registra a chegada: {@code APROVADA → EM_USO} em {@code [início, fim)}. */
    public DecisaoDaReserva checkin(long donoId, long reservaId) {
        Optional<ReservaNaAgenda> existente = agenda.busca(reservaId);
        if (existente.isEmpty()) {
            return naoEncontrada();
        }
        ReservaNaAgenda reserva = existente.get();
        if (reserva.donoId() != donoId) {
            return naoEDono();
        }
        Instant agora = clock.instant();
        boolean dentroDoHorario = !agora.isBefore(reserva.inicio()) && agora.isBefore(reserva.termino());
        if (reserva.estado() != APROVADA || !dentroDoHorario) {
            return DecisaoDaReserva.recusada(MotivoDeRecusa.FORA_DA_JANELA_DO_CHECKIN, null, MENSAGEM_CHECKIN);
        }
        return grava(reserva, EM_USO);
    }

    /** Reserva nunca é apagada (RN-08): sempre recusa. */
    public DecisaoDaReserva apaga(long reservaId) {
        if (agenda.busca(reservaId).isEmpty()) {
            return naoEncontrada();
        }
        return DecisaoDaReserva.recusada(MotivoDeRecusa.NAO_APAGAVEL, null, MENSAGEM_NAO_APAGAVEL);
    }

    /** Ponto único da mudança de estado: a auditoria (RN-09, T-40) se pluga aqui. */
    private DecisaoDaReserva grava(ReservaNaAgenda reserva, EstadoDaReserva novoEstado) {
        ReservaNaAgenda nova = new ReservaNaAgenda(reserva.id(), reserva.donoId(), novoEstado, reserva.inicio(),
                reserva.termino(), reserva.recursos(), reserva.criadaEm(), reserva.prazoAprovacaoEm());
        return DecisaoDaReserva.aceita(agenda.grava(nova), null);
    }

    private static DecisaoDaReserva transicaoProibida() {
        return DecisaoDaReserva.recusada(MotivoDeRecusa.TRANSICAO_PROIBIDA, null, MENSAGEM_TRANSICAO);
    }

    private static DecisaoDaReserva naoEncontrada() {
        return DecisaoDaReserva.recusada(MotivoDeRecusa.RESERVA_INEXISTENTE, null, "Recurso não encontrado");
    }

    private static DecisaoDaReserva naoEDono() {
        return DecisaoDaReserva.recusada(MotivoDeRecusa.NAO_DONO, null, "Acesso negado");
    }
}
