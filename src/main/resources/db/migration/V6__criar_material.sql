-- T-18: material (arquitetura 3.1, com a coluna patrimonio que a spec acrescenta). A tabela recurso vem da T-16.
-- Este passo cria só colunas e chaves. O índice único do patrimônio entra na migração seguinte, para os testes do
-- banco terem um red real.
CREATE TABLE material (
    recurso_id    BIGINT       PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    tipo_material VARCHAR(100) NOT NULL,
    patrimonio    VARCHAR(30)  NOT NULL
);
