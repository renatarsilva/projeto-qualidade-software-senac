package br.senac.recursos.seguranca;

import java.util.Set;
import org.springframework.http.HttpMethod;

/**
 * Declara qual perfil pode chamar um caminho da API, opcionalmente só para um método HTTP.
 * Quem não declara nada é negado por padrão (spec T-15, CA-08).
 * Lista vazia de perfis significa "qualquer usuário autenticado".
 */
public record RegraDeAcesso(String padrao, Set<Perfil> perfis, HttpMethod metodo) {

    public static RegraDeAcesso soPara(String padrao, Perfil... perfis) {
        return new RegraDeAcesso(padrao, Set.of(perfis), null);
    }

    public static RegraDeAcesso soPara(HttpMethod metodo, String padrao, Perfil... perfis) {
        return new RegraDeAcesso(padrao, Set.of(perfis), metodo);
    }

    public static RegraDeAcesso qualquerAutenticado(String padrao) {
        return new RegraDeAcesso(padrao, Set.of(), null);
    }

    public static RegraDeAcesso qualquerAutenticado(HttpMethod metodo, String padrao) {
        return new RegraDeAcesso(padrao, Set.of(), metodo);
    }
}
