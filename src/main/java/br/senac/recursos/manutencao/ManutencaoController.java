package br.senac.recursos.manutencao;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Períodos de manutenção de um recurso (spec T-35). Esqueleto: responde 204 em tudo até a implementação. */
@RestController
@RequestMapping("/api/recursos/{recursoId}/manutencoes")
public class ManutencaoController {

    @PostMapping
    ResponseEntity<Void> registra(@PathVariable long recursoId, @RequestBody String corpo) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    ResponseEntity<Void> lista(@PathVariable long recursoId) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{manutencaoId}")
    ResponseEntity<Void> encerra(@PathVariable long recursoId, @PathVariable long manutencaoId) {
        return ResponseEntity.noContent().build();
    }
}
