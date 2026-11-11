-- T-40 (green): a auditoria só recebe inserções e acompanha a reserva (arquitetura 3.2, RN-09 e RN-08).
ALTER TABLE auditoria
    ADD CONSTRAINT auditoria_reserva_fk FOREIGN KEY (reserva_id) REFERENCES reserva (id) ON DELETE RESTRICT,
    ADD CONSTRAINT auditoria_usuario_fk FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    ADD CONSTRAINT auditoria_estado_novo_valido CHECK (estado_novo IN
        ('SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA', 'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'));

CREATE INDEX auditoria_reserva_idx ON auditoria (reserva_id);

-- Imutável pelo banco: nenhuma alteração nem exclusão de linha, nem pela aplicação (risco R-05).
CREATE FUNCTION auditoria_somente_insercao() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A auditoria só recebe inserções';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_sem_alteracao
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_somente_insercao();
