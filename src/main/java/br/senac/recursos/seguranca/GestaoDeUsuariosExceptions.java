package br.senac.recursos.seguranca;

import java.util.List;
import java.util.Map;

/** Erros da gestão de usuários. O formato único de erro de toda a API é da T-22. */
final class GestaoDeUsuariosExceptions {

    private GestaoDeUsuariosExceptions() {
    }

    /** Um ou mais campos inválidos (400). */
    static class EntradaInvalida extends RuntimeException {
        private final transient List<Map<String, String>> erros;

        EntradaInvalida(List<Map<String, String>> erros) {
            super("Um ou mais campos estão inválidos.");
            this.erros = erros;
        }

        List<Map<String, String>> erros() {
            return erros;
        }
    }

    /** O usuário pedido não existe (404). */
    static class NaoEncontrado extends RuntimeException {
        NaoEncontrado() {
            super("Recurso não encontrado");
        }
    }

    /** E-mail repetido ou último administrador (409). */
    static class Conflito extends RuntimeException {
        Conflito(String mensagem) {
            super(mensagem);
        }
    }
}
