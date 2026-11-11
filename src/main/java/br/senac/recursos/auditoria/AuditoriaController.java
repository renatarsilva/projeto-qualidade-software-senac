package br.senac.recursos.auditoria;

import br.senac.recursos.erro.RecursoNaoEncontradoException;
import br.senac.recursos.reserva.AgendaDeReservas;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Histórico de auditoria de uma reserva (spec T-40). Só leitura: não existe endpoint de escrita. */
@RestController
@RequestMapping("/api/reservas/{id}/auditoria")
public class AuditoriaController {

    private final AuditoriaDeReservas auditoria;
    private final AgendaDeReservas agenda;

    public AuditoriaController(AuditoriaDeReservas auditoria, AgendaDeReservas agenda) {
        this.auditoria = auditoria;
        this.agenda = agenda;
    }

    @GetMapping
    List<RegistroDeAuditoria> historico(@PathVariable long id) {
        if (agenda.busca(id).isEmpty()) {
            throw new RecursoNaoEncontradoException();
        }
        return auditoria.historico(id);
    }
}
