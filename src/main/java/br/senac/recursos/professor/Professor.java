package br.senac.recursos.professor;

import br.senac.recursos.sala.Recurso;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "professor")
@DiscriminatorValue("PROFESSOR")
@PrimaryKeyJoinColumn(name = "recurso_id")
public class Professor extends Recurso {

    @Column(nullable = false, length = 5)
    private String matricula;

    @ElementCollection
    @CollectionTable(name = "competencia", joinColumns = @JoinColumn(name = "professor_id"))
    @Column(name = "descricao", nullable = false, length = 80)
    private List<String> competencias = new ArrayList<>();

    protected Professor() {
        // exigido pelo JPA
    }

    public Professor(String nome, boolean restrito, String matricula, List<String> competencias) {
        super(nome, restrito);
        this.matricula = matricula;
        this.competencias = new ArrayList<>(competencias);
    }

    public void atualiza(String nome, boolean restrito, String matricula, List<String> novasCompetencias) {
        atualizaBase(nome, restrito);
        this.matricula = matricula;
        this.competencias.clear();
        this.competencias.addAll(novasCompetencias);
    }

    public String getMatricula() {
        return matricula;
    }

    public List<String> getCompetencias() {
        return List.copyOf(competencias);
    }
}
