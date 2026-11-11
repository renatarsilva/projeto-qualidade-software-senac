package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.IdentidadesDeTeste;
import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;

/**
 * Retirada e devolução de materiais (spec T-42; TC-01 a TC-15). Reserva padrão: terça, 10:00 às 11:00, com a
 * sala-102 e o projetor. O prazo de devolução é o término da reserva.
 */
@TesteDeReservaApi
class RetiradaDeMateriaisApiTest extends BaseDaApiDeReserva {

    private RecursoPedido projetor;

    private long reservaCom(EstadoDaReserva estado, RecursoPedido... materiais) throws Exception {
        RecursoPedido[] recursos = new RecursoPedido[materiais.length + 1];
        recursos[0] = sala;
        System.arraycopy(materiais, 0, recursos, 1, materiais.length);
        return ambiente.reservaGravada(dono, estado, terca(10, 0), terca(11, 0), AGORA, recursos);
    }

    private long responsavel() {
        return IdentidadesDeTeste.garanteUsuario(Perfil.RESPONSAVEL);
    }

    /** O token vale 30 min a partir do Clock; por isso o bearer é emitido em cada chamada, com o relógio de agora. */
    private org.springframework.test.web.servlet.ResultActions retira(Perfil perfil, long reservaId, Object materialId)
            throws Exception {
        String corpo = materialId == null ? "{}" : "{\"materialId\":" + materialId + "}";
        return ambiente.chama(HttpMethod.POST, "/api/reservas/" + reservaId + "/retiradas", bearer(perfil), corpo);
    }

    private org.springframework.test.web.servlet.ResultActions devolve(Perfil perfil, long reservaId, long retiradaId)
            throws Exception {
        return ambiente.chama(HttpMethod.POST, "/api/reservas/" + reservaId + "/retiradas/" + retiradaId + "/devolucao",
                bearer(perfil), null);
    }

    private org.springframework.test.web.servlet.ResultActions lista(Perfil perfil, long reservaId) throws Exception {
        return ambiente.chama(HttpMethod.GET, "/api/reservas/" + reservaId + "/retiradas", bearer(perfil), null);
    }

    private String bearer(Perfil perfil) {
        return perfil == null ? null : perfil == Perfil.SOLICITANTE ? ambiente.bearerDoSolicitante()
                : ambiente.bearerDe(perfil);
    }

    private long registraRetirada(long reservaId, RecursoPedido material) throws Exception {
        String corpo = retira(Perfil.RESPONSAVEL, reservaId, material.id()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private int retiradas() {
        return jdbc.queryForObject("SELECT count(*) FROM retirada", Integer.class);
    }

    private void em(Instant instante) {
        relogio.defineAgora(instante);
    }

    private void prepara() throws Exception {
        projetor = ambiente.material("Projetor", "PAT-1");
    }

    @Test
    @DisplayName("TC-01: o Responsável retira o projetor de uma reserva APROVADA: 201, autor, instante e sem devolução")
    void retiradaDeReservaAprovada() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));

        retira(Perfil.RESPONSAVEL, id, projetor.id()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservaId").value((int) id))
                .andExpect(jsonPath("$.materialId").value((int) projetor.id()))
                .andExpect(jsonPath("$.registradaPor").value((int) responsavel()))
                .andExpect(jsonPath("$.retiradaEm").value("2026-11-10T10:05:00Z"))
                .andExpect(jsonPath("$.devolvidaEm").isEmpty())
                .andExpect(jsonPath("$.atrasada").value(false));
    }

    @Test
    @DisplayName("TC-02: a reserva EM_USO também permite a retirada")
    void retiradaDeReservaEmUso() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));

        retira(Perfil.RESPONSAVEL, id, projetor.id()).andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "TC-03: reserva {0} recusa a retirada")
    @EnumSource(value = EstadoDaReserva.class,
            names = {"SOLICITADA", "CANCELADA", "REJEITADA", "CONCLUIDA", "NAO_COMPARECEU"})
    @DisplayName("TC-03: sem reserva aprovada não há retirada: 409 e nada gravado")
    void semReservaAprovada(EstadoDaReserva estado) throws Exception {
        prepara();
        long id = reservaCom(estado, projetor);
        em(terca(10, 5));

        retira(Perfil.RESPONSAVEL, id, projetor.id()).andExpect(status().isConflict());

        assertThat(retiradas()).isZero();
    }

    @Test
    @DisplayName("TC-04: material que não faz parte da reserva é recusado com 409")
    void materialForaDaReserva() throws Exception {
        prepara();
        RecursoPedido microfone = ambiente.material("Microfone", "PAT-2");
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));

        retira(Perfil.RESPONSAVEL, id, microfone.id()).andExpect(status().isConflict());

        assertThat(retiradas()).isZero();
    }

    @Test
    @DisplayName("TC-05: retirar de novo o mesmo material sem devolver é recusado com 409")
    void naoSeRetiraDuasVezes() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));
        registraRetirada(id, projetor);

        retira(Perfil.RESPONSAVEL, id, projetor.id()).andExpect(status().isConflict());

        assertThat(retiradas()).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-06: a retirada no instante do término da reserva é recusada com 409")
    void retiradaNoTermino() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(11, 0));

        retira(Perfil.RESPONSAVEL, id, projetor.id()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-07: a devolução antes do término: 200, instante do Clock e atrasada = false")
    void devolucaoNoPrazo() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));
        long retirada = registraRetirada(id, projetor);
        em(terca(10, 40));

        devolve(Perfil.RESPONSAVEL, id, retirada).andExpect(status().isOk())
                .andExpect(jsonPath("$.devolvidaEm").value("2026-11-10T10:40:00Z"))
                .andExpect(jsonPath("$.atrasada").value(false));
    }

    @Test
    @DisplayName("TC-08: a devolução 1 s depois do término é sinalizada como atrasada")
    void devolucaoAtrasada() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));
        long retirada = registraRetirada(id, projetor);
        em(terca(11, 0).plusSeconds(1));

        devolve(Perfil.RESPONSAVEL, id, retirada).andExpect(status().isOk())
                .andExpect(jsonPath("$.atrasada").value(true));
    }

    @Test
    @DisplayName("TC-09: a devolução exatamente no término não é atraso")
    void devolucaoExatamenteNoTermino() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));
        long retirada = registraRetirada(id, projetor);
        em(terca(11, 0));

        devolve(Perfil.RESPONSAVEL, id, retirada).andExpect(status().isOk())
                .andExpect(jsonPath("$.atrasada").value(false));
    }

    @Test
    @DisplayName("TC-10: a devolução vale uma vez: a segunda recebe 409 e a primeira não muda")
    void devolucaoSoUmaVez() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));
        long retirada = registraRetirada(id, projetor);
        em(terca(10, 40));
        devolve(Perfil.RESPONSAVEL, id, retirada).andExpect(status().isOk());
        em(terca(10, 50));

        devolve(Perfil.RESPONSAVEL, id, retirada).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT devolvida_em FROM retirada WHERE id = ?", java.sql.Timestamp.class,
                retirada).toInstant()).isEqualTo(terca(10, 40));
    }

    @ParameterizedTest(name = "TC-11: {0} registra retirada e devolução e recebe 403")
    @EnumSource(value = Perfil.class, names = {"SOLICITANTE", "ADMINISTRADOR"})
    @DisplayName("TC-11: só o Responsável registra: 403 para os outros perfis e 401 sem token; nada gravado")
    void soOResponsavelRegistra(Perfil perfil) throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));

        retira(perfil, id, projetor.id()).andExpect(status().isForbidden());
        devolve(perfil, id, 1).andExpect(status().isForbidden());
        retira(null, id, projetor.id()).andExpect(status().isUnauthorized());
        devolve(null, id, 1).andExpect(status().isUnauthorized());

        assertThat(retiradas()).isZero();
    }

    @ParameterizedTest(name = "TC-12: {0} lista as retiradas")
    @EnumSource(value = Perfil.class, names = {"RESPONSAVEL", "ADMINISTRADOR"})
    @DisplayName("TC-12: o Responsável e o Administrador listam as retiradas da reserva, em ordem")
    void listaDasRetiradas(Perfil perfil) throws Exception {
        prepara();
        RecursoPedido microfone = ambiente.material("Microfone", "PAT-2");
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor, microfone);
        em(terca(10, 5));
        registraRetirada(id, projetor);
        registraRetirada(id, microfone);

        lista(perfil, id).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].materialId").value((int) projetor.id()))
                .andExpect(jsonPath("$[1].materialId").value((int) microfone.id()));
    }

    @Test
    @DisplayName("TC-13: o Solicitante não lista (403); reserva, material e retirada inexistentes recebem 404")
    void consultaEInexistentes() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));

        lista(Perfil.SOLICITANTE, id).andExpect(status().isForbidden());
        lista(Perfil.RESPONSAVEL, 999999).andExpect(status().isNotFound());
        retira(Perfil.RESPONSAVEL, id, 999999).andExpect(status().isNotFound());
        devolve(Perfil.RESPONSAVEL, id, 999999).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-14: retirada aberta e a lista consultada 1 s depois do término vem com atrasada = true")
    void retiradaAbertaDepoisDoPrazo() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.EM_USO, projetor);
        em(terca(10, 5));
        registraRetirada(id, projetor);
        em(terca(11, 0).plusSeconds(1));

        lista(Perfil.RESPONSAVEL, id).andExpect(status().isOk()).andExpect(jsonPath("$[0].atrasada").value(true));
    }

    @Test
    @DisplayName("TC-15: corpo sem materialId devolve 400 com o campo materialId")
    void semMaterialId() throws Exception {
        prepara();
        long id = reservaCom(EstadoDaReserva.APROVADA, projetor);
        em(terca(10, 5));

        retira(Perfil.RESPONSAVEL, id, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("materialId"));
    }
}
