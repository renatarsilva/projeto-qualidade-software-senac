package br.senac.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

/** Monta a regra de reserva com agenda em memória e Clock fixo, e oferece as conferências dos testes unitários. */
final class CenarioDeReserva {

    static final long DONO_A = 1;
    static final long DONO_B = 2;
    static final long DONO_C = 3;

    final AgendaEmMemoria agenda = new AgendaEmMemoria();
    final RegraDeReserva regra;
    final CicloDeVidaDaReserva ciclo;
    final AuditoriaEmMemoria auditoria;

    CenarioDeReserva() {
        this(HorariosDeTeste.AGORA);
    }

    CenarioDeReserva(Instant agora) {
        Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
        this.auditoria = new AuditoriaEmMemoria(clock);
        this.regra = new RegraDeReserva(clock, agenda, new SobreposicaoPelaAgenda(agenda), auditoria);
        this.ciclo = new CicloDeVidaDaReserva(clock, agenda, auditoria);
    }

    /** Recurso que existe no cadastro. */
    RecursoPedido existe(TipoDeRecurso tipo, long id) {
        RecursoPedido recurso = new RecursoPedido(tipo, id);
        agenda.cadastra(recurso);
        return recurso;
    }

    RecursoPedido sala(long numero) {
        return existe(TipoDeRecurso.SALA, numero);
    }

    RecursoPedido professor(long id) {
        return existe(TipoDeRecurso.PROFESSOR, id);
    }

    RecursoPedido material(long id) {
        return existe(TipoDeRecurso.MATERIAL, id);
    }

    /** Reserva já gravada na agenda (o Clock dos testes não interfere em reservas que já existem). */
    ReservaNaAgenda reserva(long donoId, EstadoDaReserva estado, Instant inicio, Instant termino,
            RecursoPedido... recursos) {
        return agenda.grava(new ReservaNaAgenda(0, donoId, estado, inicio, termino, List.of(recursos),
                HorariosDeTeste.AGORA.minusSeconds(5 * 24 * 3600L), inicio.minusSeconds(24 * 3600L)));
    }

    /** Reserva já gravada, com o prazo de aprovação escolhido pelo teste. */
    ReservaNaAgenda reservaComPrazo(long donoId, EstadoDaReserva estado, Instant inicio, Instant termino,
            Instant prazo, RecursoPedido... recursos) {
        return agenda.grava(new ReservaNaAgenda(0, donoId, estado, inicio, termino, List.of(recursos),
                HorariosDeTeste.AGORA.minusSeconds(5 * 24 * 3600L), prazo));
    }

    /** O recurso está ocupado neste período? Responde pela agenda, como a verificação de sobreposição. */
    boolean ocupado(RecursoPedido recurso, Instant inicio, Instant termino) {
        return new SobreposicaoPelaAgenda(agenda).ocupado(recurso, inicio, termino, null);
    }

    EstadoDaReserva estadoDe(ReservaNaAgenda reserva) {
        return agenda.busca(reserva.id()).orElseThrow().estado();
    }

    static PedidoDeReserva pedido(Instant inicio, Instant termino, RecursoPedido... recursos) {
        return new PedidoDeReserva(inicio, termino, List.of(recursos));
    }

    DecisaoDaReserva cria(long donoId, Instant inicio, Instant termino, RecursoPedido... recursos) {
        return regra.cria(donoId, pedido(inicio, termino, recursos));
    }

    DecisaoDaReserva altera(ReservaNaAgenda reserva, Instant inicio, Instant termino, RecursoPedido... recursos) {
        return regra.altera(reserva.donoId(), reserva.id(), pedido(inicio, termino, recursos));
    }

    // Conferências: o veredito vem sempre primeiro, para o teste falhar por asserção.

    static void assertAceita(DecisaoDaReserva decisao) {
        assertThat(decisao.veredito()).as("veredito (motivo: %s)", decisao.motivo()).isEqualTo(Veredito.ACEITA);
    }

    static void assertRecusada(DecisaoDaReserva decisao, MotivoDeRecusa motivo) {
        assertThat(decisao.veredito()).as("veredito").isEqualTo(Veredito.RECUSADA);
        assertThat(decisao.motivo()).as("motivo da recusa").isEqualTo(motivo);
    }

    /** A reserva fica no estado esperado, tanto na decisão quanto na agenda. */
    void assertMudouPara(DecisaoDaReserva decisao, ReservaNaAgenda reserva, EstadoDaReserva esperado) {
        assertAceita(decisao);
        assertThat(decisao.reserva().estado()).as("estado na decisão").isEqualTo(esperado);
        assertThat(estadoDe(reserva)).as("estado na agenda").isEqualTo(esperado);
    }

    void assertNadaGravado(int reservasAntes) {
        assertThat(agenda.reservas()).as("reservas gravadas").hasSize(reservasAntes);
    }

    void assertMantida(ReservaNaAgenda original) {
        assertThat(agenda.busca(original.id())).as("reserva depois da recusa").contains(original);
    }
}
