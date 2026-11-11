-- T-40 (red): só colunas da auditoria (arquitetura 3.1). As chaves estrangeiras e o gatilho de imutabilidade
-- entram na migração seguinte (green), para os testes do banco terem um red real.
CREATE TABLE auditoria (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reserva_id      BIGINT      NOT NULL,
    usuario_id      BIGINT,
    estado_anterior VARCHAR(20),
    estado_novo     VARCHAR(20) NOT NULL,
    ocorrida_em     TIMESTAMPTZ NOT NULL,
    motivo          VARCHAR(100)
);
