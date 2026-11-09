-- T-16: recurso (o que é comum a sala, professor e material) e sala (arquitetura 3.1, D-02).
-- Este ciclo cria só colunas e chaves. As restrições (CHECK de tipo, de padrão do nome e da capacidade,
-- índice único do nome) entram na migração do ciclo 7, para os testes do banco terem um red real.
CREATE TABLE recurso (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo     VARCHAR(20)  NOT NULL,
    nome     VARCHAR(100) NOT NULL,
    restrito BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE sala (
    recurso_id  BIGINT       PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    capacidade  INTEGER      NOT NULL,
    localizacao VARCHAR(100) NOT NULL,
    tipo_sala   VARCHAR(100) NOT NULL
);
