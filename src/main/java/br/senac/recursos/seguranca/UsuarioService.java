package br.senac.recursos.seguranca;

import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.Conflito;
import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.EntradaInvalida;
import br.senac.recursos.seguranca.GestaoDeUsuariosExceptions.NaoEncontrado;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    static final String MENSAGEM_DA_SENHA =
            "A senha deve ter de 8 a 72 caracteres (acentos e símbolos contam mais)";
    static final String SEM_ADMINISTRADOR = "O sistema precisa de pelo menos um administrador";

    private final UsuarioRepository usuarios;
    private final Validator validador;
    private final PoliticaDeSenha politicaDeSenha = new PoliticaDeSenha();
    private final CodificadorDeSenha codificador = new CodificadorDeSenha();

    public UsuarioService(UsuarioRepository usuarios, Validator validador) {
        this.usuarios = usuarios;
        this.validador = validador;
    }

    @Transactional
    public Usuario cria(UsuarioRequest pedido) {
        List<Map<String, String>> erros = errosDe(pedido);
        if (pedido.senha() != null && !pedido.senha().isBlank() && !politicaDeSenha.valida(pedido.senha())) {
            erros.add(Map.of("campo", "senha", "motivo", MENSAGEM_DA_SENHA));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalida(erros);
        }
        if (usuarios.existsByEmail(pedido.email())) {
            throw new Conflito("Já existe um usuário com esse e-mail");
        }
        return usuarios.save(Usuario.novo(pedido.nome(), pedido.email(), codificador.codifica(pedido.senha()),
                pedido.perfil()));
    }

    @Transactional(readOnly = true)
    public List<Usuario> lista() {
        return usuarios.findAll();
    }

    @Transactional
    public Usuario trocaPerfil(long id, PerfilRequest pedido) {
        Usuario usuario = usuarios.findById(id).orElseThrow(NaoEncontrado::new);
        exigeValido(pedido);
        if (pedido.perfil() != Perfil.ADMINISTRADOR) {
            garanteOutroAdministradorAtivo(usuario);
        }
        usuario.trocaPerfil(pedido.perfil());
        return usuario;
    }

    @Transactional
    public Usuario defineAtivo(long id, AtivoRequest pedido) {
        Usuario usuario = usuarios.findById(id).orElseThrow(NaoEncontrado::new);
        exigeValido(pedido);
        if (!pedido.ativo()) {
            garanteOutroAdministradorAtivo(usuario);
        }
        usuario.defineAtivo(pedido.ativo());
        return usuario;
    }

    /** O sistema nunca fica sem administrador ativo (CA-14, P-04). */
    private void garanteOutroAdministradorAtivo(Usuario usuario) {
        boolean eAdministradorAtivo = usuario.getPerfil() == Perfil.ADMINISTRADOR && usuario.isAtivo();
        if (eAdministradorAtivo && usuarios.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR) <= 1) {
            throw new Conflito(SEM_ADMINISTRADOR);
        }
    }

    private void exigeValido(Object pedido) {
        List<Map<String, String>> erros = errosDe(pedido);
        if (!erros.isEmpty()) {
            throw new EntradaInvalida(erros);
        }
    }

    private <T> List<Map<String, String>> errosDe(T pedido) {
        Set<ConstraintViolation<T>> violacoes = validador.validate(pedido);
        List<Map<String, String>> erros = new ArrayList<>();
        violacoes.stream()
                .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                        .thenComparing(ConstraintViolation::getMessage))
                .forEach(v -> erros.add(Map.of("campo", v.getPropertyPath().toString(), "motivo", v.getMessage())));
        return erros;
    }
}
