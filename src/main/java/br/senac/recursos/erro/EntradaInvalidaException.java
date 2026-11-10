package br.senac.recursos.erro;

import java.util.List;
import java.util.Map;

/** Um ou mais campos do pedido são inválidos (400). Cada erro traz o campo e o motivo. */
public class EntradaInvalidaException extends RuntimeException {

    private final transient List<Map<String, String>> erros;

    public EntradaInvalidaException(List<Map<String, String>> erros) {
        super("Um ou mais campos estão inválidos.");
        this.erros = erros;
    }

    public List<Map<String, String>> getErros() {
        return erros;
    }
}
