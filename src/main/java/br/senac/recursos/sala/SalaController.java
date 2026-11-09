package br.senac.recursos.sala;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<SalaResponse> cadastra(@RequestBody SalaRequest pedido) {
        Sala sala = servico.cadastra(pedido);
        return ResponseEntity.created(URI.create("/api/salas/" + sala.getId())).body(SalaResponse.de(sala));
    }

    @PutMapping("/{id}")
    public SalaResponse altera(@PathVariable long id, @RequestBody SalaRequest pedido) {
        return SalaResponse.de(servico.altera(id, pedido));
    }

    @GetMapping("/{id}")
    public SalaResponse busca(@PathVariable long id) {
        return SalaResponse.de(servico.busca(id));
    }

    @GetMapping
    public List<SalaResponse> lista() {
        return servico.lista().stream().map(SalaResponse::de).toList();
    }
}
