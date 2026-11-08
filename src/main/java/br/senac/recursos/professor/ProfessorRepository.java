package br.senac.recursos.professor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByMatricula(String matricula);
}
