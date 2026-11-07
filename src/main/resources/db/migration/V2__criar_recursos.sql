-- Recursos reserváveis (T-16, T-17, T-18). Sala, professor e material são tipos de recurso:
-- `recurso` guarda o que é comum e cada tipo tem a sua tabela (arquitetura, seção 3.1).
-- Assim reserva, manutenção e a restrição anti-conflito apontam sempre para um só recurso_id.
CREATE TABLE recurso (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo     VARCHAR(20)  NOT NULL,
    nome     VARCHAR(100) NOT NULL,
    restrito BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT recurso_tipo_valido CHECK (tipo IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    -- RN-16: toda sala se chama sala-NN (2 ou 3 dígitos)
    CONSTRAINT recurso_nome_sala CHECK (tipo <> 'SALA' OR nome ~ '^sala-[0-9]{2,3}$'),
    -- RN-17: professor = primeironome-ultimonome-matricula (5 dígitos)
    CONSTRAINT recurso_nome_professor CHECK (tipo <> 'PROFESSOR' OR nome ~ '^[a-z]+-[a-z]+-[0-9]{5}$')
);
-- Nome único por tipo sem diferenciar maiúsculas (restrição 8). O PostgreSQL não aceita
-- expressão em UNIQUE de tabela, por isso é um índice único.
CREATE UNIQUE INDEX recurso_nome_unico ON recurso (tipo, lower(nome));

CREATE TABLE sala (
    recurso_id  BIGINT       PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    capacidade  INTEGER      NOT NULL,
    localizacao VARCHAR(100) NOT NULL,
    tipo_sala   VARCHAR(100) NOT NULL,
    CONSTRAINT sala_capacidade_positiva CHECK (capacidade > 0)
);

CREATE TABLE professor (
    recurso_id BIGINT     PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    matricula  VARCHAR(5) NOT NULL,
    CONSTRAINT professor_matricula_unica UNIQUE (matricula)
);

-- Competências saem junto com o professor (são parte dele, não têm vida própria).
CREATE TABLE competencia (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    professor_id BIGINT      NOT NULL REFERENCES professor (recurso_id) ON DELETE CASCADE,
    descricao    VARCHAR(80) NOT NULL
);
CREATE UNIQUE INDEX competencia_unica_por_professor ON competencia (professor_id, lower(descricao));

CREATE TABLE material (
    recurso_id    BIGINT       PRIMARY KEY REFERENCES recurso (id) ON DELETE RESTRICT,
    tipo_material VARCHAR(100) NOT NULL,
    patrimonio    VARCHAR(30)  NOT NULL
);
-- Restrição 9: patrimônio único entre materiais, sem diferenciar maiúsculas.
CREATE UNIQUE INDEX material_patrimonio_unico ON material (lower(patrimonio));
