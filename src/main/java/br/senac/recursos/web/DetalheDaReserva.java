package br.senac.recursos.web;

import br.senac.recursos.auditoria.AuditoriaDeReservas;
import br.senac.recursos.reserva.ConsultaDeReservasService;
import br.senac.recursos.reserva.RecursoPedido;
import br.senac.recursos.reserva.ReservaResponse;
import br.senac.recursos.reserva.TipoDeRecurso;
import br.senac.recursos.retirada.RetiradaResponse;
import br.senac.recursos.retirada.RetiradaService;
import br.senac.recursos.seguranca.Perfil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Monta a tela da reserva: os dados, os materiais com a retirada e a devolução, e o histórico de auditoria. A posse
 * (o Solicitante só abre a própria reserva) é conferida pelo serviço de consulta, o mesmo da API (NC-01).
 */
@Component
class DetalheDaReserva {

    /** Um material da reserva e a retirada dele, se já houve. */
    record LinhaDeMaterial(long materialId, String nome, RetiradaResponse retirada) {
    }

    private final ConsultaDeReservasService consultas;
    private final RetiradaService retiradas;
    private final AuditoriaDeReservas auditoria;
    private final Catalogo catalogo;

    DetalheDaReserva(ConsultaDeReservasService consultas, RetiradaService retiradas, AuditoriaDeReservas auditoria,
            Catalogo catalogo) {
        this.consultas = consultas;
        this.retiradas = retiradas;
        this.auditoria = auditoria;
        this.catalogo = catalogo;
    }

    String monta(Model modelo, Authentication autenticacao, long id) {
        Perfil perfil = TelaBase.perfil(autenticacao);
        ReservaResponse reserva = consultas.consulta(TelaBase.usuario(autenticacao), perfil, id);
        Map<String, String> nomes = catalogo.nomes();
        modelo.addAttribute("reserva", reserva);
        modelo.addAttribute("nomes", nomes);
        boolean gestor = perfil != Perfil.SOLICITANTE;
        modelo.addAttribute("gestor", gestor);
        if (gestor) {
            List<RetiradaResponse> feitas = retiradas.lista(id);
            List<LinhaDeMaterial> materiais = new ArrayList<>();
            for (RecursoPedido recurso : reserva.recursos()) {
                if (recurso.tipo() == TipoDeRecurso.MATERIAL) {
                    RetiradaResponse retirada = feitas.stream().filter(r -> r.materialId() == recurso.id()).findFirst()
                            .orElse(null);
                    materiais.add(new LinhaDeMaterial(recurso.id(), nomes.get("MATERIAL:" + recurso.id()), retirada));
                }
            }
            modelo.addAttribute("materiais", materiais);
            modelo.addAttribute("historico", auditoria.historico(id));
        }
        return "reserva";
    }
}
