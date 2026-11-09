package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Validade e assinatura do token (spec T-15, CA-06; casos TC-11 a TC-14).
 * O Clock é fixo (ADR-0008), então os testes não dependem da hora real.
 */
class VerificadorDeTokenTest {

    private static final String CHAVE = "chave-de-teste-com-pelo-menos-32-caracteres-123";
    private static final String OUTRA_CHAVE = "outra-chave-de-teste-com-32-caracteres-ou-mais-9";
    private static final Instant EMISSAO = Instant.parse("2026-11-09T08:00:00Z");

    private Clock relogioEm(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private Usuario usuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getPerfil()).thenReturn(Perfil.SOLICITANTE);
        return usuario;
    }

    private String emiteToken(String chave) {
        return new TokenService(chave, relogioEm(EMISSAO)).emite(usuario()).valor();
    }

    private VerificadorDeToken verificadorEm(Instant instante) {
        return new VerificadorDeToken(CHAVE, relogioEm(instante));
    }

    @Test
    void tc11_tokenUsadoComVinteENoveMinutosECinquentaENoveSegundosEAceito() {
        // Arrange
        String token = emiteToken(CHAVE);
        VerificadorDeToken verificador = verificadorEm(EMISSAO.plus(Duration.ofMinutes(29).plusSeconds(59)));

        // Act
        boolean valido = verificador.valido(token);

        // Assert
        assertThat(valido).isTrue();
    }

    @Test
    void tc12_tokenUsadoComTrintaMinutosExatosEExpirado() {
        // Arrange
        String token = emiteToken(CHAVE);
        VerificadorDeToken verificador = verificadorEm(EMISSAO.plus(Duration.ofMinutes(30)));

        // Act
        boolean valido = verificador.valido(token);

        // Assert
        assertThat(valido).isFalse();
    }

    @Test
    void tc13_tokenComUmaLetraAlteradaERecusado() {
        // Arrange
        String token = emiteToken(CHAVE);
        String adulterado = alteraUltimaLetra(token);
        VerificadorDeToken verificador = verificadorEm(EMISSAO.plusSeconds(1));

        // Act
        boolean valido = verificador.valido(adulterado);

        // Assert
        assertThat(adulterado).isNotEqualTo(token);
        assertThat(valido).isFalse();
    }

    @Test
    void tc14_tokenAssinadoComOutraChaveERecusado() {
        // Arrange
        String tokenDeOutraChave = emiteToken(OUTRA_CHAVE);
        VerificadorDeToken verificador = verificadorEm(EMISSAO.plusSeconds(1));

        // Act
        boolean valido = verificador.valido(tokenDeOutraChave);

        // Assert
        assertThat(valido).isFalse();
    }

    private String alteraUltimaLetra(String token) {
        char ultima = token.charAt(token.length() - 1);
        char trocada = ultima == 'A' ? 'B' : 'A';
        return token.substring(0, token.length() - 1) + trocada;
    }
}
