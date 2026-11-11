package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.erro.RecursoNaoEncontradoException;
import br.senac.recursos.manutencao.ManutencaoRequest;
import br.senac.recursos.manutencao.ManutencaoResponse;
import br.senac.recursos.manutencao.ManutencaoService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Períodos de manutenção, só do Administrador (spec T-44, CA-14). Reservas afetadas voltam na própria URL. */
@Controller
class ManutencaoTelas extends TelaBase {

    private final ManutencaoService manutencoes;
    private final Catalogo catalogo;

    ManutencaoTelas(ManutencaoService manutencoes, Catalogo catalogo) {
        this.manutencoes = manutencoes;
        this.catalogo = catalogo;
    }

    @GetMapping("/admin/manutencoes")
    String lista(Model modelo, @RequestParam(name = "recurso", required = false) Long recurso,
            @RequestParam(required = false) String afetadas) {
        return pagina(modelo, recurso, afetadas, "", "", "");
    }

    @PostMapping("/admin/manutencoes")
    String registra(Model modelo, RedirectAttributes aviso, @RequestParam(required = false) Long recursoId,
            @RequestParam String inicio, @RequestParam String termino, @RequestParam String motivo) {
        try {
            if (recursoId == null) {
                throw new EntradaInvalidaException(List.of(Map.of("campo", "recursos", "motivo", "deve ser informado")));
            }
            ManutencaoResponse criada = manutencoes.registra(recursoId,
                    new ManutencaoRequest(Fuso.instante("inicio", inicio), Fuso.instante("termino", termino), motivo));
            aviso.addFlashAttribute("sucesso", "Período de manutenção registrado.");
            String afetadas = criada.reservasAfetadas() == null || criada.reservasAfetadas().isEmpty() ? ""
                    : "&afetadas=" + criada.reservasAfetadas().stream().map(String::valueOf)
                            .collect(Collectors.joining(","));
            return "redirect:/admin/manutencoes?recurso=" + recursoId + afetadas;
        } catch (RuntimeException erro) {
            if (!Mensagens.mostravel(erro)) {
                throw erro;
            }
            modelo.addAttribute("erro", Mensagens.de(erro));
            return pagina(modelo, recursoId, null, inicio, termino, motivo);
        }
    }

    @PostMapping("/admin/manutencoes/{id:\\d+}/encerrar")
    String encerra(RedirectAttributes aviso, @PathVariable long id, @RequestParam long recursoId) {
        manutencoes.encerra(recursoId, id);
        aviso.addFlashAttribute("sucesso", "Manutenção encerrada. O recurso está liberado.");
        return "redirect:/admin/manutencoes?recurso=" + recursoId;
    }

    private String pagina(Model modelo, Long recurso, String afetadas, String inicio, String termino, String motivo) {
        List<ManutencaoResponse> periodos = List.of();
        if (recurso != null) {
            try {
                periodos = manutencoes.lista(recurso);
            } catch (RecursoNaoEncontradoException erro) {
                modelo.addAttribute("erro", "Recurso não encontrado.");
            }
        }
        modelo.addAttribute("recursos", catalogo.todos());
        modelo.addAttribute("recursoEscolhido", recurso);
        modelo.addAttribute("periodos", periodos);
        modelo.addAttribute("afetadas", afetadas != null && afetadas.matches("[0-9,]+")
                ? List.of(afetadas.split(",")) : List.of());
        modelo.addAttribute("inicio", inicio);
        modelo.addAttribute("termino", termino);
        modelo.addAttribute("motivo", motivo);
        return "manutencoes";
    }
}
