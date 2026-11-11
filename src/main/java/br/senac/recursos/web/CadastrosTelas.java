package br.senac.recursos.web;

import br.senac.recursos.erro.EntradaInvalidaException;
import br.senac.recursos.material.MaterialRequest;
import br.senac.recursos.material.MaterialService;
import br.senac.recursos.professor.ProfessorRequest;
import br.senac.recursos.professor.ProfessorService;
import br.senac.recursos.sala.SalaRequest;
import br.senac.recursos.sala.SalaService;
import br.senac.recursos.seguranca.AtivoRequest;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.PerfilRequest;
import br.senac.recursos.seguranca.UsuarioRequest;
import br.senac.recursos.seguranca.UsuarioService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Cadastros do Administrador (spec T-44, CA-13): salas, materiais, professores e usuários. A tela lista e cadastra;
 * a validação e as regras (nome repetido, padrão do nome, política de senha, último administrador) são as dos serviços.
 */
@Controller
class CadastrosTelas extends TelaBase {

    private final SalaService salas;
    private final MaterialService materiais;
    private final ProfessorService professores;
    private final UsuarioService usuarios;

    CadastrosTelas(SalaService salas, MaterialService materiais, ProfessorService professores,
            UsuarioService usuarios) {
        this.salas = salas;
        this.materiais = materiais;
        this.professores = professores;
        this.usuarios = usuarios;
    }

    @GetMapping("/admin/salas")
    String salas(Model modelo) {
        return paginaDeSalas(modelo, Map.of());
    }

    @PostMapping("/admin/salas")
    String cadastraSala(Model modelo, RedirectAttributes aviso, @RequestParam Map<String, String> campos) {
        return grava(modelo, aviso, "/admin/salas", "Sala cadastrada.", campos, this::paginaDeSalas,
                () -> salas.cadastra(new SalaRequest(campos.get("nome"), campos.get("tipoSala"),
                        inteiro("capacidade", campos.get("capacidade")), campos.get("localizacao"),
                        marcado(campos.get("restrito")))));
    }

    @GetMapping("/admin/materiais")
    String materiais(Model modelo) {
        return paginaDeMateriais(modelo, Map.of());
    }

    @PostMapping("/admin/materiais")
    String cadastraMaterial(Model modelo, RedirectAttributes aviso, @RequestParam Map<String, String> campos) {
        return grava(modelo, aviso, "/admin/materiais", "Material cadastrado.", campos, this::paginaDeMateriais,
                () -> materiais.cadastra(new MaterialRequest(campos.get("nome"), campos.get("tipoMaterial"),
                        campos.get("patrimonio"), marcado(campos.get("restrito")))));
    }

    @GetMapping("/admin/professores")
    String professores(Model modelo) {
        return paginaDeProfessores(modelo, Map.of());
    }

    @PostMapping("/admin/professores")
    String cadastraProfessor(Model modelo, RedirectAttributes aviso, @RequestParam Map<String, String> campos) {
        return grava(modelo, aviso, "/admin/professores", "Professor cadastrado.", campos, this::paginaDeProfessores,
                () -> professores.cadastra(new ProfessorRequest(campos.get("nome"), marcado(campos.get("restrito")),
                        competencias(campos.get("competencias")))));
    }

    @GetMapping("/admin/usuarios")
    String usuarios(Model modelo) {
        return paginaDeUsuarios(modelo, Map.of());
    }

    @PostMapping("/admin/usuarios")
    String cadastraUsuario(Model modelo, RedirectAttributes aviso, @RequestParam Map<String, String> campos) {
        return grava(modelo, aviso, "/admin/usuarios", "Usuário cadastrado.", campos, this::paginaDeUsuarios,
                () -> usuarios.cria(new UsuarioRequest(campos.get("nome"), campos.get("email"), campos.get("senha"),
                        perfil(campos.get("perfil")))));
    }

    @PostMapping("/admin/usuarios/{id:\\d+}/ativo")
    String defineAtivo(Model modelo, RedirectAttributes aviso, @PathVariable long id,
            @RequestParam(required = false) String ativo) {
        return grava(modelo, aviso, "/admin/usuarios", "Situação do usuário atualizada.", Map.of(),
                this::paginaDeUsuarios, () -> usuarios.defineAtivo(id, new AtivoRequest(logico("ativo", ativo))));
    }

    @PostMapping("/admin/usuarios/{id:\\d+}/perfil")
    String trocaPerfil(Model modelo, RedirectAttributes aviso, @PathVariable long id,
            @RequestParam(required = false) String perfil) {
        return grava(modelo, aviso, "/admin/usuarios", "Perfil do usuário atualizado.", Map.of(),
                this::paginaDeUsuarios, () -> usuarios.trocaPerfil(id, new PerfilRequest(perfil(perfil))));
    }

    /** Executa o cadastro: sucesso volta para a lista; erro de regra ou de validação refaz a página com a mensagem. */
    private String grava(Model modelo, RedirectAttributes aviso, String lista, String sucesso,
            Map<String, String> campos, Pagina pagina, Supplier<?> acao) {
        try {
            acao.get();
            aviso.addFlashAttribute("sucesso", sucesso);
            return "redirect:" + lista;
        } catch (RuntimeException erro) {
            if (!Mensagens.mostravel(erro)) {
                throw erro;
            }
            modelo.addAttribute("erro", Mensagens.de(erro));
            return pagina.monta(modelo, campos);
        }
    }

    @FunctionalInterface
    private interface Pagina {
        String monta(Model modelo, Map<String, String> campos);
    }

    private String paginaDeSalas(Model modelo, Map<String, String> campos) {
        modelo.addAttribute("itens", salas.lista());
        modelo.addAttribute("form", campos);
        return "salas";
    }

    private String paginaDeMateriais(Model modelo, Map<String, String> campos) {
        modelo.addAttribute("itens", materiais.lista());
        modelo.addAttribute("form", campos);
        return "materiais";
    }

    private String paginaDeProfessores(Model modelo, Map<String, String> campos) {
        modelo.addAttribute("itens", professores.lista());
        modelo.addAttribute("form", campos);
        return "professores";
    }

    private String paginaDeUsuarios(Model modelo, Map<String, String> campos) {
        modelo.addAttribute("itens", usuarios.lista());
        modelo.addAttribute("perfis", Perfil.values());
        modelo.addAttribute("form", campos);
        return "usuarios";
    }

    private static Boolean marcado(String valor) {
        return valor == null ? null : Boolean.valueOf("true".equalsIgnoreCase(valor) || "on".equalsIgnoreCase(valor));
    }

    private static Boolean logico(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (!valor.equalsIgnoreCase("true") && !valor.equalsIgnoreCase("false")) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", campo, "motivo", "valor inválido")));
        }
        return Boolean.valueOf(valor);
    }

    private static Integer inteiro(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException erro) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", campo, "motivo", "deve ser um número inteiro")));
        }
    }

    private static Perfil perfil(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Perfil.valueOf(valor.trim());
        } catch (IllegalArgumentException erro) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "perfil", "motivo", "valor inválido")));
        }
    }

    private static List<String> competencias(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(",")).map(String::trim).toList();
    }
}
