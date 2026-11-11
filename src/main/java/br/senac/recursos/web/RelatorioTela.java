package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.relatorio.RelatorioService;
import java.time.Instant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Relatório de utilização por período (spec T-44, CA-14). As datas são inclusivas e em horário de Brasília. */
@Controller
class RelatorioTela extends TelaBase {

    private final RelatorioService relatorio;

    RelatorioTela(RelatorioService relatorio) {
        this.relatorio = relatorio;
    }

    @GetMapping("/relatorio")
    String relatorio(Model modelo, @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim) {
        modelo.addAttribute("inicio", inicio == null ? "" : inicio);
        modelo.addAttribute("fim", fim == null ? "" : fim);
        if (ou(inicio) != null || ou(fim) != null) {
            try {
                Instant de = Fuso.inicioDoDia("inicio", inicio);
                Instant ate = Fuso.fimDoDia("fim", fim);
                modelo.addAttribute("resultado", relatorio.utilizacao(de.toString(), ate.toString()));
            } catch (EntradaInvalidaException erro) {
                modelo.addAttribute("erro", Mensagens.de(erro));
            }
        }
        return "relatorio";
    }
}
