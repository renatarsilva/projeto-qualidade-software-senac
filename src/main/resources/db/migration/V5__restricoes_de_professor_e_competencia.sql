-- T-17: as restrições do banco para professor e competência (arquitetura 3.2). O nome do professor já tem o CHECK do padrão
-- e o índice único de nome por tipo, criados pela T-16 (V3).

-- A matrícula tem exatamente 5 dígitos e é única entre os professores (P-06).
ALTER TABLE professor
    ADD CONSTRAINT professor_matricula_formato CHECK (matricula ~ '^[0-9]{5}$');
ALTER TABLE professor
    ADD CONSTRAINT professor_matricula_unica UNIQUE (matricula);

-- A competência é única dentro do mesmo professor, sem diferenciar maiúsculas (P-02).
CREATE UNIQUE INDEX competencia_unica_por_professor ON competencia (professor_id, lower(descricao));
