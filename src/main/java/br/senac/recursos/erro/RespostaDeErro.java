package br.senac.recursos.erro;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * O ponto único que separa a resposta da API (JSON) da resposta das telas (HTML), decidindo pelo caminho original
 * da requisição, e não pelo cabeçalho Accept (spec T-22, CA-14). Caminho que começa com /api/ é API.
 */
@Component
public class RespostaDeErro {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final SpringTemplateEngine modelos;

    public RespostaDeErro(SpringTemplateEngine modelos) {
        this.modelos = modelos;
    }

    public ResponseEntity<?> para(String caminhoOriginal, ProblemDetail problema) {
        if (ehApi(caminhoOriginal)) {
            return ResponseEntity.status(problema.getStatus())
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problema);
        }
        Context contexto = new Context(Locale.forLanguageTag("pt-BR"));
        contexto.setVariable("titulo", problema.getTitle());
        contexto.setVariable("mensagem", problema.getDetail());
        String pagina = modelos.process("erro", contexto);
        return ResponseEntity.status(problema.getStatus()).contentType(HTML_UTF8).body(pagina);
    }

    static boolean ehApi(String caminho) {
        return caminho != null && caminho.startsWith("/api/");
    }
}
