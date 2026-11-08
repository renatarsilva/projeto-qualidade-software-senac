-- T-17: professor e competência (arquitetura 3.1). A tabela recurso e as restrições dela vêm da T-16 (V2 e V3).
-- Este passo cria só colunas e chaves. As restrições (matrícula única, competência única por professor)
-- entram na migração seguinte, para os testes do banco terem um red real.
CREATE TABLE professor (
    recurso_id BIGINT     PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    matricula  VARCHAR(5) NOT NULL
);

CREATE TABLE competencia (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    professor_id BIGINT       NOT NULL REFERENCES professor (recurso_id) ON DELETE CASCADE,
    descricao    VARCHAR(80)  NOT NULL
);
