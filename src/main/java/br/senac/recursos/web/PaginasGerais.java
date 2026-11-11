package br.senac.recursos.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entrada do sistema: a página inicial e o formulário de login (spec T-44, CA-02). */
@Controller
class PaginasGerais {

    @GetMapping("/")
    String inicio() {
        return "inicio";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
