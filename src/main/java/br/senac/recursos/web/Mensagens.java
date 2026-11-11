package br.senac.recursos.web;

import br.senac.recursos.erro.ConflitoDeNegocioException;
import br.senac.recursos.erro.EntradaInvalidaException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduz o erro de regra ou de validação numa frase para a pessoa (RF-13). A mensagem vem do serviço ou de uma
 * tradução fixa: nunca de uma exceção técnica, então não vaza stack trace, SQL nem nome de classe (spec T-22).
 */
final class Mensagens {

    private static final Map<String, String> ROTULOS = Map.ofEntries(
            Map.entry("inicio", "Início"), Map.entry("termino", "Término"), Map.entry("fim", "Fim"),
            Map.entry("recursos", "Recursos"), Map.entry("nome", "Nome"), Map.entry("tipoSala", "Tipo da sala"),
            Map.entry("capacidade", "Capacidade"), Map.entry("localizacao", "Localização"),
            Map.entry("tipoMaterial", "Tipo do material"), Map.entry("patrimonio", "Patrimônio"),
            Map.entry("email", "E-mail"), Map.entry("senha", "Senha"), Map.entry("perfil", "Perfil"),
            Map.entry("motivo", "Motivo"), Map.entry("competencias", "Competências"),
            Map.entry("materialId", "Material"), Map.entry("restrito", "Restrito"), Map.entry("ativo", "Situação"),
            Map.entry("estado", "Estado"), Map.entry("tipo", "Tipo"), Map.entry("capacidadeMinima", "Capacidade mínima"),
            Map.entry("disponivelDe", "Disponível de"), Map.entry("disponivelAte", "Disponível até"));

    private Mensagens() {
    }

    /** Só traduz os erros que a tela sabe mostrar; qualquer outro segue para a página de erro amigável. */
    static boolean mostravel(RuntimeException erro) {
        return erro instanceof EntradaInvalidaException || erro instanceof ConflitoDeNegocioException;
    }

    static String de(RuntimeException erro) {
        if (erro instanceof EntradaInvalidaException invalida) {
            List<Map<String, String>> campos = invalida.getErros();
            return "Corrija os dados: " + campos.stream()
                    .map(c -> ROTULOS.getOrDefault(c.get("campo"), c.get("campo")) + " (" + c.get("motivo") + ")")
                    .collect(Collectors.joining("; ")) + ".";
        }
        return erro.getMessage();
    }
}
