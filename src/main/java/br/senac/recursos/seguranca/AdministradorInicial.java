package br.senac.recursos.seguranca;

/**
 * Garante que o sistema nunca fique sem Administrador (spec T-15, CA-16).
 * Se não houver nenhum, cria o primeiro com o e-mail e a senha vindos do ambiente; sem eles, recusa a subida.
 * Nenhuma senha fica no código nem na migração.
 */
public class AdministradorInicial {

    private final UsuarioRepository usuarios;
    private final CodificadorDeSenha codificador;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarios, CodificadorDeSenha codificador, String email, String senha) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.email = email;
        this.senha = senha;
    }

    public void garantirAdministrador() {
        if (usuarios.existsByPerfil(Perfil.ADMINISTRADOR)) {
            return; // já existe: as variáveis são ignoradas
        }
        if (vazio(email) || vazio(senha)) {
            throw new IllegalStateException("Não há nenhum Administrador cadastrado. "
                    + "Defina as variáveis de ambiente APP_ADMIN_EMAIL e APP_ADMIN_SENHA para criar o primeiro.");
        }
        usuarios.save(Usuario.novo("Administrador", email, codificador.codifica(senha), Perfil.ADMINISTRADOR));
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
