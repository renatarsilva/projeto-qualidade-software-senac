package br.senac.recursos.relatorio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Relatório de utilização (spec T-41, RF-12). Esqueleto: responde 204 até a implementação. */
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    @GetMapping("/utilizacao")
    ResponseEntity<Void> utilizacao() {
        return ResponseEntity.noContent().build();
    }
}
