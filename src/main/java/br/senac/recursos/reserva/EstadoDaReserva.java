package br.senac.recursos.reserva;

/** Estados da reserva (RN-05 e RN-06). Quais deles ocupam o horário é decisão da T-23 (S-02). */
public enum EstadoDaReserva {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU
}
