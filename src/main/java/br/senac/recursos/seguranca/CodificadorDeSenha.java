package br.senac.recursos.seguranca;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CodificadorDeSenha {

    // O BCrypt sorteia um sal novo a cada chamada: a mesma senha gera hashes diferentes.
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String codifica(String senha) {
        return encoder.encode(senha);
    }
}
