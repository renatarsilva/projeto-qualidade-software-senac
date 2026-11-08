package br.senac.recursos.seguranca;

/** O que a API devolve de um usuário. Nunca inclui a senha nem o hash (spec T-15, CA-09 e CA-10). */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.isAtivo());
    }
}
