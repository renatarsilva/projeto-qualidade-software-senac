-- T-41 (red): só colunas do registro de conflito evitado (arquitetura 3.1, RF-12). A chave estrangeira e o índice
-- entram na migração seguinte (green). Cada linha é uma tentativa de reserva recusada por sobreposição.
CREATE TABLE conflito_evitado (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id  BIGINT      NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL
);
