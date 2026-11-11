package br.senac.recursos.notificacao;

/**
 * O resultado de notificar. Toda falha vira {@code FALHOU} com a causa: o {@link Notificador} nunca lança exceção.
 * {@code PENDENTE} é o resultado neutro da T-31 (red): nem enviada nem falha.
 */
public record ResultadoDaNotificacao(Situacao situacao, String id, CausaDeFalha causa) {

    public enum Situacao {
        ENVIADA,
        FALHOU,
        PENDENTE
    }

    public static ResultadoDaNotificacao pendente() {
        return new ResultadoDaNotificacao(Situacao.PENDENTE, null, null);
    }

    public static ResultadoDaNotificacao enviada(String id) {
        return new ResultadoDaNotificacao(Situacao.ENVIADA, id, null);
    }

    public static ResultadoDaNotificacao falhou(CausaDeFalha causa) {
        return new ResultadoDaNotificacao(Situacao.FALHOU, null, causa);
    }
}
