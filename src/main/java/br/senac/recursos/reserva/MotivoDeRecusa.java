package br.senac.recursos.reserva;

/** Por que um pedido foi recusado. A ordem dos 409 está na spec T-20, seção 3 (G9). */
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
    SOBREPOSICAO
}
