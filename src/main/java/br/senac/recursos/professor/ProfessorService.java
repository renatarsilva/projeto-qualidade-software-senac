package br.senac.recursos.professor;

import br.senac.recursos.sala.ConflitoDeNegocioException;
import br.senac.recursos.sala.EntradaInvalidaException;
import br.senac.recursos.sala.RecursoNaoEncontradoException;
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
public class ProfessorService {

    private static final String UNICA_VIOLADA = "23505"; // violação de restrição única
    private static final String CHAVE_ESTRANGEIRA_VIOLADA = "23503"; // o registro é referenciado por outro

    private final ProfessorRepository professores;
    private final Validator validador;

    public ProfessorService(ProfessorRepository professores, Validator validador) {
        this.professores = professores;
        this.validador = validador;
    }

    @Transactional
    public ProfessorResponse cadastra(ProfessorRequest pedido) {
        ProfessorRequest limpo = valida(pedido, false);
        exigeNomeEMatriculaLivres(limpo.nome(), limpo.matricula(), null);
        boolean restrito = Boolean.TRUE.equals(limpo.restrito()); // P-05: omitido no cadastro vale false
        List<String> competencias = limpo.competencias() == null ? List.of() : limpo.competencias();
        return ProfessorResponse.de(grava(new Professor(limpo.nome(), restrito, limpo.matricula(), competencias)));
    }

    @Transactional
    public ProfessorResponse altera(long id, ProfessorRequest pedido) {
        Professor professor = professores.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
        ProfessorRequest limpo = valida(pedido, true);
        exigeNomeEMatriculaLivres(limpo.nome(), limpo.matricula(), id);
        professor.atualiza(limpo.nome(), limpo.restrito(), limpo.matricula(), limpo.competencias());
        return ProfessorResponse.de(grava(professor));
    }

    @Transactional
    public void exclui(long id) {
        Professor professor = professores.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
        try {
            professores.delete(professor);
            professores.flush();
        } catch (DataIntegrityViolationException erro) {
            if (sqlState(erro).equals(CHAVE_ESTRANGEIRA_VIOLADA)) {
                throw new ConflitoDeNegocioException("Este registro está em uso e não pode ser removido");
            }
            throw erro;
        }
    }

    @Transactional(readOnly = true)
    public ProfessorResponse busca(long id) {
        return ProfessorResponse.de(professores.findById(id).orElseThrow(RecursoNaoEncontradoException::new));
    }

    @Transactional(readOnly = true)
    public List<ProfessorResponse> lista() {
        return professores.findAll().stream().map(ProfessorResponse::de).toList();
    }

    /** Limpa as competências e valida o nome e a lista. No PUT, restrito e competencias são obrigatórios (P-03). */
    private ProfessorRequest valida(ProfessorRequest pedido, boolean substituicaoCompleta) {
        ProfessorRequest limpo = pedido.limpo();
        List<Map<String, String>> erros = new ArrayList<>();
        validador.validate(limpo).stream()
                .sorted(Comparator.comparing((ConstraintViolation<ProfessorRequest> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .forEach(v -> erros.add(Map.of("campo", v.getPropertyPath().toString(), "motivo", v.getMessage())));
        limpo.motivosDasCompetencias().forEach(motivo -> erros.add(Map.of("campo", "competencias", "motivo", motivo)));
        if (substituicaoCompleta && limpo.restrito() == null) {
            erros.add(Map.of("campo", "restrito", "motivo", "deve ser informado"));
        }
        if (substituicaoCompleta && limpo.competencias() == null) {
            erros.add(Map.of("campo", "competencias", "motivo", "deve ser informado"));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return limpo;
    }

    private void exigeNomeEMatriculaLivres(String nome, String matricula, Long idDoProprio) {
        boolean nomeRepetido = idDoProprio == null
                ? professores.existsByNomeIgnoreCase(nome)
                : professores.existsByNomeIgnoreCaseAndIdNot(nome, idDoProprio);
        if (nomeRepetido) {
            throw new ConflitoDeNegocioException("Já existe um professor com esse nome");
        }
        boolean matriculaRepetida = idDoProprio == null
                ? professores.existsByMatricula(matricula)
                : professores.existsByMatriculaAndIdNot(matricula, idDoProprio);
        if (matriculaRepetida) {
            throw new ConflitoDeNegocioException("Já existe um professor com essa matrícula");
        }
    }

    /** Grava e, se o banco recusar por nome ou matrícula repetidos (corrida), responde 409 e nunca 500. */
    private Professor grava(Professor professor) {
        try {
            return professores.saveAndFlush(professor);
        } catch (DataIntegrityViolationException erro) {
            if (sqlState(erro).equals(UNICA_VIOLADA)) {
                throw new ConflitoDeNegocioException(ConflitoDeNegocioException.MENSAGEM_DO_BANCO);
            }
            throw erro;
        }
    }

    private String sqlState(DataIntegrityViolationException erro) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(erro);
        return causa instanceof SQLException sql && sql.getSQLState() != null ? sql.getSQLState() : "";
    }
}
