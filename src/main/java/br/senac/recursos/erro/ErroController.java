package br.senac.recursos.erro;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O ponto de chegada de /error: falha em filtro, falha na renderização de uma tela ou qualquer erro que aconteceu
 * sem um controlador escolhido. Decide API ou tela pelo caminho ORIGINAL da requisição (spec T-22, CA-14).
 */
@RestController
public class ErroController implements ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(ErroController.class);

    private final RespostaDeErro resposta;

    public ErroController(RespostaDeErro resposta) {
        this.resposta = resposta;
    }

    @RequestMapping("/error")
    public ResponseEntity<?> trata(HttpServletRequest requisicao) {
        String caminho = (String) requisicao.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object codigo = requisicao.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable falha = (Throwable) requisicao.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (falha != null) {
            LOG.error("Falha fora de um controlador em {}", caminho, falha);
        }
        return resposta.para(caminho, problemaPara(codigo, falha, caminho));
    }

    private ProblemDetail problemaPara(Object codigo, Throwable falha, String caminho) {
        int status = codigo instanceof Integer inteiro ? inteiro : HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (falha == null) {
            if (status == HttpStatus.NOT_FOUND.value()) {
                return FabricaDeProblemas.naoEncontrado(caminho);
            }
            if (status == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                return FabricaDeProblemas.metodoNaoPermitido(caminho);
            }
            if (status == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
                return FabricaDeProblemas.formatoNaoSuportado(caminho);
            }
        }
        return FabricaDeProblemas.erroInesperado(caminho);
    }
}
