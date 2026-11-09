package br.senac.recursos.material;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {

    private final MaterialService servico;

    public MaterialController(MaterialService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> cadastra(@RequestBody MaterialRequest pedido) {
        MaterialResponse material = servico.cadastra(pedido);
        return ResponseEntity.created(URI.create("/api/materiais/" + material.id())).body(material);
    }

    @PutMapping("/{id}")
    public MaterialResponse altera(@PathVariable long id, @RequestBody MaterialRequest pedido) {
        return servico.altera(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> exclui(@PathVariable long id) {
        servico.exclui(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public MaterialResponse busca(@PathVariable long id) {
        return servico.busca(id);
    }

    @GetMapping
    public List<MaterialResponse> lista() {
        return servico.lista();
    }
}
