package br.senac.recursos.manutencao;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Períodos de manutenção de um recurso (spec T-35, RF-08). Só o Administrador (regra de acesso). */
@RestController
@RequestMapping("/api/recursos/{recursoId}/manutencoes")
public class ManutencaoController {

    private final ManutencaoService manutencoes;

    public ManutencaoController(ManutencaoService manutencoes) {
        this.manutencoes = manutencoes;
    }

    @PostMapping
    ResponseEntity<ManutencaoResponse> registra(@PathVariable long recursoId, @RequestBody ManutencaoRequest corpo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manutencoes.registra(recursoId, corpo));
    }

    @GetMapping
    List<ManutencaoResponse> lista(@PathVariable long recursoId) {
        return manutencoes.lista(recursoId);
    }

    @DeleteMapping("/{manutencaoId}")
    ResponseEntity<Void> encerra(@PathVariable long recursoId, @PathVariable long manutencaoId) {
        manutencoes.encerra(recursoId, manutencaoId);
        return ResponseEntity.noContent().build();
    }
}
