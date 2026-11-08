package br.senac.recursos.sala;

import br.senac.recursos.erro.ConflitoDeNegocioException;
import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaService {

    private static final String UNICA_VIOLADA = "23505"; // SQLState de violação de restrição única

    private final SalaRepository salas;
    private final Validator validador;

    public SalaService(SalaRepository salas, Validator validador) {
        this.salas = salas;
        this.validador = validador;
    }

    @Transactional
    public Sala cadastra(SalaRequest pedido) {
        SalaRequest limpo = valida(pedido, false);
        if (salas.existsByNomeIgnoreCase(limpo.nome())) {
            throw new ConflitoDeNegocioException("Já existe uma sala com esse nome");
        }
        boolean restrito = Boolean.TRUE.equals(limpo.restrito()); // P-03: omitido no cadastro vale false
        return grava(new Sala(limpo.nome(), restrito, limpo.capacidade(), limpo.localizacao(), limpo.tipoSala()));
    }

    @Transactional
    public Sala altera(long id, SalaRequest pedido) {
        Sala sala = salas.findById(id).orElseThrow(RecursoNaoEncontradoException::new); // 404 antes do 400
        SalaRequest limpo = valida(pedido, true);
        if (salas.existsByNomeIgnoreCaseAndIdNot(limpo.nome(), id)) {
            throw new ConflitoDeNegocioException("Já existe uma sala com esse nome");
        }
        sala.atualiza(limpo.nome(), limpo.restrito(), limpo.capacidade(), limpo.localizacao(), limpo.tipoSala());
        return grava(sala);
    }

    @Transactional(readOnly = true)
    public Sala busca(long id) {
        return salas.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
    }

    @Transactional(readOnly = true)
    public List<Sala> lista() {
        return salas.findAll();
    }

    /** Limpa os espaços e valida. No PUT, o restrito é obrigatório (P-03). */
    private SalaRequest valida(SalaRequest pedido, boolean restritoObrigatorio) {
        SalaRequest limpo = pedido.limpo();
        List<Map<String, String>> erros = new ArrayList<>();
        validador.validate(limpo).stream()
                .sorted(Comparator.comparing((ConstraintViolation<SalaRequest> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .forEach(v -> erros.add(Map.of("campo", v.getPropertyPath().toString(), "motivo", v.getMessage())));
        if (restritoObrigatorio && limpo.restrito() == null) {
            erros.add(Map.of("campo", "restrito", "motivo", "deve ser informado"));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return limpo;
    }

    /** Grava e, se o banco recusar por nome repetido (cadastros simultâneos), responde 409 e nunca 500. */
    private Sala grava(Sala sala) {
        try {
            return salas.saveAndFlush(sala);
        } catch (DataIntegrityViolationException erro) {
            Throwable causa = NestedExceptionUtils.getMostSpecificCause(erro);
            if (causa instanceof SQLException sql && UNICA_VIOLADA.equals(sql.getSQLState())) {
                throw new ConflitoDeNegocioException(ConflitoDeNegocioException.MENSAGEM_DO_BANCO);
            }
            throw erro;
        }
    }
}
