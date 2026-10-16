package br.senac.recursos;

import org.springframework.boot.SpringApplication;

/**
 * Roda a aplicação localmente usando o PostgreSQL do Testcontainers, sem precisar do compose.yaml.
 * No IntelliJ: botão "Run" ao lado do main.
 */
public class TestOrganizacaoRecursosApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrganizacaoRecursosApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
