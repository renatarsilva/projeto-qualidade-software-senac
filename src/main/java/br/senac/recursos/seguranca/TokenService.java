package br.senac.recursos.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Emite o token assinado da API. A hora vem do Clock injetado (ADR-0008); a validade é de 30 minutos (P-01). */
@Service
public class TokenService {

    private static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final int CHAVE_MINIMA_BYTES = 32;

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(@Value("${app.seguranca.chave-assinatura}") String chave, Clock clock) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalStateException(
                    "A chave de assinatura do token não foi definida. Defina a variável de ambiente APP_JWT_CHAVE.");
        }
        if (chave.getBytes(StandardCharsets.UTF_8).length < CHAVE_MINIMA_BYTES) {
            throw new IllegalStateException(
                    "A chave de assinatura do token (APP_JWT_CHAVE) precisa ter pelo menos 32 caracteres.");
        }
        SecretKey chaveSecreta = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chaveSecreta));
        this.clock = clock;
    }

    public TokenEmitido emite(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(VALIDADE);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEm);
    }

    public record TokenEmitido(String valor, Instant expiraEm) {
    }
}
