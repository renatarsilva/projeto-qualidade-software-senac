package br.senac.recursos.retirada;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Retirada e devolução de materiais (spec T-42, RF-09). Esqueleto: responde 204 em tudo até a implementação. */
@RestController
@RequestMapping("/api/reservas/{reservaId}/retiradas")
public class RetiradaController {

    @PostMapping
    ResponseEntity<Void> retira(@PathVariable long reservaId, @RequestBody String corpo) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{retiradaId}/devolucao")
    ResponseEntity<Void> devolve(@PathVariable long reservaId, @PathVariable long retiradaId) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    ResponseEntity<Void> lista(@PathVariable long reservaId) {
        return ResponseEntity.noContent().build();
    }
}
