package br.senac.recursos.reserva;

import java.util.function.Function;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Uma linha da matriz de autorização ({@code docs/seguranca/matriz-autorizacao.md}): a operação e o status esperado
 * de cada perfil. {@code chamada} faz a requisição válida, com dados novos, para as células que não são 401 nem 403.
 */
record Operacao(String id, HttpMethod metodo, String modelo, String corpoFicticio, int anonimo, int solicitante,
        int responsavel, int administrador, Function<CenarioDeAutorizacao, ResultActions> chamada) {

    int esperado(Quem quem) {
        return switch (quem) {
            case ANONIMO -> anonimo;
            case SOLICITANTE -> solicitante;
            case RESPONSAVEL -> responsavel;
            case ADMINISTRADOR -> administrador;
        };
    }

    /** O caminho com um id qualquer: a autorização (401 e 403) vem antes de qualquer outra verificação. */
    String caminhoFicticio() {
        return modelo.replace("{id}", "1");
    }

    @Override
    public String toString() {
        return id + " " + metodo + " " + modelo;
    }
}
