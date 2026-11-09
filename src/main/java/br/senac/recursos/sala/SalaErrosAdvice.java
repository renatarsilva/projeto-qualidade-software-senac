package br.senac.recursos.sala;

import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento provisório dos erros das salas. O formato único de erro de toda a API é da T-22:
 * quando ela entrar, este arquivo sai.
 */
@RestControllerAdvice(assignableTypes = SalaController.class)
public class SalaErrosAdvice {

    @ExceptionHandler(EntradaInvalidaException.class)
    public ProblemDetail entradaInvalida(EntradaInvalidaException erro) {
        return dadosInvalidos(erro.getErros());
    }

    /** Tipo ou formato errado em um campo do JSON (texto no lugar de número, decimal, número grande demais). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail jsonIlegivel(HttpMessageNotReadableException erro) {
        String campo = "corpo";
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof JsonMappingException mapeamento && !mapeamento.getPath().isEmpty()) {
                campo = mapeamento.getPath().get(mapeamento.getPath().size() - 1).getFieldName();
                break;
            }
        }
        return dadosInvalidos(List.of(Map.of("campo", String.valueOf(campo), "motivo", "valor inválido")));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(ConflitoDeNegocioException.class)
    public ProblemDetail conflito(ConflitoDeNegocioException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, erro.getMessage());
        problema.setTitle("Conflito");
        return problema;
    }

    private ProblemDetail dadosInvalidos(List<Map<String, String>> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);
        return problema;
    }
}
