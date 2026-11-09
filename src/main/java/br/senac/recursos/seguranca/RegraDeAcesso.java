package br.senac.recursos.seguranca;

import java.util.Set;

/**
 * Declara qual perfil pode chamar um caminho da API. Quem não declara nada é negado por padrão (spec T-15, CA-08).
 * Lista vazia de perfis significa "qualquer usuário autenticado".
 */
public record RegraDeAcesso(String padrao, Set<Perfil> perfis) {

    public static RegraDeAcesso soPara(String padrao, Perfil... perfis) {
        return new RegraDeAcesso(padrao, Set.of(perfis));
    }

    public static RegraDeAcesso qualquerAutenticado(String padrao) {
        return new RegraDeAcesso(padrao, Set.of());
    }
}
