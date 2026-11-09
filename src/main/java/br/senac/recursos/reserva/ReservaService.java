package br.senac.recursos.reserva;

import br.senac.recursos.sala.ConflitoDeNegocioException;
import br.senac.recursos.sala.EntradaInvalidaException;
import br.senac.recursos.sala.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aplica a regra de reserva numa transação e traduz a recusa para as exceções que a API já sabe responder. */
@Service
public class ReservaService {

    private final RegraDeReserva regra;

    public ReservaService(RegraDeReserva regra) {
        this.regra = regra;
    }

    @Transactional
    public ReservaResponse cria(long donoId, PedidoDeReserva pedido) {
        return resposta(gravando(() -> regra.cria(donoId, pedido)));
    }

    @Transactional
    public ReservaResponse altera(long donoId, long reservaId, PedidoDeReserva pedido) {
        return resposta(gravando(() -> regra.altera(donoId, reservaId, pedido)));
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
                throw new AccessDeniedException("Acesso negado");
            case DADO_AUSENTE, INTERVALO_INVALIDO, SEM_RECURSO:
                throw new EntradaInvalidaException(
                        List.of(Map.of("campo", decisao.campo(), "motivo", decisao.mensagem())));
            default:
                throw new ConflitoDeNegocioException(decisao.mensagem());
        }
    }
}
