package br.senac.recursos.reserva;

/** Por que um pedido foi recusado. A ordem dos 409 está na spec T-20, seção 3 (G9); os últimos motivos são da T-21. */
public enum MotivoDeRecusa {
    NENHUM,
    RESERVA_INEXISTENTE,
    NAO_DONO,
    DADO_AUSENTE,
    INTERVALO_INVALIDO,
    SEM_RECURSO,
    RECURSO_INEXISTENTE,
    NAO_ALTERAVEL,
    ANTECEDENCIA_INSUFICIENTE,
    SOBREPOSICAO,
    ESTADO_INEXISTENTE,
    TRANSICAO_PROIBIDA,
    PRAZO_DE_APROVACAO_VENCIDO,
    NAO_CANCELAVEL,
    NAO_APAGAVEL,
    FORA_DA_JANELA_DO_CHECKIN
}
