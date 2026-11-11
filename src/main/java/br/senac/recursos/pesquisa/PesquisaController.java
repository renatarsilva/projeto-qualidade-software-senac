package br.senac.recursos.pesquisa;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade (spec T-43, RF-03).
 * Esqueleto da T-43: responde 204 em tudo, para os testes falharem por asserção até a implementação.
 */
@RestController
@RequestMapping("/api/recursos")
public class PesquisaController {

    @GetMapping
    ResponseEntity<Void> pesquisa() {
        return ResponseEntity.noContent().build();
    }
}
