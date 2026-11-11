package br.senac.recursos.documentacao;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

/**
 * Documentação da API (RF-14, T-51). O resumo de cada endpoint vem de {@link ResumosDaApi}; os perfis permitidos vêm
 * das mesmas {@link RegraDeAcesso} que a segurança usa, para a documentação não divergir da autorização.
 */
@Configuration
public class OpenApiConfiguracao {

    private static final String LOGIN = "/api/auth/login";

    @Bean
    OpenAPI organizacaoDeRecursos() {
        return new OpenAPI()
                .info(new Info().title("Organização de Recursos").version("1.0.0")
                        .description("Reserva de salas, professores e materiais sem conflito de horário. "
                                + "Entre por POST /api/auth/login e envie o token no cabeçalho Authorization: Bearer. "
                                + "Os erros seguem o formato ProblemDetail (RFC 9457)."))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    /** Resumo, perfis permitidos, respostas de erro e exigência do token em cada operação. */
    @Bean
    OpenApiCustomizer descricaoDosEndpoints(ObjectProvider<RegraDeAcesso> regras) {
        List<RegraDeAcesso> declaradas = regras.orderedStream().toList();
        AntPathMatcher casador = new AntPathMatcher();
        return openApi -> openApi.getPaths().forEach((caminho, item) ->
                item.readOperationsMap().forEach((metodo, operacao) -> {
                    String chave = metodo.name() + " " + caminho;
                    operacao.setSummary(ResumosDaApi.RESUMOS.getOrDefault(chave, operacao.getSummary()));
                    boolean publico = LOGIN.equals(caminho);
                    String perfis = publico ? "Público (sem token)"
                            : perfisDe(declaradas, casador, metodo, caminho);
                    operacao.setDescription("Perfis permitidos: " + perfis + ".");
                    if (!publico) {
                        operacao.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                        resposta(operacao, "401", "Sem autenticação ou token inválido");
                        resposta(operacao, "403", "Perfil sem permissão para esta operação");
                    }
                    if (metodo != PathItem.HttpMethod.GET && metodo != PathItem.HttpMethod.DELETE) {
                        resposta(operacao, "400", "Entrada inválida: o corpo traz o campo e o motivo");
                    }
                    if (caminho.contains("{")) {
                        resposta(operacao, "404", "Recurso não encontrado");
                    }
                    if (metodo != PathItem.HttpMethod.GET) {
                        resposta(operacao, "409", "Conflito com uma regra de negócio");
                    }
                }));
    }

    private static void resposta(Operation operacao, String codigo, String descricao) {
        if (operacao.getResponses().get(codigo) == null) {
            operacao.getResponses().addApiResponse(codigo, new ApiResponse().description(descricao));
        }
    }

    private static String perfisDe(List<RegraDeAcesso> regras, AntPathMatcher casador, PathItem.HttpMethod metodo,
            String caminho) {
        // A segurança usa a primeira regra que casa o método e o caminho; a documentação faz a mesma conta.
        String exemplo = caminho.replaceAll("\\{[^}]+\\}", "1");
        for (RegraDeAcesso regra : regras) {
            boolean metodoCasa = regra.metodo() == null || regra.metodo().equals(HttpMethod.valueOf(metodo.name()));
            if (metodoCasa && casador.match(regra.padrao(), exemplo)) {
                return regra.perfis().isEmpty() ? "qualquer usuário autenticado"
                        : regra.perfis().stream().map(Perfil::name).sorted().collect(Collectors.joining(", "));
            }
        }
        return "ninguém (negado por padrão)";
    }
}
