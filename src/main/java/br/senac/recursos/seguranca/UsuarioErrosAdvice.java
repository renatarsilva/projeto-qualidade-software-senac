package br.senac.recursos.seguranca;

import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.Conflito;
import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.EntradaInvalida;
import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.NaoEncontrado;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Tratamento provisório dos erros da gestão de usuários. O formato único de erro da API é da T-22. */
@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioErrosAdvice {

    @ExceptionHandler(EntradaInvalida.class)
    public ProblemDetail entradaInvalida(EntradaInvalida erro) {
        return dadosInvalidos(erro.erros());
    }

    /** Valor de tipo errado em um campo do JSON, como um perfil que não existe. */
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

    @ExceptionHandler(NaoEncontrado.class)
    public ProblemDetail naoEncontrado(NaoEncontrado erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(Conflito.class)
    public ProblemDetail conflito(Conflito erro) {
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
