package br.senac.recursos.reserva;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato HTTP da reserva (spec T-20, seção 4). Esqueleto da T-20: responde 204 em tudo,
 * para os testes de contrato falharem por asserção até a T-23.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @PostMapping
    ResponseEntity<Void> cria(@RequestBody String corpo) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> altera(@PathVariable long id, @RequestBody String corpo) {
        return ResponseEntity.noContent().build();
    }
}
