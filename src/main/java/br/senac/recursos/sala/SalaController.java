package br.senac.recursos.sala;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private final SalaService servico;

    public SalaController(SalaService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<SalaResponse> cadastra(@Valid @RequestBody SalaRequest pedido) {
        Sala sala = servico.cadastra(pedido);
        return ResponseEntity.created(URI.create("/api/salas/" + sala.getId())).body(SalaResponse.de(sala));
    }
}
