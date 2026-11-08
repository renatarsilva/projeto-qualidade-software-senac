package br.senac.recursos.sala;

public record SalaResponse(Long id, String nome, String tipoSala, int capacidade, String localizacao,
        boolean restrito) {

    static SalaResponse de(Sala sala) {
        return new SalaResponse(sala.getId(), sala.getNome(), sala.getTipoSala(), sala.getCapacidade(),
                sala.getLocalizacao(), sala.isRestrito());
    }
}
