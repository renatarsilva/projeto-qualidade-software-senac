package br.senac.recursos.reserva;

import static br.senac.recursos.reserva.HorariosDeTeste.AGORA;
import static br.senac.recursos.reserva.HorariosDeTeste.terca;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;

/** Pesquisa de recursos por tipo, capacidade, localização, competência e disponibilidade (spec T-43, CA-01 a CA-07; TC-01 a TC-13). */
@TesteDeReservaApi
class PesquisaDeRecursosApiTest extends BaseDaApiDeReserva {

    /** Pesquisa e devolve os nomes, na ordem da resposta. Falha por asserção se o status não for 200. */
    private List<String> pesquisa(String consulta) throws Exception {
        MockHttpServletResponse resposta = ambiente.chama(HttpMethod.GET, "/api/recursos" + consulta,
                ambiente.bearerDoSolicitante(), null).andReturn().getResponse();
        assertThat(resposta.getStatus()).as("status da pesquisa %s", consulta).isEqualTo(200);
        return JsonPath.read(resposta.getContentAsString(), "$[*].nome");
    }

    private int statusDe(String consulta) throws Exception {
        return ambiente.chama(HttpMethod.GET, "/api/recursos" + consulta, ambiente.bearerDoSolicitante(), null)
                .andReturn().getResponse().getStatus();
    }

    @Test
    @DisplayName("TC-01: sem filtro, devolve os recursos dos três tipos, ordenados por nome")
    void semFiltro() throws Exception {
        ambiente.professorCom("renata-silva-12345", "Java");
        ambiente.material("Projetor", "PAT-1");

        assertThat(pesquisa("")).containsExactly("Projetor", "renata-silva-12345", "sala-102");
    }

    @ParameterizedTest(name = "TC-02: tipo={0}")
    @CsvSource({"SALA, sala-102", "PROFESSOR, renata-silva-12345", "MATERIAL, Projetor"})
    @DisplayName("TC-02: o filtro por tipo devolve só aquele tipo")
    void filtroPorTipo(String tipo, String esperado) throws Exception {
        ambiente.professorCom("renata-silva-12345", "Java");
        ambiente.material("Projetor", "PAT-1");

        assertThat(pesquisa("?tipo=" + tipo)).containsExactly(esperado);
    }

    @Test
    @DisplayName("TC-03: capacidadeMinima=30 com salas de 29, 30 e 31 lugares devolve as de 30 e 31 (limite inclusivo)")
    void capacidadeMinima() throws Exception {
        ambiente.salaCom("sala-201", 29, "Bloco B");
        ambiente.salaCom("sala-202", 30, "Bloco B");
        ambiente.salaCom("sala-203", 31, "Bloco B");

        assertThat(pesquisa("?capacidadeMinima=30")).containsExactly("sala-202", "sala-203");
    }

    @Test
    @DisplayName("TC-04: localizacao=BLOCO devolve só a sala do Bloco B, sem diferenciar maiúsculas")
    void localizacao() throws Exception {
        ambiente.salaCom("sala-201", 30, "Bloco B");
        ambiente.salaCom("sala-202", 30, "Anexo");

        assertThat(pesquisa("?localizacao=BLOCO")).containsExactly("sala-102", "sala-201");
    }

    @Test
    @DisplayName("TC-05: capacidadeMinima sem tipo devolve só salas, mesmo com professores e materiais cadastrados")
    void capacidadeVaiSoParaSalas() throws Exception {
        ambiente.professorCom("renata-silva-12345", "Java");
        ambiente.material("Projetor", "PAT-1");

        assertThat(pesquisa("?capacidadeMinima=1")).containsExactly("sala-102");
    }

    @Test
    @DisplayName("TC-06: competencia=jav devolve só o professor de Java")
    void competencia() throws Exception {
        ambiente.professorCom("renata-silva-12345", "Java", "Banco de Dados");
        ambiente.professorCom("ana-souza-54321", "Banco de Dados");

        assertThat(pesquisa("?competencia=jav")).containsExactly("renata-silva-12345");
    }

    @Test
    @DisplayName("TC-07: tipo=SALA com competencia=java devolve lista vazia")
    void filtroDeOutroTipoDaVazio() throws Exception {
        ambiente.professorCom("renata-silva-12345", "Java");

        assertThat(pesquisa("?tipo=SALA&competencia=java")).isEmpty();
    }

    @Test
    @DisplayName("TC-08: sala reservada das 10:00 às 11:00 não aparece na disponibilidade das 10:30 às 11:30")
    void disponibilidadeSobreposta() throws Exception {
        ambiente.salaCom("sala-103", 30, "Bloco B");
        ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), AGORA, sala);

        assertThat(pesquisa("?tipo=SALA&disponivelDe=" + terca(10, 30) + "&disponivelAte=" + terca(11, 30)))
                .containsExactly("sala-103");
    }

    @ParameterizedTest(name = "TC-09: disponibilidade de {0} a {1} inclui a sala (encostada)")
    @CsvSource({"2026-11-10T11:00:00Z, 2026-11-10T12:00:00Z", "2026-11-10T09:00:00Z, 2026-11-10T10:00:00Z"})
    @DisplayName("TC-09: intervalos encostados na reserva não tiram a sala da pesquisa")
    void disponibilidadeEncostada(String de, String ate) throws Exception {
        ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), AGORA, sala);

        assertThat(pesquisa("?tipo=SALA&disponivelDe=" + de + "&disponivelAte=" + ate)).containsExactly("sala-102");
    }

    @Test
    @DisplayName("TC-10: reserva CANCELADA no horário não tira a sala da pesquisa")
    void reservaCanceladaNaoOcupa() throws Exception {
        ambiente.reservaGravada(dono, EstadoDaReserva.CANCELADA, terca(10, 0), terca(11, 0), AGORA, sala);

        assertThat(pesquisa("?tipo=SALA&disponivelDe=" + terca(10, 0) + "&disponivelAte=" + terca(11, 0)))
                .containsExactly("sala-102");
    }

    @Test
    @DisplayName("TC-11: professor e material reservados no horário também somem da pesquisa por disponibilidade")
    void professorEMaterialOcupados() throws Exception {
        RecursoPedido professor = ambiente.professorCom("renata-silva-12345", "Java");
        RecursoPedido material = ambiente.material("Projetor", "PAT-1");
        ambiente.salaCom("sala-103", 30, "Bloco B");
        ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), AGORA, sala, professor,
                material);

        assertThat(pesquisa("?disponivelDe=" + terca(10, 30) + "&disponivelAte=" + terca(11, 30)))
                .containsExactly("sala-103");
    }

    @ParameterizedTest(name = "TC-12: {0} devolve 400")
    @CsvSource(delimiter = ';', value = {
        "?tipo=XYZ",
        "?capacidadeMinima=0",
        "?capacidadeMinima=-1",
        "?capacidadeMinima=abc",
        "?disponivelDe=2026-11-10T10:00:00Z",
        "?disponivelDe=2026-11-10T11:00:00Z&disponivelAte=2026-11-10T10:00:00Z"})
    @DisplayName("TC-12: filtro inválido (tipo, capacidade, só um limite ou término antes do início) devolve 400")
    void filtroInvalido(String consulta) throws Exception {
        assertThat(statusDe(consulta)).isEqualTo(400);
    }

    @Test
    @DisplayName("TC-13: tipo, capacidade, localização e disponibilidade juntos devolvem só o que atende a todos")
    void filtrosCombinados() throws Exception {
        ambiente.salaCom("sala-201", 40, "Bloco B");
        ambiente.salaCom("sala-202", 40, "Anexo");
        RecursoPedido ocupada = ambiente.salaCom("sala-203", 40, "Bloco B");
        ambiente.reservaGravada(dono, EstadoDaReserva.APROVADA, terca(10, 0), terca(11, 0), AGORA, ocupada);

        assertThat(pesquisa("?tipo=SALA&capacidadeMinima=40&localizacao=bloco&disponivelDe=" + terca(10, 0)
                + "&disponivelAte=" + terca(11, 0))).containsExactly("sala-201");
    }
}
