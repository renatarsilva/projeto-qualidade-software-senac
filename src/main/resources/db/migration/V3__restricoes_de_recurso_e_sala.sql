-- T-16 (ciclo 7): as restrições do banco, a última linha de defesa (arquitetura 3.2, restrições 3, 8, 10, 11, 12 e 14).
-- O tipo do recurso só aceita os valores válidos.
ALTER TABLE recurso
    ADD CONSTRAINT recurso_tipo_valido CHECK (tipo IN ('SALA', 'PROFESSOR', 'MATERIAL'));

-- O padrão do nome vale só para o tipo certo: sala (RN-16) e professor (RN-17). Material tem nome livre.
ALTER TABLE recurso
    ADD CONSTRAINT recurso_nome_sala_padrao CHECK (tipo <> 'SALA' OR nome ~ '^sala-[0-9]{2,3}$');
ALTER TABLE recurso
    ADD CONSTRAINT recurso_nome_professor_padrao CHECK (tipo <> 'PROFESSOR' OR nome ~ '^[a-z]+-[a-z]+-[0-9]{5}$');

-- Nome único por tipo, sem diferenciar maiúsculas. É um índice, porque o PostgreSQL não aceita expressão em UNIQUE de tabela.
CREATE UNIQUE INDEX recurso_tipo_nome_unico ON recurso (tipo, lower(nome));

-- A capacidade da sala é sempre maior que zero.
ALTER TABLE sala
    ADD CONSTRAINT sala_capacidade_positiva CHECK (capacidade > 0);
