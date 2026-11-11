package br.senac.recursos.web;

import br.senac.recursos.pesquisa.PesquisaDeRecursosService;
import br.senac.recursos.pesquisa.RecursoPesquisado;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Os recursos cadastrados, para montar os formulários e mostrar o nome de cada recurso de uma reserva. */
@Component
class Catalogo {

    private final PesquisaDeRecursosService pesquisa;

    Catalogo(PesquisaDeRecursosService pesquisa) {
        this.pesquisa = pesquisa;
    }

    List<RecursoPesquisado> todos() {
        return pesquisa.pesquisa(null, null, null, null, null, null);
    }

    /** "SALA:3" → "sala-102". */
    Map<String, String> nomes() {
        Map<String, String> nomes = new LinkedHashMap<>();
        todos().forEach(r -> nomes.put(r.tipo() + ":" + r.id(), r.nome()));
        return nomes;
    }
}
