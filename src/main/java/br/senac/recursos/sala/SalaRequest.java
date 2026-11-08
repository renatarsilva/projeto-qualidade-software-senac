package br.senac.recursos.sala;

public record SalaRequest(String nome, String tipoSala, Integer capacidade, String localizacao, Boolean restrito) {
}
