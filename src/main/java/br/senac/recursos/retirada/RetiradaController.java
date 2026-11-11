package br.senac.recursos.retirada;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Retirada e devolução de materiais (spec T-42, RF-09). Só o Responsável registra (DP-04, regra de acesso). */
@RestController
@RequestMapping("/api/reservas/{reservaId}/retiradas")
public class RetiradaController {

    private final RetiradaService retiradas;

    public RetiradaController(RetiradaService retiradas) {
        this.retiradas = retiradas;
    }

    @PostMapping
    ResponseEntity<RetiradaResponse> retira(Authentication autenticacao, @PathVariable long reservaId,
            @RequestBody RetiradaRequest corpo) {
        RetiradaResponse retirada = retiradas.retira(Long.parseLong(autenticacao.getName()), reservaId,
                corpo.materialId());
        return ResponseEntity.status(HttpStatus.CREATED).body(retirada);
    }

    @PostMapping("/{retiradaId}/devolucao")
    RetiradaResponse devolve(@PathVariable long reservaId, @PathVariable long retiradaId) {
        return retiradas.devolve(reservaId, retiradaId);
    }

    @GetMapping
    List<RetiradaResponse> lista(@PathVariable long reservaId) {
        return retiradas.lista(reservaId);
    }
}
