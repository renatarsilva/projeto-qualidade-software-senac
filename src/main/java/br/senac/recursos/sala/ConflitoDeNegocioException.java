package br.senac.recursos.sala;

/** A operação conflita com outro registro (409). A mensagem está em linguagem de negócio. */
public class ConflitoDeNegocioException extends RuntimeException {

    public static final String MENSAGEM_DO_BANCO = "Esta operação conflita com outro registro";

    public ConflitoDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
