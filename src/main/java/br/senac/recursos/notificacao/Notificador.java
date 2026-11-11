package br.senac.recursos.notificacao;

/** A porta de notificação (RF-11). Nunca lança exceção: a falha é um resultado. */
public interface Notificador {

    ResultadoDaNotificacao notifica(Notificacao notificacao);
}
