package br.senac.recursos.auditoria;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de auditoria de uma reserva (spec T-40). Esqueleto: responde 204 até a implementação. */
@RestController
@RequestMapping("/api/reservas/{id}/auditoria")
public class AuditoriaController {

    @GetMapping
    ResponseEntity<Void> historico(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }
}
