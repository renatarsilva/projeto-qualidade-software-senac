-- T-42 (red): só colunas da retirada de material (arquitetura 3.1). As chaves estrangeiras, o CHECK e o índice da
-- retirada aberta entram na migração seguinte (green). A retirada guarda a reserva e o material, e não o vínculo
-- reserva_recurso, que é recriado quando a reserva é alterada.
CREATE TABLE retirada (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id    BIGINT      NOT NULL,
    material_id   BIGINT      NOT NULL,
    registrada_por BIGINT     NOT NULL,
    retirada_em   TIMESTAMPTZ NOT NULL,
    devolvida_em  TIMESTAMPTZ
);
