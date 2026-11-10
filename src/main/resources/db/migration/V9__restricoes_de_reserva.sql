-- T-23 (green): restrições da reserva (arquitetura 3.2). O EXCLUDE é a última linha de defesa contra a dupla reserva:
-- o banco recusa dois vínculos que ocupam o mesmo recurso em períodos que se sobrepõem, mesmo numa corrida.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reserva
    ADD CONSTRAINT reserva_intervalo_valido CHECK (fim > inicio),
    ADD CONSTRAINT reserva_estado_valido CHECK (estado IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'));

ALTER TABLE reserva_recurso
    ADD CONSTRAINT reserva_recurso_unico UNIQUE (reserva_id, recurso_id),
    ADD CONSTRAINT reserva_recurso_sem_sobreposicao
        EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&) WHERE (ocupa);

CREATE INDEX reserva_solicitante_idx ON reserva (solicitante_id);
