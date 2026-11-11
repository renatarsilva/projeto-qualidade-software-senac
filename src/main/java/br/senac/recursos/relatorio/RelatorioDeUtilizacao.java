package br.senac.recursos.relatorio;

import java.time.Instant;
import java.util.List;

/** O relatório de utilização de um período (spec T-41, RF-12). */
public record RelatorioDeUtilizacao(Instant inicio, Instant fim, List<HorasDoRecurso> recursos,
        List<CargaDoProfessor> professores, int conflitosEvitados) {

    /** Horas reservadas de um recurso dentro do período. */
    public record HorasDoRecurso(long recursoId, String tipo, String nome, double horasReservadas) {
    }

    /** Carga horária alocada de um professor dentro do período. */
    public record CargaDoProfessor(long professorId, String nome, double horasAlocadas) {
    }
}
