-- Período de manutenção (RF-08, RN-04). S-04: bloqueio e manutenção são o mesmo mecanismo.
CREATE TABLE periodo_manutencao (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id BIGINT       NOT NULL REFERENCES recurso (id) ON DELETE RESTRICT,
    inicio     TIMESTAMPTZ  NOT NULL,
    fim        TIMESTAMPTZ  NOT NULL,
    motivo     VARCHAR(200) NOT NULL,
    criado_por BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    periodo    TSTZRANGE GENERATED ALWAYS AS (tstzrange(inicio, fim, '[)')) STORED,
    CONSTRAINT manutencao_fim_depois_do_inicio CHECK (fim > inicio),
    -- Restrição 4: manutenções do mesmo recurso não se sobrepõem.
    CONSTRAINT manutencao_sem_sobreposicao EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&)
);

-- Retirada e devolução (RF-09). Restrição 7: uma retirada por recurso reservado,
-- e a devolução nunca antes da retirada.
CREATE TABLE retirada (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_recurso_id BIGINT      NOT NULL REFERENCES reserva_recurso (id) ON DELETE RESTRICT,
    registrada_por     BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    retirada_em        TIMESTAMPTZ NOT NULL,
    devolvida_em       TIMESTAMPTZ,
    devolucao_registrada_por BIGINT REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT retirada_unica UNIQUE (reserva_recurso_id),
    CONSTRAINT devolucao_depois_da_retirada CHECK (devolvida_em IS NULL OR devolvida_em >= retirada_em)
);

-- Auditoria (RF-10, RN-09). usuario_id nulo = o sistema (job periódico), que não é um usuário
-- (decisão D-05 em docs/decisoes-de-implementacao.md, resolve o conflito C-12 do catálogo).
CREATE TABLE auditoria (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id      BIGINT       NOT NULL REFERENCES reserva (id) ON DELETE RESTRICT,
    usuario_id      BIGINT       REFERENCES usuario (id) ON DELETE RESTRICT,
    estado_anterior VARCHAR(20),
    estado_novo     VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(200),
    ocorrida_em     TIMESTAMPTZ  NOT NULL
);
CREATE INDEX auditoria_reserva ON auditoria (reserva_id);

-- Restrição 6: a auditoria só recebe inserções. Reforçado no banco (fecha a lacuna R-05):
-- nem um bug, nem um acesso direto conseguem alterar ou apagar o histórico.
CREATE FUNCTION auditoria_somente_insercao() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'auditoria aceita apenas insercoes' USING ERRCODE = 'restrict_violation';
END
$$;

CREATE TRIGGER auditoria_imutavel
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_somente_insercao();

-- Conflitos evitados (RF-12): tentativa de reserva recusada por sobreposição ou manutenção.
-- A recusa não grava a reserva; este registro é gravado à parte só para o relatório (D-06).
CREATE TABLE conflito_evitado (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id     BIGINT      REFERENCES recurso (id) ON DELETE RESTRICT,
    solicitante_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    motivo         VARCHAR(20) NOT NULL,
    inicio         TIMESTAMPTZ NOT NULL,
    fim            TIMESTAMPTZ NOT NULL,
    ocorrido_em    TIMESTAMPTZ NOT NULL,
    CONSTRAINT conflito_motivo_valido CHECK (motivo IN ('SOBREPOSICAO', 'MANUTENCAO'))
);
