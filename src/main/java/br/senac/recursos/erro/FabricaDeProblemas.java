package br.senac.recursos.erro;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Cria todos os ProblemDetail de erro da API (RFC 9457), com título e mensagem em português e sem nenhum detalhe interno.
 * Os filtros de segurança (T-15) e o tratamento de erros usam esta mesma fábrica: o formato é um só (spec T-22).
 */
public final class FabricaDeProblemas {

    public static final String MENSAGEM_DO_BANCO = "Esta operação conflita com outro registro";
    public static final String MENSAGEM_DE_REGISTRO_EM_USO = "Este registro está em uso e não pode ser removido";
    public static final String MENSAGEM_DE_ERRO_INESPERADO = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    private FabricaDeProblemas() {
    }

    public static ProblemDetail autenticacaoNecessaria(String instancia) {
        return problema(HttpStatus.UNAUTHORIZED, "Autenticação necessária",
                "É necessário estar autenticado para acessar este recurso.", instancia);
    }

    public static ProblemDetail credenciaisInvalidas(String mensagem, String instancia) {
        return problema(HttpStatus.UNAUTHORIZED, "Autenticação necessária", mensagem, instancia);
    }

    public static ProblemDetail acessoNegado(String instancia) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", AcessoNegadoException.MENSAGEM, instancia);
    }

    public static ProblemDetail dadosInvalidos(List<Map<String, String>> erros, String instancia) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos estão inválidos.", instancia);
        problema.setProperty("erros", erros);
        return problema;
    }

    public static ProblemDetail naoEncontrado(String instancia) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", "Recurso não encontrado", instancia);
    }

    public static ProblemDetail metodoNaoPermitido(String instancia) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "Este método não é permitido para este recurso.", instancia);
    }

    public static ProblemDetail formatoNaoSuportado(String instancia) {
        return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato não suportado",
                "O formato enviado não é suportado.", instancia);
    }

    public static ProblemDetail conflito(String mensagem, String instancia) {
        return problema(HttpStatus.CONFLICT, "Conflito", mensagem, instancia);
    }

    public static ProblemDetail erroInesperado(String instancia) {
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado", MENSAGEM_DE_ERRO_INESPERADO, instancia);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String instancia) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        if (instancia != null) {
            URI uri = UriComponentsBuilder.fromPath(instancia).build().encode().toUri();
            problema.setInstance(uri);
        }
        return problema;
    }
}
