package br.senac.recursos.seguranca;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService autenticacao;
    private final TokenService tokens;

    public AuthController(AutenticacaoService autenticacao, TokenService tokens) {
        this.autenticacao = autenticacao;
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest pedido) {
        Usuario usuario = autenticacao.autentica(pedido.email(), pedido.senha());
        TokenService.TokenEmitido token = tokens.emite(usuario);
        return new LoginResponse(token.valor(), "Bearer", token.expiraEm());
    }
}
