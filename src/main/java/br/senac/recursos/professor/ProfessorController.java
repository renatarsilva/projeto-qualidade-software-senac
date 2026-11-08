package br.senac.recursos.professor;

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
@RequestMapping("/api/professores")
public class ProfessorController {

    private final ProfessorService servico;

    public ProfessorController(ProfessorService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<ProfessorResponse> cadastra(@RequestBody ProfessorRequest pedido) {
        ProfessorResponse professor = servico.cadastra(pedido);
        return ResponseEntity.created(URI.create("/api/professores/" + professor.id())).body(professor);
    }

    @PutMapping("/{id}")
    public ProfessorResponse altera(@PathVariable long id, @RequestBody ProfessorRequest pedido) {
        return servico.altera(id, pedido);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> exclui(@PathVariable long id) {
        servico.exclui(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ProfessorResponse busca(@PathVariable long id) {
        return servico.busca(id);
    }

    @GetMapping
    public List<ProfessorResponse> lista() {
        return servico.lista();
    }
}
