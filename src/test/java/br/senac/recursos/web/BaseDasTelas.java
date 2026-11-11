package br.senac.recursos.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import br.senac.recursos.TesteDeApiComBanco;
import br.senac.recursos.material.MaterialRequest;
import br.senac.recursos.material.MaterialService;
import br.senac.recursos.professor.ProfessorRequest;
import br.senac.recursos.professor.ProfessorService;
import br.senac.recursos.sala.SalaRequest;
import br.senac.recursos.sala.SalaService;
import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.UsuarioRequest;
import br.senac.recursos.seguranca.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Preparação comum dos testes das telas (spec T-44): banco limpo, um usuário de cada perfil e atalhos para criar
 * recursos. O relógio é o fixo dos testes (segunda, 2026-11-09, 08:00 UTC), então as reservas usam a terça seguinte.
 * Os horários digitados estão no horário de Brasília (UTC-3).
 */
abstract class BaseDasTelas {

    static final String SENHA = "senha-segura-123";
    static final String SUFIXO = "@telas.test";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UsuarioService usuarios;

    @Autowired
    protected SalaService salas;

    @Autowired
    protected MaterialService materiais;

    @Autowired
    protected ProfessorService professores;

    protected long solicitante;
    protected long outroSolicitante;
    protected long responsavel;
    protected long administrador;

    @BeforeEach
    void prepararAsTelas() {
        limpa();
        solicitante = usuario("marina", Perfil.SOLICITANTE);
        outroSolicitante = usuario("rafael", Perfil.SOLICITANTE);
        responsavel = usuario("carlos", Perfil.RESPONSAVEL);
        administrador = usuario("admin", Perfil.ADMINISTRADOR);
    }

    @AfterEach
    void limparAsTelas() {
        limpa();
    }

    protected long usuario(String apelido, Perfil perfil) {
        return usuarios.cria(new UsuarioRequest("Pessoa " + apelido, apelido + SUFIXO, SENHA, perfil)).getId();
    }

    protected RequestPostProcessor como(long id, Perfil perfil) {
        return user(String.valueOf(id)).roles(perfil.name());
    }

    protected RequestPostProcessor comoSolicitante() {
        return como(solicitante, Perfil.SOLICITANTE);
    }

    protected RequestPostProcessor comoOutroSolicitante() {
        return como(outroSolicitante, Perfil.SOLICITANTE);
    }

    protected RequestPostProcessor comoResponsavel() {
        return como(responsavel, Perfil.RESPONSAVEL);
    }

    protected RequestPostProcessor comoAdministrador() {
        return como(administrador, Perfil.ADMINISTRADOR);
    }

    protected long sala(String nome, int capacidade, String localizacao) {
        return salas.cadastra(new SalaRequest(nome, "Laboratório", capacidade, localizacao, false)).getId();
    }

    protected long material(String nome, String patrimonio) {
        return materiais.cadastra(new MaterialRequest(nome, "Projetor", patrimonio, false)).id();
    }

    protected long professor(String nome, String... competencias) {
        return professores.cadastra(new ProfessorRequest(nome, false, List.of(competencias))).id();
    }

    protected String estadoDa(long reserva) {
        List<String> estados = jdbc.queryForList("SELECT estado FROM reserva WHERE id = ?", String.class, reserva);
        return estados.isEmpty() ? "(sem reserva)" : estados.get(0);
    }

    protected int totalDeReservas() {
        return jdbc.queryForObject("SELECT count(*) FROM reserva", Integer.class);
    }

    protected long ultimaReserva() {
        return maiorId("reserva");
    }

    /** O maior id da tabela, ou -1 se ela estiver vazia (o teste então falha na asserção, e não com exceção). */
    protected long maiorId(String tabela) {
        Long maior = jdbc.queryForObject("SELECT max(id) FROM " + tabela, Long.class);
        return maior == null ? -1 : maior;
    }

    private void limpa() {
        jdbc.update("DELETE FROM conflito_evitado");
        jdbc.update("DELETE FROM retirada");
        // TRUNCATE não dispara o gatilho de imutabilidade (a auditoria só recebe inserções).
        jdbc.execute("TRUNCATE TABLE auditoria");
        jdbc.update("DELETE FROM periodo_manutencao");
        jdbc.update("DELETE FROM reserva_recurso");
        jdbc.update("DELETE FROM reserva");
        jdbc.update("DELETE FROM usuario WHERE email LIKE ?", "%" + SUFIXO);
        jdbc.update("DELETE FROM material");
        jdbc.update("DELETE FROM competencia");
        jdbc.update("DELETE FROM professor");
        jdbc.update("DELETE FROM sala");
        jdbc.update("DELETE FROM recurso");
    }
}
