package br.senac.recursos.reserva;

import br.senac.recursos.erro.AcessoNegadoException;
import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import br.senac.recursos.seguranca.Perfil;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de reservas com a posse respeitada (spec T-43, NC-01): o Solicitante vê só as próprias; o Responsável
 * e o Administrador veem todas.
 */
@Service
public class ConsultaDeReservasService {

    private final AgendaDeReservas agenda;

    public ConsultaDeReservasService(AgendaDeReservas agenda) {
        this.agenda = agenda;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> lista(long usuarioId, Perfil perfil, String estado) {
        EstadoDaReserva filtro = null;
        if (estado != null) {
            filtro = CicloDeVidaDaReserva.estadoDe(estado).orElseThrow(
                    () -> new EntradaInvalidaException(List.of(Map.of("campo", "estado", "motivo", "valor inválido"))));
        }
        EstadoDaReserva desejado = filtro;
        return agenda.reservas().stream()
                .filter(r -> perfil != Perfil.SOLICITANTE || r.donoId() == usuarioId)
                .filter(r -> desejado == null || r.estado() == desejado)
                .sorted(Comparator.comparing(ReservaNaAgenda::inicio).thenComparingLong(ReservaNaAgenda::id))
                .map(r -> ReservaResponse.de(DecisaoDaReserva.aceita(r, null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservaResponse consulta(long usuarioId, Perfil perfil, long reservaId) {
        ReservaNaAgenda reserva = agenda.busca(reservaId).orElseThrow(RecursoNaoEncontradoException::new);
        if (perfil == Perfil.SOLICITANTE && reserva.donoId() != usuarioId) {
            throw new AcessoNegadoException();
        }
        return ReservaResponse.de(DecisaoDaReserva.aceita(reserva, null));
    }
}
