package br.senac.recursos.pesquisa;

import java.util.List;

/** Um recurso na resposta da pesquisa. Capacidade e localização só existem em sala; competências, em professor. */
public record RecursoPesquisado(long id, String tipo, String nome, boolean restrito, Integer capacidade,
        String localizacao, List<String> competencias) {
}
