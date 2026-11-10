package br.senac.recursos.reserva;

/** Estados da reserva (RN-05 e RN-06). Os três primeiros ocupam o horário (S-02). */
public enum EstadoDaReserva {
    SOLICITADA,
    APROVADA,
    EM_USO,
    CONCLUIDA,
    REJEITADA,
    CANCELADA,
    NAO_COMPARECEU;

    /** Só {@code SOLICITADA}, {@code APROVADA} e {@code EM_USO} bloqueiam outro pedido (S-02). */
    public boolean ocupaHorario() {
        return this == SOLICITADA || this == APROVADA || this == EM_USO;
    }
}
