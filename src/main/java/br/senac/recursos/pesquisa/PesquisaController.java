package br.senac.recursos.pesquisa;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade (spec T-43, RF-03). */
@RestController
@RequestMapping("/api/recursos")
public class PesquisaController {

    private final PesquisaDeRecursosService pesquisa;

    public PesquisaController(PesquisaDeRecursosService pesquisa) {
        this.pesquisa = pesquisa;
    }

    @GetMapping
    List<RecursoPesquisado> pesquisa(@RequestParam(required = false) String tipo,
            @RequestParam(required = false) String capacidadeMinima,
            @RequestParam(required = false) String localizacao, @RequestParam(required = false) String competencia,
            @RequestParam(required = false) String disponivelDe,
            @RequestParam(required = false) String disponivelAte) {
        return pesquisa.pesquisa(tipo, capacidadeMinima, localizacao, competencia, disponivelDe, disponivelAte);
    }
}
