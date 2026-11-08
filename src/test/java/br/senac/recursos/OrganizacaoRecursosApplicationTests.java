package br.senac.recursos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = "app.seguranca.chave-assinatura=chave-de-teste-com-pelo-menos-32-caracteres-123")
class OrganizacaoRecursosApplicationTests {

    @Test
    void contextLoads() {
    }

}
