package br.senac.recursos.seguranca;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gestão de usuários: só o Administrador (regra de acesso em UsuarioAcessoConfiguracao). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cria(@RequestBody UsuarioRequest pedido) {
        Usuario usuario = servico.cria(pedido);
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.getId())).body(UsuarioResponse.de(usuario));
    }

    @GetMapping
    public List<UsuarioResponse> lista() {
        return servico.lista().stream().map(UsuarioResponse::de).toList();
    }

    @PatchMapping("/{id}/perfil")
    public UsuarioResponse trocaPerfil(@PathVariable long id, @RequestBody PerfilRequest pedido) {
        return UsuarioResponse.de(servico.trocaPerfil(id, pedido));
    }

    @PatchMapping("/{id}/ativo")
    public UsuarioResponse defineAtivo(@PathVariable long id, @RequestBody AtivoRequest pedido) {
        return UsuarioResponse.de(servico.defineAtivo(id, pedido));
    }
}
