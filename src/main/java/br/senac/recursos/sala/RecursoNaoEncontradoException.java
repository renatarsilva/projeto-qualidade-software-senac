package br.senac.recursos.sala;

/** O identificador pedido não existe (404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException() {
        super("Recurso não encontrado");
    }
}
