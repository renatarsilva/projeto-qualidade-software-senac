package br.senac.recursos.seguranca;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    // Tratamento local do login. O formato único de erro de toda a API é da T-22.
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException erro) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, erro.getMessage());
        problema.setTitle("Autenticação necessária");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail entradaInvalida(MethodArgumentNotValidException erro) {
        List<Map<String, String>> erros = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> Map.of("campo", campo.getField(), "motivo", String.valueOf(campo.getDefaultMessage())))
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);
        return problema;
    }
}
