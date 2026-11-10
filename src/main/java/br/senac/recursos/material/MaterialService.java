package br.senac.recursos.material;

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
public class MaterialService {

    private static final String UNICA_VIOLADA = "23505"; // violação de restrição única
    private static final String CHAVE_ESTRANGEIRA_VIOLADA = "23503"; // o registro é referenciado por outro

    private final MaterialRepository materiais;
    private final Validator validador;

    public MaterialService(MaterialRepository materiais, Validator validador) {
        this.materiais = materiais;
        this.validador = validador;
    }

    @Transactional
    public MaterialResponse cadastra(MaterialRequest pedido) {
        MaterialRequest limpo = valida(pedido, false);
        exigeNomeEPatrimonioLivres(limpo, null);
        boolean restrito = Boolean.TRUE.equals(limpo.restrito()); // P-04: omitido no cadastro vale false
        return MaterialResponse.de(grava(
                new Material(limpo.nome(), restrito, limpo.tipoMaterial(), limpo.patrimonio())));
    }

    @Transactional
    public MaterialResponse altera(long id, MaterialRequest pedido) {
        Material material = materiais.findById(id).orElseThrow(RecursoNaoEncontradoException::new); // 404 antes do 400
        MaterialRequest limpo = valida(pedido, true);
        exigeNomeEPatrimonioLivres(limpo, id);
        material.atualiza(limpo.nome(), limpo.restrito(), limpo.tipoMaterial(), limpo.patrimonio());
        return MaterialResponse.de(grava(material));
    }

    @Transactional
    public void exclui(long id) {
        Material material = materiais.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
        try {
            materiais.delete(material);
            materiais.flush();
        } catch (DataIntegrityViolationException erro) {
            if (sqlState(erro).equals(CHAVE_ESTRANGEIRA_VIOLADA)) {
                throw new ConflitoDeNegocioException("Este registro está em uso e não pode ser removido");
            }
            throw erro;
        }
    }

    @Transactional(readOnly = true)
    public MaterialResponse busca(long id) {
        return MaterialResponse.de(materiais.findById(id).orElseThrow(RecursoNaoEncontradoException::new));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> lista() {
        return materiais.findAll().stream().map(MaterialResponse::de).toList();
    }

    /** Limpa os espaços e valida. No PUT, o restrito é obrigatório (P-04). */
    private MaterialRequest valida(MaterialRequest pedido, boolean restritoObrigatorio) {
        MaterialRequest limpo = pedido.limpo();
        List<Map<String, String>> erros = new ArrayList<>();
        validador.validate(limpo).stream()
                .sorted(Comparator.comparing((ConstraintViolation<MaterialRequest> v) -> v.getPropertyPath().toString())
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

    /** A mensagem diz qual campo repetiu: o nome, o patrimônio ou os dois (spec T-18, CA-03). */
    private void exigeNomeEPatrimonioLivres(MaterialRequest pedido, Long idDoProprio) {
        boolean nomeRepetido = idDoProprio == null
                ? materiais.existsByNomeIgnoreCase(pedido.nome())
                : materiais.existsByNomeIgnoreCaseAndIdNot(pedido.nome(), idDoProprio);
        boolean patrimonioRepetido = idDoProprio == null
                ? materiais.existsByPatrimonioIgnoreCase(pedido.patrimonio())
                : materiais.existsByPatrimonioIgnoreCaseAndIdNot(pedido.patrimonio(), idDoProprio);
        if (nomeRepetido && patrimonioRepetido) {
            throw new ConflitoDeNegocioException("Já existe um material com esse nome e esse patrimônio");
        }
        if (nomeRepetido) {
            throw new ConflitoDeNegocioException("Já existe um material com esse nome");
        }
        if (patrimonioRepetido) {
            throw new ConflitoDeNegocioException("Já existe um material com esse patrimônio");
        }
    }

    /** Grava e, se o banco recusar por nome ou patrimônio repetidos (corrida), responde 409 e nunca 500. */
    private Material grava(Material material) {
        try {
            return materiais.saveAndFlush(material);
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
