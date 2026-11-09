package br.senac.recursos.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Confere a assinatura e a validade do token. A hora vem do Clock injetado (ADR-0008).
 * O token só vale enquanto a hora atual for menor que a de expiração (P-01): aos 30 minutos exatos já venceu.
 */
public class VerificadorDeToken {

    private final NimbusJwtDecoder decodificador;
    private final Clock clock;

    public VerificadorDeToken(String chave, Clock clock) {
        SecretKey chaveSecreta = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.decodificador = NimbusJwtDecoder.withSecretKey(chaveSecreta)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // O validador padrão aceita o instante exato da expiração. Aqui a validade é conferida abaixo,
        // com a regra "hora atual menor que a expiração" e sem tolerância de relógio.
        this.decodificador.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        this.clock = clock;
    }

    public boolean valido(String token) {
        try {
            Jwt jwt = decodificador.decode(token);
            Instant expiraEm = jwt.getExpiresAt();
            return expiraEm != null && clock.instant().isBefore(expiraEm);
        } catch (JwtException erro) {
            return false; // assinatura inválida, token adulterado ou formato errado
        }
    }
}
