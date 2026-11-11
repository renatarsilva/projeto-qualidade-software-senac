package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;

import org.springframework.jdbc.core.JdbcTemplate;

/** Dados novos para uma célula da matriz: a sala, o dono das reservas e quem chama (o token do perfil testado). */
final class CenarioDeAutorizacao {

    final AmbienteDeReserva ambiente;
    final RelogioDeTeste relogio;
    final JdbcTemplate jdbc;
    final RecursoPedido sala;
    final String bearer;
    private int sequencia = 0;

    CenarioDeAutorizacao(AmbienteDeReserva ambiente, RelogioDeTeste relogio, JdbcTemplate jdbc, RecursoPedido sala,
            String bearer) {
        this.ambiente = ambiente;
        this.relogio = relogio;
        this.jdbc = jdbc;
        this.sala = sala;
        this.bearer = bearer;
    }

    /** Token novo do mesmo perfil, emitido com o relógio de agora (depois de o teste avançar o Clock). */
    String bearerRenovado() {
        return ambiente.bearerDoSolicitante();
    }

    /** E-mail novo, para a criação de usuário não conflitar com outra célula. */
    String emailNovo() {
        return "usuario-" + (++sequencia) + System.nanoTime() + AmbienteDeReserva.DOMINIO_DA_MATRIZ;
    }

    /** Um usuário qualquer, alvo das mudanças de perfil e de situação. */
    long usuarioAlvo() {
        return jdbc.queryForObject(
                "INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES (?, ?, 'hash-de-teste', 'SOLICITANTE', true) RETURNING id",
                Long.class, "Usuário alvo", emailNovo());
    }

    /** Reserva do dono (o Solicitante de teste), terça das 10:00 às 11:00, na sala do cenário. */
    long reservaDoDono(EstadoDaReserva estado) {
        return ambiente.reservaGravada(ambiente.solicitante(), estado, terca(10, 0), terca(11, 0), AGORA, sala);
    }
}
