package br.senac.recursos.erro;

import br.senac.recursos.seguranca.CredenciaisInvalidasException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * O tratamento único dos erros de todos os controladores (spec T-22). Cada exceção vira o ProblemDetail certo
 * (na API) ou a página amigável (nas telas), e nenhum detalhe interno vai para a resposta: ele só vai para o log.
 */
@ControllerAdvice
public class ErrosGlobaisAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(ErrosGlobaisAdvice.class);

    private static final String UNICA_VIOLADA = "23505";
    private static final String EXCLUSAO_VIOLADA = "23P01";
    private static final String CHAVE_ESTRANGEIRA_VIOLADA = "23503";

    private final RespostaDeErro resposta;

    public ErrosGlobaisAdvice(RespostaDeErro resposta) {
        this.resposta = resposta;
    }

    @ExceptionHandler(EntradaInvalidaException.class)
    ResponseEntity<?> entradaInvalida(EntradaInvalidaException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.dadosInvalidos(erro.getErros(), caminho(requisicao)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> corpoInvalido(MethodArgumentNotValidException erro, HttpServletRequest requisicao) {
        List<Map<String, String>> erros = new ArrayList<>();
        erro.getBindingResult().getFieldErrors().forEach(campo -> erros.add(
                Map.of("campo", campo.getField(), "motivo", String.valueOf(campo.getDefaultMessage()))));
        return resposta.para(caminho(requisicao), FabricaDeProblemas.dadosInvalidos(erros, caminho(requisicao)));
    }

    /** Tipo ou formato errado em um campo do JSON, ou JSON malformado. Sem conversão de tipos (P-04 da T-16). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> jsonIlegivel(HttpMessageNotReadableException erro, HttpServletRequest requisicao) {
        String campo = null;
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
                campo = mapeamento.getPath().get(mapeamento.getPath().size() - 1).getFieldName();
                break;
            }
        }
        List<Map<String, String>> erros = campo == null
                ? List.of(Map.of("campo", "corpo", "motivo", "Corpo da requisição inválido"))
                : List.of(Map.of("campo", campo, "motivo", "valor inválido"));
        return resposta.para(caminho(requisicao), FabricaDeProblemas.dadosInvalidos(erros, caminho(requisicao)));
    }

    /** Um valor de caminho que não é do tipo esperado, como um id que não é número. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<?> tipoErradoNoCaminho(MethodArgumentTypeMismatchException erro, HttpServletRequest requisicao) {
        List<Map<String, String>> erros = List.of(Map.of("campo", erro.getName(), "motivo", "valor inválido"));
        return resposta.para(caminho(requisicao), FabricaDeProblemas.dadosInvalidos(erros, caminho(requisicao)));
    }

    @ExceptionHandler({RecursoNaoEncontradoException.class, NoResourceFoundException.class})
    ResponseEntity<?> naoEncontrado(Exception erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.naoEncontrado(caminho(requisicao)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> metodoNaoPermitido(HttpRequestMethodNotSupportedException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.metodoNaoPermitido(caminho(requisicao)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<?> formatoNaoSuportado(HttpMediaTypeNotSupportedException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.formatoNaoSuportado(caminho(requisicao)));
    }

    @ExceptionHandler(ConflitoDeNegocioException.class)
    ResponseEntity<?> conflito(ConflitoDeNegocioException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.conflito(erro.getMessage(), caminho(requisicao)));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<?> acessoNegado(AcessoNegadoException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao), FabricaDeProblemas.acessoNegado(caminho(requisicao)));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<?> credenciaisInvalidas(CredenciaisInvalidasException erro, HttpServletRequest requisicao) {
        return resposta.para(caminho(requisicao),
                FabricaDeProblemas.credenciaisInvalidas(erro.getMessage(), caminho(requisicao)));
    }

    /** Violação do banco: repetido (UNIQUE ou EXCLUDE) e registro em uso (chave estrangeira) viram 409; o resto, 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> violacaoDoBanco(DataIntegrityViolationException erro, HttpServletRequest requisicao) {
        String estado = sqlState(erro);
        if (estado.equals(UNICA_VIOLADA) || estado.equals(EXCLUSAO_VIOLADA)) {
            return resposta.para(caminho(requisicao),
                    FabricaDeProblemas.conflito(FabricaDeProblemas.MENSAGEM_DO_BANCO, caminho(requisicao)));
        }
        if (estado.equals(CHAVE_ESTRANGEIRA_VIOLADA)) {
            return resposta.para(caminho(requisicao),
                    FabricaDeProblemas.conflito(FabricaDeProblemas.MENSAGEM_DE_REGISTRO_EM_USO, caminho(requisicao)));
        }
        return inesperado(erro, requisicao);
    }

    /** Qualquer outra falha: 500 neutro. O detalhe técnico fica só no log. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> inesperado(Exception erro, HttpServletRequest requisicao) {
        LOG.error("Erro inesperado em {}", caminho(requisicao), erro);
        return resposta.para(caminho(requisicao), FabricaDeProblemas.erroInesperado(caminho(requisicao)));
    }

    private String caminho(HttpServletRequest requisicao) {
        return requisicao.getRequestURI();
    }

    private String sqlState(DataIntegrityViolationException erro) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(erro);
        return causa instanceof SQLException sql && sql.getSQLState() != null ? sql.getSQLState() : "";
    }
}
