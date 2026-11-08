package br.senac.recursos.seguranca;

import java.time.Clock;

public class VerificadorDeToken {

    public VerificadorDeToken(String chave, Clock clock) {
        // neutro: ainda não guarda a chave nem o relógio
    }

    public boolean valido(String token) {
        return true; // neutro: ainda não verifica assinatura nem validade
    }
}
