package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.List;

/**
 * A matriz de autorização como dados (spec T-49, grill 1): é o espelho de {@code docs/seguranca/matriz-autorizacao.md},
 * uma linha por operação. O teste de completude confere esta lista contra as rotas mapeadas na aplicação.
 */
final class MatrizDeAutorizacao {

    private static final String SALA_VALIDA = "{\"nome\":\"%s\",\"tipoSala\":\"Laboratório\",\"capacidade\":40,"
            + "\"localizacao\":\"Bloco B\",\"restrito\":false}";
    private static final String PROFESSOR_VALIDO = "{\"nome\":\"%s\",\"restrito\":false,\"competencias\":[\"Java\"]}";
    private static final String MATERIAL_VALIDO = "{\"nome\":\"%s\",\"tipoMaterial\":\"Projetor\","
            + "\"patrimonio\":\"%s\",\"restrito\":false}";

    private static final String MANUTENCAO_VALIDA = "{\"inicio\":\"2026-11-10T10:00:00Z\","
            + "\"termino\":\"2026-11-10T12:00:00Z\",\"motivo\":\"Troca do ar-condicionado\"}";

    /** As rotas que não são da matriz de negócio e não contam na completude. */
    static final String LOGIN = "/api/auth/login";

    static final List<Operacao> OPERACOES = List.of(
            // Usuários: só o Administrador
            new Operacao("U-01", POST, "/api/usuarios", "{}", 401, 403, 403, 201, c -> chama(c, POST, "/api/usuarios",
                    "{\"nome\":\"Novo\",\"email\":\"" + c.emailNovo() + "\",\"senha\":\"senha-segura-123\","
                            + "\"perfil\":\"SOLICITANTE\"}")),
            new Operacao("U-02", GET, "/api/usuarios", null, 401, 403, 403, 200, c -> chama(c, GET, "/api/usuarios", null)),
            new Operacao("U-03", PATCH, "/api/usuarios/{id}/perfil", "{\"perfil\":\"SOLICITANTE\"}", 401, 403, 403, 200,
                    c -> chama(c, PATCH, "/api/usuarios/" + c.usuarioAlvo() + "/perfil", "{\"perfil\":\"SOLICITANTE\"}")),
            new Operacao("U-04", PATCH, "/api/usuarios/{id}/ativo", "{\"ativo\":true}", 401, 403, 403, 200,
                    c -> chama(c, PATCH, "/api/usuarios/" + c.usuarioAlvo() + "/ativo", "{\"ativo\":true}")),
            // Salas
            new Operacao("S-01", GET, "/api/salas", null, 401, 200, 200, 200, c -> chama(c, GET, "/api/salas", null)),
            new Operacao("S-02", GET, "/api/salas/{id}", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/salas/" + c.sala.id(), null)),
            new Operacao("S-03", POST, "/api/salas", "{}", 401, 403, 403, 201,
                    c -> chama(c, POST, "/api/salas", String.format(SALA_VALIDA, "sala-150"))),
            new Operacao("S-04", PUT, "/api/salas/{id}", "{}", 401, 403, 403, 200,
                    c -> chama(c, PUT, "/api/salas/" + c.sala.id(), String.format(SALA_VALIDA, "sala-102"))),
            // Professores
            new Operacao("P-01", GET, "/api/professores", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/professores", null)),
            new Operacao("P-02", GET, "/api/professores/{id}", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/professores/" + professor(c), null)),
            new Operacao("P-03", POST, "/api/professores", "{}", 401, 403, 403, 201,
                    c -> chama(c, POST, "/api/professores", String.format(PROFESSOR_VALIDO, "ana-souza-54321"))),
            new Operacao("P-04", PUT, "/api/professores/{id}", "{}", 401, 403, 403, 200,
                    c -> chama(c, PUT, "/api/professores/" + professor(c),
                            String.format(PROFESSOR_VALIDO, "renata-silva-12345"))),
            new Operacao("P-05", DELETE, "/api/professores/{id}", null, 401, 403, 403, 204,
                    c -> chama(c, DELETE, "/api/professores/" + professor(c), null)),
            // Materiais
            new Operacao("M-01", GET, "/api/materiais", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/materiais", null)),
            new Operacao("M-02", GET, "/api/materiais/{id}", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/materiais/" + material(c), null)),
            new Operacao("M-03", POST, "/api/materiais", "{}", 401, 403, 403, 201,
                    c -> chama(c, POST, "/api/materiais", String.format(MATERIAL_VALIDO, "Microfone", "PAT-9"))),
            new Operacao("M-04", PUT, "/api/materiais/{id}", "{}", 401, 403, 403, 200,
                    c -> chama(c, PUT, "/api/materiais/" + material(c), String.format(MATERIAL_VALIDO, "Projetor", "PAT-1"))),
            new Operacao("M-05", DELETE, "/api/materiais/{id}", null, 401, 403, 403, 204,
                    c -> chama(c, DELETE, "/api/materiais/" + material(c), null)),
            // Pesquisa de recursos (T-43)
            new Operacao("G-01", GET, "/api/recursos", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/recursos", null)),
            // Reservas
            new Operacao("R-01", POST, "/api/reservas", "{}", 401, 201, 403, 403,
                    c -> chama(c, POST, "/api/reservas", corpo(terca(10, 0), terca(11, 0), c.sala))),
            new Operacao("R-02", PUT, "/api/reservas/{id}", "{}", 401, 200, 403, 403,
                    c -> chama(c, PUT, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.SOLICITADA),
                            corpo(quarta(10, 0), quarta(11, 0), c.sala))),
            new Operacao("R-03", POST, "/api/reservas/{id}/cancelamento", null, 401, 200, 403, 403,
                    c -> chama(c, POST, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.APROVADA) + "/cancelamento",
                            null)),
            new Operacao("R-04", POST, "/api/reservas/{id}/checkin", null, 401, 200, 403, 403, c -> {
                long id = c.reservaDoDono(EstadoDaReserva.APROVADA);
                c.relogio.defineAgora(terca(10, 30));
                // O token vale 30 min a partir do Clock: com o relógio avançado, emite-se um novo.
                return chama(c, POST, "/api/reservas/" + id + "/checkin", null, c.bearerRenovado());
            }),
            new Operacao("R-05", PATCH, "/api/reservas/{id}/estado", "{\"estado\":\"APROVADA\"}", 401, 403, 200, 403,
                    c -> chama(c, PATCH, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.SOLICITADA) + "/estado",
                            "{\"estado\":\"APROVADA\"}")),
            new Operacao("R-06", PATCH, "/api/reservas/{id}/estado", "{\"estado\":\"EM_USO\"}", 401, 403, 403, 403,
                    null),
            new Operacao("R-07", DELETE, "/api/reservas/{id}", null, 401, 409, 403, 403,
                    c -> chama(c, DELETE, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.SOLICITADA), null)),
            // Manutenção (T-35): só o Administrador
            new Operacao("MT-01", POST, "/api/recursos/{recursoId}/manutencoes", "{}", 401, 403, 403, 201,
                    c -> chama(c, POST, "/api/recursos/" + c.sala.id() + "/manutencoes", MANUTENCAO_VALIDA)),
            new Operacao("MT-02", GET, "/api/recursos/{recursoId}/manutencoes", null, 401, 403, 403, 200,
                    c -> chama(c, GET, "/api/recursos/" + c.sala.id() + "/manutencoes", null)),
            new Operacao("MT-03", DELETE, "/api/recursos/{recursoId}/manutencoes/{manutencaoId}", null, 401, 403, 403,
                    204, c -> {
                        try {
                            String resposta = chama(c, POST, "/api/recursos/" + c.sala.id() + "/manutencoes",
                                    MANUTENCAO_VALIDA).andReturn().getResponse().getContentAsString();
                            long id = ((Number) com.jayway.jsonpath.JsonPath.read(resposta, "$.id")).longValue();
                            return chama(c, DELETE, "/api/recursos/" + c.sala.id() + "/manutencoes/" + id, null);
                        } catch (Exception erro) {
                            throw new AssertionError("Não foi possível preparar a manutenção: " + erro, erro);
                        }
                    }),
            // Retirada e devolução de materiais (T-42): só o Responsável registra
            new Operacao("RT-01", POST, "/api/reservas/{reservaId}/retiradas", "{}", 401, 403, 201, 403, c -> {
                RecursoPedido material = materialDaRetirada(c);
                long id = reservaComMaterial(c, material);
                return chama(c, POST, "/api/reservas/" + id + "/retiradas", "{\"materialId\":" + material.id() + "}");
            }),
            new Operacao("RT-02", POST, "/api/reservas/{reservaId}/retiradas/{retiradaId}/devolucao", null, 401, 403,
                    200, 403, c -> {
                        try {
                            RecursoPedido material = materialDaRetirada(c);
                            long id = reservaComMaterial(c, material);
                            String resposta = chama(c, POST, "/api/reservas/" + id + "/retiradas",
                                    "{\"materialId\":" + material.id() + "}").andReturn().getResponse()
                                    .getContentAsString();
                            long retirada = ((Number) com.jayway.jsonpath.JsonPath.read(resposta, "$.id")).longValue();
                            return chama(c, POST, "/api/reservas/" + id + "/retiradas/" + retirada + "/devolucao", null);
                        } catch (Exception erro) {
                            throw new AssertionError("Não foi possível preparar a retirada: " + erro, erro);
                        }
                    }),
            new Operacao("RT-03", GET, "/api/reservas/{reservaId}/retiradas", null, 401, 403, 200, 200,
                    c -> chama(c, GET, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.APROVADA) + "/retiradas",
                            null)),
            // Auditoria (T-40): só leitura, só Responsável e Administrador
            new Operacao("R-10", GET, "/api/reservas/{id}/auditoria", null, 401, 403, 200, 200,
                    c -> chama(c, GET, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.SOLICITADA) + "/auditoria",
                            null)),
            // Consulta de reservas (T-43): o Solicitante vê só as próprias
            new Operacao("R-08", GET, "/api/reservas", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/reservas", null)),
            new Operacao("R-09", GET, "/api/reservas/{id}", null, 401, 200, 200, 200,
                    c -> chama(c, GET, "/api/reservas/" + c.reservaDoDono(EstadoDaReserva.SOLICITADA), null)));

    private MatrizDeAutorizacao() {
    }

    private static org.springframework.test.web.servlet.ResultActions chama(CenarioDeAutorizacao c,
            org.springframework.http.HttpMethod metodo, String caminho, String json) {
        return chama(c, metodo, caminho, json, c.bearer);
    }

    private static org.springframework.test.web.servlet.ResultActions chama(CenarioDeAutorizacao c,
            org.springframework.http.HttpMethod metodo, String caminho, String json, String bearer) {
        try {
            return c.ambiente.chama(metodo, caminho, bearer, json);
        } catch (Exception erro) {
            throw new AssertionError("A chamada " + metodo + " " + caminho + " falhou: " + erro, erro);
        }
    }

    /** Reserva APROVADA do dono, terça das 10:00 às 11:00, com a sala e o material. */
    private static long reservaComMaterial(CenarioDeAutorizacao c, RecursoPedido material) {
        return c.ambiente.reservaGravada(c.ambiente.solicitante(), EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0),
                HorariosDeTeste.AGORA, c.sala, material);
    }

    private static RecursoPedido materialDaRetirada(CenarioDeAutorizacao c) {
        try {
            return c.ambiente.material("Projetor", "PAT-1");
        } catch (Exception erro) {
            throw new AssertionError("Não foi possível criar o material do cenário: " + erro, erro);
        }
    }

    private static long professor(CenarioDeAutorizacao c) {
        try {
            return c.ambiente.professor("renata-silva-12345").id();
        } catch (Exception erro) {
            throw new AssertionError("Não foi possível criar o professor do cenário: " + erro, erro);
        }
    }

    private static long material(CenarioDeAutorizacao c) {
        try {
            return c.ambiente.material("Projetor", "PAT-1").id();
        } catch (Exception erro) {
            throw new AssertionError("Não foi possível criar o material do cenário: " + erro, erro);
        }
    }
}
