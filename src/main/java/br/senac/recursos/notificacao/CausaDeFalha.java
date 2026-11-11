package br.senac.recursos.notificacao;

/** Por que uma notificação falhou (spec T-31, seção 4). */
public enum CausaDeFalha {
    HTTP_4XX,
    INDISPONIVEL,
    TIMEOUT,
    RESPOSTA_INVALIDA,
    CONEXAO
}
