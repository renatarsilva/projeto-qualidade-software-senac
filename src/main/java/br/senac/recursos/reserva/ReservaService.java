package br.senac.recursos.reserva;

import br.senac.recursos.erro.AcessoNegadoException;
import br.senac.recursos.erro.ConflitoDeNegocioException;
import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplica a regra de reserva numa transação e traduz a recusa para as exceções que a API já sabe responder. */
@Service
public class ReservaService {

    /** Destinos que só o sistema ou os endpoints próprios aplicam: pelo PATCH, qualquer perfil recebe 403 (CA-14). */
    private static final Set<EstadoDaReserva> DESTINOS_DO_SISTEMA = Set.of(EstadoDaReserva.EM_USO,
            EstadoDaReserva.CONCLUIDA, EstadoDaReserva.CANCELADA, EstadoDaReserva.NAO_COMPARECEU,
            EstadoDaReserva.SOLICITADA);

    private final RegraDeReserva regra;
    private final CicloDeVidaDaReserva ciclo;
    private final AgendaDeReservas agenda;

    public ReservaService(RegraDeReserva regra, CicloDeVidaDaReserva ciclo, AgendaDeReservas agenda) {
        this.regra = regra;
        this.ciclo = ciclo;
        this.agenda = agenda;
    }

    @Transactional
    public ReservaResponse cria(long donoId, PedidoDeReserva pedido) {
        return resposta(gravando(() -> regra.cria(donoId, pedido)));
    }

    @Transactional
    public ReservaResponse altera(long donoId, long reservaId, PedidoDeReserva pedido) {
        return resposta(gravando(() -> regra.altera(donoId, reservaId, pedido)));
    }

    /**
     * O Responsável muda o estado pelo PATCH, só para APROVADA ou REJEITADA. A ordem dos erros é a da spec T-21
     * (CA-17): 403 pelo destino, 404, 400 por estado inexistente e, por fim, o 409 da regra.
     */
    @Transactional
    public ReservaResponse mudaEstado(long usuarioId, long reservaId, String destino) {
        Optional<EstadoDaReserva> estado = CicloDeVidaDaReserva.estadoDe(destino);
        if (estado.isPresent() && DESTINOS_DO_SISTEMA.contains(estado.get())) {
            throw new AcessoNegadoException();
        }
        if (agenda.busca(reservaId).isEmpty()) {
            throw new RecursoNaoEncontradoException();
        }
        if (destino == null) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "estado", "motivo", "deve ser informado")));
        }
        return resposta(gravando(() -> ciclo.muda(reservaId, destino, usuarioId)));
    }

    @Transactional
    public ReservaResponse cancela(long donoId, long reservaId) {
        return resposta(gravando(() -> ciclo.cancela(donoId, reservaId)));
    }

    @Transactional
    public ReservaResponse checkin(long donoId, long reservaId) {
        return resposta(gravando(() -> ciclo.checkin(donoId, reservaId)));
    }

    /** Só recusa: reserva nunca é apagada. */
    @Transactional
    public ReservaResponse apaga(long reservaId) {
        return resposta(ciclo.apaga(reservaId));
    }

    /** Se a verificação da aplicação passou e o banco recusou (EXCLUDE, numa corrida), a resposta é o 409 genérico. */
    private DecisaoDaReserva gravando(java.util.function.Supplier<DecisaoDaReserva> operacao) {
        try {
            return operacao.get();
        } catch (DataIntegrityViolationException erro) {
            throw new ConflitoDeNegocioException(ConflitoDeNegocioException.MENSAGEM_DO_BANCO);
        }
    }

    private ReservaResponse resposta(DecisaoDaReserva decisao) {
        if (decisao.veredito() == Veredito.ACEITA) {
            return ReservaResponse.de(decisao);
        }
        switch (decisao.motivo()) {
            case RESERVA_INEXISTENTE, RECURSO_INEXISTENTE:
                throw new RecursoNaoEncontradoException();
            case NAO_DONO:
                throw new AcessoNegadoException();
            case DADO_AUSENTE, INTERVALO_INVALIDO, SEM_RECURSO, ESTADO_INEXISTENTE:
                throw new EntradaInvalidaException(
                        List.of(Map.of("campo", decisao.campo(), "motivo", decisao.mensagem())));
            default:
                throw new ConflitoDeNegocioException(decisao.mensagem());
        }
    }
}
