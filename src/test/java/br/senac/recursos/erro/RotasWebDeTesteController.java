package br.senac.recursos.erro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Rotas de tela que só existem nos testes e falham de propósito (spec T-22, CA-05 e CA-14). */
@Controller
@RequestMapping("/teste-web")
class RotasWebDeTesteController {

    @GetMapping("/falha")
    String falha() {
        throw new RuntimeException("falha na tela: SELECT * FROM usuario em br.senac.recursos.seguranca");
    }

    @GetMapping("/conflito")
    String conflito() {
        throw new ConflitoDeNegocioException("Já existe um registro com esse valor");
    }

    /** O modelo da página falha durante a renderização. */
    @GetMapping("/renderizacao")
    String renderizacao() {
        return "teste-web-quebrado";
    }
}
