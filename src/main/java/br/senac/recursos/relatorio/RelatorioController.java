package br.senac.recursos.relatorio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relatório de utilização (spec T-41, RF-12). Só o Responsável e o Administrador (regra de acesso). */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping("/utilizacao")
    RelatorioDeUtilizacao utilizacao(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim) {
        return relatorios.utilizacao(inicio, fim);
    }
}
