package br.senac.recursos.erro;

/** Quem pede não pode fazer a operação, e só o serviço sabe disso (por exemplo, não é o dono da reserva): 403. */
public class AcessoNegadoException extends RuntimeException {

    public static final String MENSAGEM = "Você não tem permissão para esta operação";

    public AcessoNegadoException() {
        super(MENSAGEM);
    }
}
