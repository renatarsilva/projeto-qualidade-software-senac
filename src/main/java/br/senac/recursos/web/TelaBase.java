package br.senac.recursos.web;

import br.senac.recursos.erro.AcessoNegadoException;
import br.senac.recursos.seguranca.Perfil;
import org.springframework.security.core.Authentication;

/** O que toda tela precisa saber da sessão: quem é o usuário (o id) e o perfil (da permissão ROLE_<perfil>). */
abstract class TelaBase {

    static long usuario(Authentication autenticacao) {
        return Long.parseLong(autenticacao.getName());
    }

    static Perfil perfil(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream().map(a -> a.getAuthority()).filter(a -> a.startsWith("ROLE_"))
                .map(a -> Perfil.valueOf(a.substring(5))).findFirst().orElseThrow(AcessoNegadoException::new);
    }

    /** Valor opcional de um formulário: texto em branco vale "não informado". */
    static String ou(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
