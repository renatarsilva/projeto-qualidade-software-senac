package br.senac.recursos.professor;

import java.util.List;

public record ProfessorResponse(Long id, String nome, String matricula, boolean restrito, List<String> competencias) {

    static ProfessorResponse de(Professor professor) {
        return new ProfessorResponse(professor.getId(), professor.getNome(), professor.getMatricula(),
                professor.isRestrito(), professor.getCompetencias());
    }
}
