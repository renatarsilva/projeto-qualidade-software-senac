package br.senac.recursos.seguranca;

import java.nio.charset.StandardCharsets;

public class PoliticaDeSenha {

    private static final int MINIMO_BYTES = 8;
    private static final int MAXIMO_BYTES = 72; // limite do BCrypt

    public boolean valida(String senha) {
        int bytes = senha.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= MINIMO_BYTES && bytes <= MAXIMO_BYTES;
    }
}