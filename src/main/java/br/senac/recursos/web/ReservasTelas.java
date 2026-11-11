package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.pesquisa.PesquisaDeRecursosService;
import br.senac.recursos.pesquisa.RecursoPesquisado;
import br.senac.recursos.reserva.ConsultaDeReservasService;
import br.senac.recursos.reserva.PedidoDeReserva;
import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.ReservaResponse;
import br.senac.recursos.reserva.ReservaService;
import br.senac.recursos.reserva.TipoDeRecurso;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Pesquisa de recursos e reservas pela tela (spec T-44, CA-07 a CA-10). Não tem regra própria: chama os serviços da
 * API, então conflito, antecedência, posse e ciclo de vida valem aqui como lá.
 */
@Controller
class ReservasTelas extends TelaBase {

    private final PesquisaDeRecursosService pesquisa;
    private final ReservaService reservas;
    private final ConsultaDeReservasService consultas;
    private final Catalogo catalogo;
    private final DetalheDaReserva detalhe;

    ReservasTelas(PesquisaDeRecursosService pesquisa, ReservaService reservas, ConsultaDeReservasService consultas,
            Catalogo catalogo, DetalheDaReserva detalhe) {
        this.pesquisa = pesquisa;
        this.reservas = reservas;
        this.consultas = consultas;
        this.catalogo = catalogo;
        this.detalhe = detalhe;
    }

    @GetMapping("/recursos")
    String pesquisa(Model modelo, @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String capacidadeMinima,
            @RequestParam(required = false) String localizacao, @RequestParam(required = false) String competencia,
            @RequestParam(required = false) String de, @RequestParam(required = false) String ate) {
        modelo.addAttribute("filtro", Map.of("tipo", nulo(tipo), "capacidadeMinima", nulo(capacidadeMinima),
                "localizacao", nulo(localizacao), "competencia", nulo(competencia), "de", nulo(de), "ate", nulo(ate)));
        List<RecursoPesquisado> resultado = List.of();
        try {
            Instant inicio = Fuso.instante("de", de);
            Instant fim = Fuso.instante("ate", ate);
            resultado = pesquisa.pesquisa(ou(tipo), ou(capacidadeMinima), ou(localizacao), ou(competencia),
                    inicio == null ? null : inicio.toString(), fim == null ? null : fim.toString());
        } catch (EntradaInvalidaException erro) {
            modelo.addAttribute("erro", Mensagens.de(erro));
        }
        modelo.addAttribute("resultado", resultado);
        return "recursos";
    }

    @GetMapping("/reservas")
    String lista(Model modelo, Authentication autenticacao, @RequestParam(required = false) String estado) {
        try {
            modelo.addAttribute("reservas", consultas.lista(usuario(autenticacao), perfil(autenticacao), ou(estado)));
        } catch (EntradaInvalidaException erro) {
            modelo.addAttribute("erro", Mensagens.de(erro));
            modelo.addAttribute("reservas", List.<ReservaResponse>of());
        }
        modelo.addAttribute("nomes", catalogo.nomes());
        modelo.addAttribute("estado", nulo(estado));
        return "reservas";
    }

    @GetMapping("/reservas/{id:\\d+}")
    String detalhe(Model modelo, Authentication autenticacao, @PathVariable long id) {
        return detalhe.monta(modelo, autenticacao, id);
    }

    @GetMapping("/reservas/nova")
    String nova(Model modelo, @RequestParam(name = "recurso", required = false) List<String> recursos,
            @RequestParam(required = false) String inicio, @RequestParam(required = false) String termino) {
        return formulario(modelo, "Nova reserva", "/reservas", nulo(inicio), nulo(termino),
                recursos == null ? Set.of() : new LinkedHashSet<>(recursos));
    }

    @PostMapping("/reservas")
    String cria(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @RequestParam String inicio,
            @RequestParam String termino, @RequestParam(name = "recursos", required = false) List<String> recursos) {
        try {
            ReservaResponse criada = reservas.cria(usuario(autenticacao), pedido(inicio, termino, recursos));
            aviso.addFlashAttribute("sucesso", "Reserva solicitada. Ela precisa ser aprovada pelo Responsável"
                    + (criada.aviso() == null ? "." : ". " + criada.aviso()));
            return "redirect:/reservas/" + criada.id();
        } catch (RuntimeException erro) {
            return recusada(erro, modelo, "Nova reserva", "/reservas", inicio, termino, recursos);
        }
    }

    @GetMapping("/reservas/{id:\\d+}/editar")
    String edita(Model modelo, Authentication autenticacao, @PathVariable long id) {
        ReservaResponse reserva = consultas.consulta(usuario(autenticacao), perfil(autenticacao), id);
        Set<String> selecionados = new LinkedHashSet<>();
        reserva.recursos().forEach(r -> selecionados.add(r.tipo() + ":" + r.id()));
        return formulario(modelo, "Alterar a reserva #" + id, "/reservas/" + id + "/editar", campo(reserva.inicio()),
                campo(reserva.termino()), selecionados);
    }

    @PostMapping("/reservas/{id:\\d+}/editar")
    String altera(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id,
            @RequestParam String inicio, @RequestParam String termino,
            @RequestParam(name = "recursos", required = false) List<String> recursos) {
        try {
            reservas.altera(usuario(autenticacao), id, pedido(inicio, termino, recursos));
            aviso.addFlashAttribute("sucesso", "Reserva alterada.");
            return "redirect:/reservas/" + id;
        } catch (RuntimeException erro) {
            return recusada(erro, modelo, "Alterar a reserva #" + id, "/reservas/" + id + "/editar", inicio, termino,
                    recursos);
        }
    }

    @PostMapping("/reservas/{id:\\d+}/cancelar")
    String cancela(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id) {
        try {
            reservas.cancela(usuario(autenticacao), id);
            aviso.addFlashAttribute("sucesso", "Reserva cancelada.");
            return "redirect:/reservas/" + id;
        } catch (RuntimeException erro) {
            return naoRealizada(erro, modelo, autenticacao, id);
        }
    }

    @PostMapping("/reservas/{id:\\d+}/checkin")
    String checkin(Model modelo, Authentication autenticacao, RedirectAttributes aviso, @PathVariable long id) {
        try {
            reservas.checkin(usuario(autenticacao), id);
            aviso.addFlashAttribute("sucesso", "Check-in registrado. Bom uso!");
            return "redirect:/reservas/" + id;
        } catch (RuntimeException erro) {
            return naoRealizada(erro, modelo, autenticacao, id);
        }
    }

    private String naoRealizada(RuntimeException erro, Model modelo, Authentication autenticacao, long id) {
        if (!Mensagens.mostravel(erro)) {
            throw erro;
        }
        modelo.addAttribute("erro", Mensagens.de(erro));
        return detalhe.monta(modelo, autenticacao, id);
    }

    private String recusada(RuntimeException erro, Model modelo, String titulo, String acao, String inicio,
            String termino, List<String> recursos) {
        if (!Mensagens.mostravel(erro)) {
            throw erro;
        }
        modelo.addAttribute("erro", Mensagens.de(erro));
        return formulario(modelo, titulo, acao, inicio, termino,
                recursos == null ? Set.of() : new LinkedHashSet<>(recursos));
    }

    private String formulario(Model modelo, String titulo, String acao, String inicio, String termino,
            Set<String> selecionados) {
        modelo.addAttribute("tituloDaPagina", titulo);
        modelo.addAttribute("acao", acao);
        modelo.addAttribute("inicio", inicio);
        modelo.addAttribute("termino", termino);
        modelo.addAttribute("selecionados", selecionados);
        modelo.addAttribute("recursos", catalogo.todos());
        return "reserva-form";
    }

    private static PedidoDeReserva pedido(String inicio, String termino, List<String> recursos) {
        List<RecursoPedido> pedidos = new ArrayList<>();
        for (String valor : recursos == null ? List.<String>of() : recursos) {
            pedidos.add(recurso(valor));
        }
        return new PedidoDeReserva(Fuso.instante("inicio", inicio), Fuso.instante("termino", termino), pedidos);
    }

    private static RecursoPedido recurso(String valor) {
        try {
            String[] partes = valor.split(":");
            return new RecursoPedido(TipoDeRecurso.valueOf(partes[0]), Long.parseLong(partes[1]));
        } catch (RuntimeException erro) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "recursos", "motivo", "valor inválido")));
        }
    }

    private static String campo(Instant instante) {
        return new Apresentacao().campo(instante);
    }

    private static String nulo(String texto) {
        return texto == null ? "" : texto;
    }
}
