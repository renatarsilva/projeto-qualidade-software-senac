package br.senac.recursos.material;

public record MaterialResponse(Long id, String nome, String tipoMaterial, String patrimonio, boolean restrito) {

    static MaterialResponse de(Material material) {
        return new MaterialResponse(material.getId(), material.getNome(), material.getTipoMaterial(),
                material.getPatrimonio(), material.isRestrito());
    }
}
