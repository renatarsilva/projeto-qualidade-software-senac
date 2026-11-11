package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.AmbienteDeReserva.corpo;
import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.quarta;
import static br.senac.recursos.reserva.HorariosDeTeste.segunda;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.seguranca.Perfil;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Relatório de utilização em API caixa-preta (spec T-41; TC-01 a TC-12). A massa é pequena e semeada no banco; os
 * valores esperados foram calculados à mão e a conta está no comentário de cada teste, e não saiu da aplicação.
 * Período padrão do relatório: segunda 09/11 00:00 a segunda 16/11 00:00. A sala-102 é a do cenário base.
 */
@TesteDeReservaApi
class RelatorioDeUtilizacaoApiTest extends BaseDaApiDeReserva {

    private static final String PERIODO = "?inicio=2026-11-09T00:00:00Z&fim=2026-11-16T00:00:00Z";
    private static final Instant SEXTA = Instant.parse("2026-11-13T00:00:00Z");

    private MockHttpServletResponse relatorio(String consulta) throws Exception {
        return ambiente.chama(HttpMethod.GET, "/api/relatorios/utilizacao" + consulta, ambiente.bearerDe(Perfil.RESPONSAVEL),
                null).andReturn().getResponse();
    }

    private String relatorioOk(String consulta) throws Exception {
        MockHttpServletResponse resposta = relatorio(consulta);
        assertThat(resposta.getStatus()).as("status do relatório %s", consulta).isEqualTo(200);
        return resposta.getContentAsString();
    }

    private static double horasDo(String corpo, String lista, String chave, String nome) {
        List<Number> horas = JsonPath.read(corpo, "$." + lista + "[?(@.nome=='" + nome + "')]." + chave);
        assertThat(horas).as("%s de %s", chave, nome).hasSize(1);
        return horas.get(0).doubleValue();
    }

    private long reserva(EstadoDaReserva estado, Instant inicio, Instant fim, RecursoPedido... recursos) {
        return ambiente.reservaGravada(dono, estado, inicio, fim, AGORA, recursos);
    }

    @Test
    @DisplayName("TC-01: horas por recurso, com o oráculo calculado à mão")
    void horasPorRecurso() throws Exception {
        // Conta à mão: sala-102 = terça 10:00-11:30 (1,5 h) + quarta 14:00-16:00 (2,0 h) = 3,5 h;
        //              sala-101 = terça 09:00-10:00 (1,0 h) = 1,0 h.
        RecursoPedido sala101 = ambiente.salaCom("sala-101", 30, "Bloco B");
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 30), sala);
        reserva(EstadoDaReserva.CONCLUIDA, quarta(14, 0), quarta(16, 0), sala);
        reserva(EstadoDaReserva.APROVADA, terca(9, 0), terca(10, 0), sala101);

        String corpo = relatorioOk(PERIODO);

        assertThat(horasDo(corpo, "recursos", "horasReservadas", "sala-102")).isEqualTo(3.5);
        assertThat(horasDo(corpo, "recursos", "horasReservadas", "sala-101")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("TC-02: carga horária do professor, com o oráculo calculado à mão")
    void cargaDoProfessor() throws Exception {
        // Conta à mão: professor X = terça 10:00-11:00 (1,0 h) + terça 14:00-15:30 (1,5 h) = 2,5 h.
        RecursoPedido professor = ambiente.professorCom("renata-silva-12345", "Java");
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala, professor);
        reserva(EstadoDaReserva.APROVADA, terca(14, 0), terca(15, 30), sala, professor);

        String corpo = relatorioOk(PERIODO);

        assertThat(horasDo(corpo, "professores", "horasAlocadas", "renata-silva-12345")).isEqualTo(2.5);
    }

    @Test
    @DisplayName("TC-03: reservas canceladas, rejeitadas e não comparecidas não contam; a solicitada conta")
    void reservasQueNaoContam() throws Exception {
        // Conta à mão: só a SOLICITADA de sexta 10:00-11:00 conta = 1,0 h; as três de 2 h cada ficam de fora.
        RecursoPedido sala103 = ambiente.salaCom("sala-103", 30, "Bloco B");
        reserva(EstadoDaReserva.CANCELADA, terca(10, 0), terca(12, 0), sala103);
        reserva(EstadoDaReserva.REJEITADA, quarta(10, 0), quarta(12, 0), sala103);
        reserva(EstadoDaReserva.NAO_COMPARECEU, segunda(10, 0).plusSeconds(3 * 86400), segunda(12, 0)
                .plusSeconds(3 * 86400), sala103);
        reserva(EstadoDaReserva.SOLICITADA, SEXTA.plusSeconds(10 * 3600), SEXTA.plusSeconds(11 * 3600), sala103);

        String corpo = relatorioOk(PERIODO);

        assertThat(horasDo(corpo, "recursos", "horasReservadas", "sala-103")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("TC-04: reserva que começa antes do período conta só o trecho de dentro")
    void reservaCruzandoOInicio() throws Exception {
        // Conta à mão: domingo 08/11 23:00 a segunda 09/11 02:00; o período começa em 09/11 00:00 -> 00:00-02:00 = 2,0 h.
        reserva(EstadoDaReserva.APROVADA, Instant.parse("2026-11-08T23:00:00Z"), segunda(2, 0), sala);

        assertThat(horasDo(relatorioOk(PERIODO), "recursos", "horasReservadas", "sala-102")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("TC-05: reserva que termina depois do período conta só o trecho de dentro")
    void reservaCruzandoOFim() throws Exception {
        // Conta à mão: domingo 15/11 23:00 a segunda 16/11 01:00; o período termina em 16/11 00:00 -> 23:00-00:00 = 1,0 h.
        reserva(EstadoDaReserva.APROVADA, Instant.parse("2026-11-15T23:00:00Z"), Instant.parse("2026-11-16T01:00:00Z"),
                sala);

        assertThat(horasDo(relatorioOk(PERIODO), "recursos", "horasReservadas", "sala-102")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("TC-06: período sem reservas nem conflitos: listas vazias e zero conflitos evitados")
    void periodoVazio() throws Exception {
        String corpo = relatorioOk(PERIODO);

        assertThat((List<?>) JsonPath.read(corpo, "$.recursos")).isEmpty();
        assertThat((List<?>) JsonPath.read(corpo, "$.professores")).isEmpty();
        assertThat((Integer) JsonPath.read(corpo, "$.conflitosEvitados")).isZero();
    }

    private void tentaReservarSobreposta(Instant inicio, Instant fim) throws Exception {
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(inicio, fim, sala)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-07: 3 pedidos recusados por sobreposição contam 3 conflitos evitados")
    void conflitosEvitados() throws Exception {
        // Conta à mão: 3 recusas por sobreposição na terça 10:00-11:00 -> 3 conflitos.
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        tentaReservarSobreposta(terca(10, 30), terca(11, 30));
        tentaReservarSobreposta(terca(10, 0), terca(11, 0));
        tentaReservarSobreposta(terca(9, 30), terca(10, 30));

        assertThat((Integer) JsonPath.read(relatorioOk(PERIODO), "$.conflitosEvitados")).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-08: recusa por manutenção e por antecedência não contam; só a sobreposição conta")
    void soSobreposicaoConta() throws Exception {
        // Conta à mão: 1 sobreposição + 1 manutenção + 1 antecedência -> só 1 conta.
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        tentaReservarSobreposta(terca(10, 30), terca(11, 30));
        ambiente.manutencao(sala, terca(14, 0), terca(16, 0), "Troca do ar-condicionado")
                .andExpect(status().isCreated());
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(terca(14, 30), terca(15, 30), sala))
                .andExpect(status().isConflict());
        ambiente.cria(ambiente.bearerDoSolicitante(), corpo(segunda(8, 30), segunda(9, 30), sala))
                .andExpect(status().isConflict());

        assertThat((Integer) JsonPath.read(relatorioOk(PERIODO), "$.conflitosEvitados")).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-09: uma recusa registrada antes do início do período não conta")
    void recusaForaDoPeriodo() throws Exception {
        // Conta à mão: a recusa é de segunda 08:00; o período pedido começa às 09:00 -> 0 conflitos.
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        tentaReservarSobreposta(terca(10, 30), terca(11, 30));

        String corpo = relatorioOk("?inicio=2026-11-09T09:00:00Z&fim=2026-11-16T00:00:00Z");

        assertThat((Integer) JsonPath.read(corpo, "$.conflitosEvitados")).isZero();
    }

    @Test
    @DisplayName("TC-10: o pedido recusado por sobreposição deixa 1 registro de conflito e nenhuma reserva nova")
    void recusaDeixaRegistro() throws Exception {
        reserva(EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), sala);
        tentaReservarSobreposta(terca(10, 30), terca(11, 30));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM conflito_evitado", Integer.class)).isEqualTo(1);
        assertThat(ambiente.reservas()).as("reservas gravadas").isEqualTo(1);
    }

    @Test
    @DisplayName("TC-11: o Solicitante recebe 403, sem token 401, e o Responsável e o Administrador recebem 200")
    void acessoAoRelatorio() throws Exception {
        String caminho = "/api/relatorios/utilizacao" + PERIODO;

        assertThat(ambiente.chama(HttpMethod.GET, caminho, ambiente.bearerDoSolicitante(), null).andReturn()
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(ambiente.chama(HttpMethod.GET, caminho, null, null).andReturn().getResponse().getStatus())
                .isEqualTo(401);
        assertThat(ambiente.chama(HttpMethod.GET, caminho, ambiente.bearerDe(Perfil.RESPONSAVEL), null).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(ambiente.chama(HttpMethod.GET, caminho, ambiente.bearerDe(Perfil.ADMINISTRADOR), null).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
    }

    @ParameterizedTest(name = "TC-12: {0} devolve 400 no campo {2}")
    @CsvSource(delimiter = ';', value = {
        "sem inicio; ?fim=2026-11-16T00:00:00Z; inicio",
        "sem fim; ?inicio=2026-11-09T00:00:00Z; fim",
        "fim igual ao inicio; ?inicio=2026-11-09T00:00:00Z&fim=2026-11-09T00:00:00Z; fim",
        "data inválida; ?inicio=abc&fim=2026-11-16T00:00:00Z; inicio"})
    @DisplayName("TC-12: período inválido (ausente, igual ou data inválida) devolve 400 com o campo")
    void periodoInvalido(String descricao, String consulta, String campo) throws Exception {
        MockHttpServletResponse resposta = relatorio(consulta);

        assertThat(resposta.getStatus()).isEqualTo(400);
        List<String> campos = JsonPath.read(resposta.getContentAsString(), "$.erros[*].campo");
        assertThat(campos).contains(campo);
    }
}
