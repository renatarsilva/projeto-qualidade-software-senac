package br.senac.recursos.material;

import br.senac.recursos.sala.Recurso;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "material")
@DiscriminatorValue("MATERIAL")
@PrimaryKeyJoinColumn(name = "recurso_id")
public class Material extends Recurso {

    @Column(name = "tipo_material", nullable = false, length = 100)
    private String tipoMaterial;

    @Column(nullable = false, length = 30)
    private String patrimonio;

    protected Material() {
        // exigido pelo JPA
    }

    public Material(String nome, boolean restrito, String tipoMaterial, String patrimonio) {
        super(nome, restrito);
        this.tipoMaterial = tipoMaterial;
        this.patrimonio = patrimonio;
    }

    public void atualiza(String nome, boolean restrito, String tipoMaterial, String patrimonio) {
        atualizaBase(nome, restrito);
        this.tipoMaterial = tipoMaterial;
        this.patrimonio = patrimonio;
    }

    public String getTipoMaterial() {
        return tipoMaterial;
    }

    public String getPatrimonio() {
        return patrimonio;
    }
}
