package br.senac.recursos.reserva;

import br.senac.recursos.erro.EntradaInvalidaException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contrato HTTP da reserva (spec T-20, seção 4). O dono é sempre o usuário autenticado. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
    }

    @PostMapping
    ResponseEntity<ReservaResponse> cria(Authentication autenticacao, @RequestBody ReservaRequest corpo) {
        ReservaResponse criada = reservas.cria(donoDe(autenticacao), pedidoDe(corpo));
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PutMapping("/{id}")
    ResponseEntity<ReservaResponse> altera(Authentication autenticacao, @PathVariable long id,
            @RequestBody ReservaRequest corpo) {
        return ResponseEntity.ok(reservas.altera(donoDe(autenticacao), id, pedidoDe(corpo)));
    }

    // Esqueletos da T-21: respondem 204 em tudo, para os testes de contrato falharem por asserção até a T-24.

    @PatchMapping("/{id}/estado")
    ResponseEntity<Void> mudaEstado(@PathVariable long id, @RequestBody String corpo) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancelamento")
    ResponseEntity<Void> cancela(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/checkin")
    ResponseEntity<Void> checkin(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> apaga(@PathVariable long id) {
        return ResponseEntity.noContent().build();
    }

    private static long donoDe(Authentication autenticacao) {
        return Long.parseLong(autenticacao.getName());
    }

    private static PedidoDeReserva pedidoDe(ReservaRequest corpo) {
        List<ReservaRequest.RecursoRequest> recebidos = corpo.recursos() == null ? List.of() : corpo.recursos();
        if (recebidos.stream().anyMatch(r -> r == null || r.tipo() == null || r.id() == null)) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "recursos", "motivo", "valor inválido")));
        }
        List<RecursoPedido> recursos = recebidos.stream().map(r -> new RecursoPedido(r.tipo(), r.id())).toList();
        return new PedidoDeReserva(corpo.inicio(), corpo.termino(), recursos);
    }
}
