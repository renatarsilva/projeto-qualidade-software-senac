package br.senac.recursos.notificacao;

/** O adaptador HTTP da API externa de notificações. Esqueleto da T-31: devolve o resultado neutro. */
public class NotificadorHttp implements Notificador {

    private final NotificacaoPropriedades propriedades;

    public NotificadorHttp(NotificacaoPropriedades propriedades) {
        this.propriedades = propriedades;
    }

    @Override
    public ResultadoDaNotificacao notifica(Notificacao notificacao) {
        return ResultadoDaNotificacao.pendente();
    }
}
