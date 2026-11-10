package br.senac.recursos.reserva;

/**
 * Resposta da regra de reserva. Em caso de recusa traz o motivo, o campo (quando houver) e a mensagem de negócio;
 * em caso de aceite traz a reserva gravada.
 */
public record DecisaoDaReserva(Veredito veredito, MotivoDeRecusa motivo, String campo, String mensagem,
        ReservaNaAgenda reserva, String aviso) {

    /** O resultado neutro da T-20: não é aceite nem recusa. */
    public static DecisaoDaReserva pendente() {
        return new DecisaoDaReserva(Veredito.PENDENTE, MotivoDeRecusa.NENHUM, null, null, null, null);
    }

    public static DecisaoDaReserva recusada(MotivoDeRecusa motivo, String campo, String mensagem) {
        return new DecisaoDaReserva(Veredito.RECUSADA, motivo, campo, mensagem, null, null);
    }

    public static DecisaoDaReserva aceita(ReservaNaAgenda reserva, String aviso) {
        return new DecisaoDaReserva(Veredito.ACEITA, MotivoDeRecusa.NENHUM, null, null, reserva, aviso);
    }
}
