package br.senac.recursos.erro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas que só existem nos testes (src/test) e lançam cada situação de erro (spec T-22, seção 6).
 * Ficam em /api/testes-de-erro, que não colide com nenhuma rota real.
 */
@RestController
@RequestMapping("/api/testes-de-erro")
class RotasDeErroDeTesteController {

    record CadastroDeTeste(@NotNull(message = "deve ser informado") @Min(value = 1, message = "deve ser maior que zero")
            Integer capacidade) {
    }

    enum Estado { SOLICITADA, APROVADA }

    record EstadoDeTeste(Estado estado) {
    }

    record PeriodoDeTeste(Instant inicio, Instant termino) {
    }

    record ValorUnico(String valor, boolean verificar) {
    }

    private final JdbcTemplate jdbc;

    RotasDeErroDeTesteController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/cadastros")
    Map<String, Integer> cadastra(@Valid @RequestBody CadastroDeTeste pedido) {
        return Map.of("capacidade", pedido.capacidade());
    }

    @GetMapping("/nao-encontrado")
    String naoEncontrado() {
        throw new RecursoNaoEncontradoException();
    }

    @GetMapping("/conflito")
    String conflitoPorGet() {
        throw new ConflitoDeNegocioException("Já existe um registro com esse valor");
    }

    @PostMapping("/conflito")
    String conflitoPorPost() {
        throw new ConflitoDeNegocioException("Já existe um registro com esse valor");
    }

    @GetMapping("/falha-inesperada")
    String falhaInesperada() {
        throw new RuntimeException("falha ao executar SELECT * FROM salas em br.senac.recursos.sala.SalaService");
    }

    @GetMapping("/falha-de-dados")
    String falhaDeDados() {
        throw dados("valor longo demais: INSERT INTO sala (nome) VALUES (...) violates varchar(100)", "22001");
    }

    @GetMapping("/simula-unique")
    String simulaUnique() {
        throw dados("duplicate key value violates unique constraint \"recurso_tipo_nome_unico\"", "23505");
    }

    @GetMapping("/simula-exclude")
    String simulaExclude() {
        throw dados("conflicting key value violates exclusion constraint \"reserva_sem_sobreposicao\"", "23P01");
    }

    @GetMapping("/simula-fk")
    String simulaFk() {
        throw dados("update or delete on table \"recurso\" violates foreign key constraint", "23503");
    }

    @GetMapping("/numero/{id}")
    Map<String, Long> numero(@PathVariable long id) {
        return Map.of("id", id);
    }

    @PostMapping("/estados")
    String estado(@RequestBody EstadoDeTeste pedido) {
        return String.valueOf(pedido.estado());
    }

    @PostMapping("/periodos")
    String periodo(@RequestBody PeriodoDeTeste pedido) {
        if (!pedido.termino().isAfter(pedido.inicio())) {
            throw new EntradaInvalidaException(List.of(Map.of("campo", "termino", "motivo",
                    "deve ser posterior ao início")));
        }
        return "ok";
    }

    @GetMapping("/acesso-negado")
    String acessoNegado() {
        throw new AcessoNegadoException();
    }

    /** O serviço decide que quem pede não é o dono antes de olhar o corpo (a ordem dos erros, CA-15). */
    @PostMapping("/dono")
    String dono(@RequestBody CadastroDeTeste pedido) {
        throw new AcessoNegadoException();
    }

    /** O corpo é validado antes do método rodar; o conflito só apareceria depois (CA-15). */
    @PostMapping("/conflito-e-corpo-invalido")
    String conflitoECorpoInvalido(@Valid @RequestBody CadastroDeTeste pedido) {
        throw new ConflitoDeNegocioException("Já existe um registro com esse valor");
    }

    @PostMapping(value = "/so-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    String soJson(@RequestBody CadastroDeTeste pedido) {
        return "ok";
    }

    // ---- violações reais no banco de teste ----

    /** Se verificar, o "serviço" confere antes e responde com a mensagem específica. Senão, só o banco decide. */
    @PostMapping("/unico")
    String unico(@RequestBody ValorUnico pedido) {
        if (pedido.verificar()
                && jdbc.queryForObject("SELECT count(*) FROM teste_unico WHERE valor = ?", Long.class,
                        pedido.valor()) > 0) {
            throw new ConflitoDeNegocioException("Já existe um registro com esse valor");
        }
        jdbc.update("INSERT INTO teste_unico (valor) VALUES (?)", pedido.valor());
        return "ok";
    }

    @PostMapping("/periodos-reais")
    String periodoReal(@RequestBody PeriodoDeTeste pedido) {
        jdbc.update("INSERT INTO teste_periodo (periodo) VALUES (tstzrange(?::timestamptz, ?::timestamptz))",
                pedido.inicio().toString(), pedido.termino().toString());
        return "ok";
    }

    @DeleteMapping("/pais/{id}")
    String apagaPai(@PathVariable long id) {
        jdbc.update("DELETE FROM teste_pai WHERE id = ?", id);
        return "ok";
    }

    private static DataIntegrityViolationException dados(String mensagem, String sqlState) {
        return new DataIntegrityViolationException(mensagem, new SQLException(mensagem, sqlState));
    }
}
