package br.senac.recursos.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Cria os ProblemDetail de erro. Neutro por enquanto: devolve sempre um erro genérico. */
public final class FabricaDeProblemas {

    private FabricaDeProblemas() {
    }

    public static ProblemDetail autenticacaoNecessaria(String instancia) {
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ProblemDetail acessoNegado(String instancia) {
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
