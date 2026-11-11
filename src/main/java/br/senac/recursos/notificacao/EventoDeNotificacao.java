package br.senac.recursos.notificacao;

/** Os eventos que notificam (DP-03): aprovação, rejeição e cancelamento. Os demais estados não notificam. */
public enum EventoDeNotificacao {
    APROVADA,
    REJEITADA,
    CANCELADA
}
