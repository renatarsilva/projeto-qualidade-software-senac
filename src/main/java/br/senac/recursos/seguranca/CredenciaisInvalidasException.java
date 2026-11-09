package br.senac.recursos.seguranca;

/** Senha errada, e-mail inexistente ou usuário desativado: a resposta é sempre a mesma (spec T-15, P-03). */
public class CredenciaisInvalidasException extends RuntimeException {

    public static final String MENSAGEM = "E-mail ou senha inválidos";

    public CredenciaisInvalidasException() {
        super(MENSAGEM);
    }
}
