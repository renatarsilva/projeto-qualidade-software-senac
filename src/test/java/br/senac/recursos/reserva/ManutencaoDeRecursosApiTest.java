package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;

/**
 * Manutenção bloqueia a reserva (spec T-35; TC-01 a TC-17 e TC-19). A manutenção da sala-102 é, por padrão, a de
 * terça das 10:00 às 12:00.
 */
@TesteDeReservaApi
class ManutencaoDeRecursosApiTest extends BaseDaApiDeReserva {

    private static final String SALA_EM_MANUTENCAO = "A sala está em manutenção neste horário";

    /** Registra a manutenção pela API e exige 201: se o registro falha, o teste falha por asserção. */
    private long registra(RecursoPedido recurso, Instant inicio, Instant termino) throws Exception {
        String resposta = ambiente.manutencao(recurso, inicio, termino, "Troca do ar-condicionado")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private void emManutencaoDas10As12() throws Exception {
        registra(sala, terca(10, 0), terca(12, 0));
    }

    private void recusaReserva(Instant inicio, Instant termino, RecursoPedido... recursos) throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(inicio, termino, recursos))
                .andExpect(status().isConflict());
        assertThat(ambiente.reservas()).as("reservas gravadas").isZero();
    }

    @ParameterizedTest(name = "TC-01, TC-02 e TC-19: reserva das {0} às {1} é recusada")
    @CsvSource({"2026-11-10T10:30:00Z, 2026-11-10T11:30:00Z", "2026-11-10T09:30:00Z, 2026-11-10T10:30:00Z",
        "2026-11-10T09:00:00Z, 2026-11-10T13:00:00Z"})
    @DisplayName("TC-01, TC-02 e TC-19 (RN-04-T1 e T2): reserva dentro, invadindo o início ou contendo a manutenção é recusada com 409")
    void reservaQueTocaAManutencao(Instant inicio, Instant termino) throws Exception {
        emManutencaoDas10As12();

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(inicio, termino, sala))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(SALA_EM_MANUTENCAO));
        assertThat(ambiente.reservas()).isZero();
    }

    @ParameterizedTest(name = "TC-03 e TC-04: reserva das {0} às {1} é aceita (encostada)")
    @CsvSource({"2026-11-10T09:00:00Z, 2026-11-10T10:00:00Z", "2026-11-10T12:00:00Z, 2026-11-10T13:00:00Z"})
    @DisplayName("TC-03 e TC-04 (RN-04-T3 e T4): reserva que encosta na manutenção é aceita (S-01)")
    void reservaEncostadaNaManutencao(Instant inicio, Instant termino) throws Exception {
        emManutencaoDas10As12();

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(inicio, termino, sala)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-05 (RN-04-T5): a sala-101, sem manutenção, no mesmo horário é aceita")
    void outraSalaNaoEAfetada() throws Exception {
        RecursoPedido outraSala = ambiente.sala("sala-101");
        emManutencaoDas10As12();

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), outraSala))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-06 (RN-04-T6): a sala em manutenção em outro dia é aceita")
    void outroDiaNaoEAfetado() throws Exception {
        emManutencaoDas10As12();

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(quarta(10, 30), quarta(11, 30), sala))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("TC-07 (RN-04-T7): material em manutenção é recusado com a mensagem do material")
    void materialEmManutencao() throws Exception {
        RecursoPedido projetor = ambiente.material("Projetor", "PAT-1");
        registra(projetor, terca(10, 0), terca(12, 0));

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), projetor))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O material está em manutenção neste horário"));
    }

    @Test
    @DisplayName("TC-08: professor em manutenção é recusado com a mensagem do professor")
    void professorEmManutencao() throws Exception {
        RecursoPedido professor = ambiente.professorCom("renata-silva-12345", "Java");
        registra(professor, terca(10, 0), terca(12, 0));

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), professor))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("O professor está em manutenção neste horário"));
    }

    @Test
    @DisplayName("TC-09 (RN-04-T8): sala livre e projetor em manutenção: recusado por inteiro e a sala não fica gravada")
    void tudoOuNadaComManutencao() throws Exception {
        RecursoPedido projetor = ambiente.material("Projetor", "PAT-1");
        registra(projetor, terca(10, 0), terca(12, 0));

        recusaReserva(terca(10, 30), terca(11, 30), sala, projetor);
        assertThat(ambiente.vinculos()).as("vínculos gravados").isZero();
    }

    @Test
    @DisplayName("TC-10: o dono altera a reserva para o horário em manutenção: 409 e ela mantém o horário original")
    void alteracaoParaHorarioEmManutencao() throws Exception {
        long id = ambiente.reservaGravada(dono, EstadoDaReserva.SOLICITADA, terca(14, 0), terca(15, 0), AGORA, sala);
        emManutencaoDas10As12();

        ambiente.altera(ambiente.bearerDoSolicitante(), id, corpo(terca(10, 30), terca(11, 30), sala))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(SALA_EM_MANUTENCAO));
        assertThat(ambiente.inicioNoBanco(id)).isEqualTo(terca(14, 0));
    }

    @Test
    @DisplayName("TC-11 (RN-04-T9): criar a manutenção não cancela a reserva existente, e ela aparece em reservasAfetadas")
    void reservaExistenteContinuaEFicaListada() throws Exception {
        long id = ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 30), terca(11, 30), AGORA, sala);

        ambiente.manutencao(sala, terca(10, 0), terca(12, 0), "Troca do ar-condicionado")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservasAfetadas[0]").value((int) id));
        assertThat(ambiente.estadoNoBanco(id)).isEqualTo("APROVADA");
    }

    @Test
    @DisplayName("TC-12 (RN-04-T11): encerrar a manutenção libera o recurso para novas reservas")
    void encerrarLiberaORecurso() throws Exception {
        long manutencao = registra(sala, terca(10, 0), terca(12, 0));

        ambiente.chama(HttpMethod.DELETE, "/api/recursos/" + sala.id() + "/manutencoes/" + manutencao,
                ambiente.bearerDe(Perfil.ADMINISTRADOR), null).andExpect(status().isNoContent());

        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(10, 30), terca(11, 30), sala))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "TC-13: {0} devolve 400 no campo {3}")
    @CsvSource(delimiter = ';', value = {
        "término igual ao início; 2026-11-10T10:00:00Z; 2026-11-10T10:00:00Z; termino; Motivo",
        "término antes do início; 2026-11-10T11:00:00Z; 2026-11-10T10:00:00Z; termino; Motivo",
        "término que já passou; 2026-11-09T06:00:00Z; 2026-11-09T07:00:00Z; termino; Motivo",
        "motivo ausente; 2026-11-10T10:00:00Z; 2026-11-10T12:00:00Z; motivo; "})
    @DisplayName("TC-13: período inválido (término, passado ou motivo ausente) devolve 400 com o campo")
    void periodoInvalido(String descricao, Instant inicio, Instant termino, String campo, String motivo)
            throws Exception {
        ambiente.manutencao(sala, inicio, termino, motivo == null ? "" : motivo)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value(campo));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM periodo_manutencao", Integer.class)).isZero();
    }

    @Test
    @DisplayName("TC-13: motivo com 101 caracteres devolve 400")
    void motivoGrandeDemais() throws Exception {
        ambiente.manutencao(sala, terca(10, 0), terca(12, 0), "x".repeat(101))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros[0].campo").value("motivo"));
    }

    @Test
    @DisplayName("TC-14: manutenção que cruza outra do mesmo recurso devolve 409 e nada é gravado")
    void manutencoesNaoSeCruzam() throws Exception {
        emManutencaoDas10As12();

        ambiente.manutencao(sala, terca(11, 0), terca(13, 0), "Outra").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM periodo_manutencao", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest(name = "TC-15: {0} registra manutenção e recebe 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "RESPONSAVEL"})
    @DisplayName("TC-15: só o Administrador registra, lista e encerra: 403 para os outros perfis e 401 sem token")
    void soOAdministradorMexeNaManutencao(Perfil perfil) throws Exception {
        long manutencao = registra(sala, terca(10, 0), terca(12, 0));
        String bearer = perfil == Perfil.SOLICITANTE ? ambiente.bearerDoSolicitante() : ambiente.bearerDe(perfil);
        String base = "/api/recursos/" + sala.id() + "/manutencoes";

        ambiente.chama(HttpMethod.POST, base, bearer, "{}").andExpect(status().isForbidden());
        ambiente.chama(HttpMethod.GET, base, bearer, null).andExpect(status().isForbidden());
        ambiente.chama(HttpMethod.DELETE, base + "/" + manutencao, bearer, null).andExpect(status().isForbidden());
        ambiente.chama(HttpMethod.GET, base, null, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-16: manutenção de um recurso que não existe devolve 404")
    void recursoInexistente() throws Exception {
        ambiente.manutencao(new RecursoPedido(TipoDeRecurso.SALA, 999999), terca(10, 0), terca(12, 0), "Motivo")
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-17: o recurso em manutenção não aparece na pesquisa de disponibilidade do intervalo")
    void pesquisaExcluiORecursoEmManutencao() throws Exception {
        ambiente.salaCom("sala-103", 30, "Bloco B");
        emManutencaoDas10As12();

        String resposta = ambiente.chama(HttpMethod.GET, "/api/recursos?tipo=SALA&disponivelDe=" + terca(10, 30)
                + "&disponivelAte=" + terca(11, 30), ambiente.bearerDoSolicitante(), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> nomes = JsonPath.read(resposta, "$[*].nome");
        assertThat(nomes).containsExactly("sala-103");
    }
}
