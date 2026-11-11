package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.reserva.ConsultaDeReservasService;
import br.senac.recursos.reserva.ReservaService;
import br.senac.recursos.retirada.RetiradaService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Telas do Responsável (spec T-44, CA-11 e CA-12): a fila de aprovação, a retirada e a devolução de materiais. */
@Controller
class AprovacoesTelas extends TelaBase {

    private final ReservaService reservas;
    private final ConsultaDeReservasService consultas;
    private final RetiradaService retiradas;
    private final Catalogo catalogo;
    private final DetalheDaReserva detalhe;

    AprovacoesTelas(ReservaService reservas, ConsultaDeReservasService consultas, RetiradaService retiradas,
            Catalogo catalogo, DetalheDaReserva detalhe) {
        this.reservas = reservas;
        this.consultas = consultas;
        this.retiradas = retiradas;
        this.catalogo = catalogo;
        this.detalhe = detalhe;
    }

    @GetMapping("/aprovacoes")
    String fila(Model modelo, Authentication autenticacao) {
        modelo.addAttribute("reservas", consultas.lista(usuario(autenticacao), perfil(autenticacao), "SOLICITADA"));
        modelo.addAttribute("nomes", catalogo.nomes());
        return "aprovacoes";
    }

    @PostMapping("/reservas/{id:\\d+}/aprovar")
    String aprova(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id) {
        return decide(modelo, autenticacao, aviso, id, "APROVADA", "Reserva #" + id + " aprovada.");
    }

    @PostMapping("/reservas/{id:\\d+}/rejeitar")
    String rejeita(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id) {
        return decide(modelo, autenticacao, aviso, id, "REJEITADA", "Reserva #" + id + " rejeitada.");
    }

    @PostMapping("/reservas/{id:\\d+}/retiradas")
    String retira(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id,
            @RequestParam(required = false) String materialId) {
        try {
            retiradas.retira(usuario(autenticacao), id, identificador("materialId", materialId));
            aviso.addFlashAttribute("sucesso", "Retirada registrada.");
            return "redirect:/reservas/" + id;
        } catch (RuntimeException erro) {
            return naoRealizada(erro, modelo, autenticacao, id);
        }
    }

    @PostMapping("/reservas/{id:\\d+}/retiradas/{retirada:\\d+}/devolucao")
    String devolve(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id,
            @PathVariable long retirada) {
        try {
            retiradas.devolve(id, retirada);
            aviso.addFlashAttribute("sucesso", "Devolução registrada.");
            return "redirect:/reservas/" + id;
        } catch (RuntimeException erro) {
            return naoRealizada(erro, modelo, autenticacao, id);
        }
    }

    private String decide(Model modelo, Authentication autenticacao, RedirectAttributes aviso, long id, String destino,
            String sucesso) {
        try {
            reservas.mudaEstado(usuario(autenticacao), id, destino);
            aviso.addFlashAttribute("sucesso", sucesso);
            return "redirect:/aprovacoes";
        } catch (RuntimeException erro) {
            if (!Mensagens.mostravel(erro)) {
                throw erro;
            }
            modelo.addAttribute("erro", Mensagens.de(erro));
            return fila(modelo, autenticacao);
        }
    }

    private String naoRealizada(RuntimeException erro, Model modelo, Authentication autenticacao, long id) {
        if (!Mensagens.mostravel(erro)) {
            throw erro;
        }
        modelo.addAttribute("erro", Mensagens.de(erro));
        return detalhe.monta(modelo, autenticacao, id);
    }

    private static Long identificador(String campo, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException erro) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", campo, "motivo", "valor inválido")));
        }
    }
}
