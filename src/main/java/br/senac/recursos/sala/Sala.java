package br.senac.recursos.sala;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "sala")
@DiscriminatorValue("SALA")
@PrimaryKeyJoinColumn(name = "recurso_id")
public class Sala extends Recurso {

    @Column(nullable = false)
    private int capacidade;

    @Column(nullable = false, length = 100)
    private String localizacao;

    @Column(name = "tipo_sala", nullable = false, length = 100)
    private String tipoSala;

    protected Sala() {
        // exigido pelo JPA
    }

    public Sala(String nome, boolean restrito, int capacidade, String localizacao, String tipoSala) {
        super(nome, restrito);
        this.capacidade = capacidade;
        this.localizacao = localizacao;
        this.tipoSala = tipoSala;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public String getTipoSala() {
        return tipoSala;
    }
}
