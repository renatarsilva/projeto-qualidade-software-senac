-- T-35 (red): só colunas do período de manutenção (arquitetura 3.1). A chave estrangeira, o EXCLUDE e os CHECKs
-- entram na migração seguinte (green), para os testes do banco terem um red real.
CREATE TABLE periodo_manutencao (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recurso_id BIGINT       NOT NULL,
    periodo    TSTZRANGE    NOT NULL,
    motivo     VARCHAR(100) NOT NULL
);
