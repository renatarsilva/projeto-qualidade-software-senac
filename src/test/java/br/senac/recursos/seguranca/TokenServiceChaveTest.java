package br.senac.recursos.seguranca;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import org.junit.jupiter.api.Test;

/**
 * A chave de assinatura do token é obrigatória e precisa ser forte (spec T-15, CA-13; caso TC-35).
 */
class TokenServiceChaveTest {

    @Test
    void tc35_chaveAusenteOuCurtaImpedeAAplicacaoDeSubirComErroClaro() {
        assertThatThrownBy(() -> new TokenService("", Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_CHAVE");

        assertThatThrownBy(() -> new TokenService("curta", Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
