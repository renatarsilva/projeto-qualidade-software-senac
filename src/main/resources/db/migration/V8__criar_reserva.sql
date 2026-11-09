-- T-20 (red): só colunas e chaves da reserva (arquitetura 3.1). As restrições (CHECKs e o EXCLUDE anti-conflito,
-- que precisa da extensão btree_gist) entram na T-23 (green).
CREATE TABLE reserva (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitante_id     BIGINT      NOT NULL REFERENCES usuario (id),
    inicio             TIMESTAMPTZ NOT NULL,
    fim                TIMESTAMPTZ NOT NULL,
    estado             VARCHAR(20) NOT NULL,
    criada_em          TIMESTAMPTZ NOT NULL,
    prazo_aprovacao_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE reserva_recurso (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id BIGINT    NOT NULL REFERENCES reserva (id),
    recurso_id BIGINT    NOT NULL REFERENCES recurso (id),
    periodo    TSTZRANGE NOT NULL,
    ocupa      BOOLEAN   NOT NULL
);
