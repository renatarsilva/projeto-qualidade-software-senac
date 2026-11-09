package br.senac.recursos.sala;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/** O que sala, professor e material têm em comum (arquitetura 3.1). Cada tipo tem a sua tabela própria. */
@Entity
@Table(name = "recurso")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tipo")
public abstract class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private boolean restrito;

    protected Recurso() {
        // exigido pelo JPA
    }

    protected Recurso(String nome, boolean restrito) {
        this.nome = nome;
        this.restrito = restrito;
    }

    protected void atualizaBase(String nome, boolean restrito) {
        this.nome = nome;
        this.restrito = restrito;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public boolean isRestrito() {
        return restrito;
    }
}
