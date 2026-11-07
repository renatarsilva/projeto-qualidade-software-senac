-- Reservas (T-20, T-21, T-37). O banco é a última linha de defesa contra a dupla reserva
-- (driver 1 da arquitetura): a restrição de exclusão decide o vencedor entre pedidos simultâneos.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reserva (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitante_id     BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    inicio             TIMESTAMPTZ NOT NULL,
    fim                TIMESTAMPTZ NOT NULL,
    estado             VARCHAR(20) NOT NULL,
    criada_em          TIMESTAMPTZ NOT NULL,
    prazo_aprovacao_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT reserva_fim_depois_do_inicio CHECK (fim > inicio),          -- RN-01
    CONSTRAINT reserva_estado_valido CHECK (estado IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'))
);

-- S-02 / D-01: só estes estados ocupam o horário.
CREATE FUNCTION estado_ocupa(estado VARCHAR) RETURNS BOOLEAN
    LANGUAGE sql IMMUTABLE AS
$$ SELECT estado IN ('SOLICITADA', 'APROVADA', 'EM_USO') $$;

-- D-02: uma reserva ocupa vários recursos (sala, professor e material juntos).
-- inicio, fim e ocupa são CÓPIAS da reserva, porque o EXCLUDE só enxerga colunas da própria tabela.
-- Quem mantém as cópias é o próprio banco (gatilhos abaixo), na mesma transação que muda a reserva:
-- isso fecha o risco R-08 ("ocupa diverge do estado") sem depender de o código lembrar de atualizar.
CREATE TABLE reserva_recurso (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id BIGINT      NOT NULL REFERENCES reserva (id) ON DELETE RESTRICT,
    recurso_id BIGINT      NOT NULL REFERENCES recurso (id) ON DELETE RESTRICT,
    inicio     TIMESTAMPTZ NOT NULL,
    fim        TIMESTAMPTZ NOT NULL,
    ocupa      BOOLEAN     NOT NULL,
    -- S-01: [início, fim) — reservas encostadas não conflitam.
    periodo    TSTZRANGE GENERATED ALWAYS AS (tstzrange(inicio, fim, '[)')) STORED,
    CONSTRAINT reserva_recurso_unico UNIQUE (reserva_id, recurso_id),
    -- Restrição 1 (RN-02, RN-03, RNF-05): sem sobreposição no mesmo recurso entre reservas que ocupam.
    CONSTRAINT sem_sobreposicao EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&) WHERE (ocupa)
);

CREATE FUNCTION copiar_reserva_para_vinculo() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    SELECT r.inicio, r.fim, estado_ocupa(r.estado)
      INTO NEW.inicio, NEW.fim, NEW.ocupa
      FROM reserva r WHERE r.id = NEW.reserva_id;
    RETURN NEW;
END
$$;

CREATE TRIGGER reserva_recurso_copia
    BEFORE INSERT OR UPDATE ON reserva_recurso
    FOR EACH ROW EXECUTE FUNCTION copiar_reserva_para_vinculo();

CREATE FUNCTION propagar_reserva_para_vinculos() RETURNS TRIGGER
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE reserva_recurso
       SET inicio = NEW.inicio, fim = NEW.fim, ocupa = estado_ocupa(NEW.estado)
     WHERE reserva_id = NEW.id;
    RETURN NEW;
END
$$;

CREATE TRIGGER reserva_propaga
    AFTER UPDATE OF inicio, fim, estado ON reserva
    FOR EACH ROW EXECUTE FUNCTION propagar_reserva_para_vinculos();

CREATE INDEX reserva_solicitante ON reserva (solicitante_id);
CREATE INDEX reserva_estado ON reserva (estado);
