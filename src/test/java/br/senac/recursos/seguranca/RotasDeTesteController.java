package br.senac.recursos.seguranca;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas que existem só nos testes (ficam em src/test), para a T-15 provar a autenticação e a autorização
 * sem depender das outras funcionalidades (spec T-15, seção 6).
 */
@RestController
@RequestMapping("/api/teste")
class RotasDeTesteController {

    /** Qualquer usuário autenticado pode chamar. */
    @GetMapping("/autenticado")
    String autenticado() {
        return "ok";
    }

    /** Só o Administrador pode chamar. */
    @GetMapping("/administrador")
    String administrador() {
        return "ok";
    }

    /** Ninguém declarou regra para esta rota: o padrão é negar (CA-08). */
    @GetMapping("/sem-regra")
    String semRegra() {
        return "ok";
    }
}
