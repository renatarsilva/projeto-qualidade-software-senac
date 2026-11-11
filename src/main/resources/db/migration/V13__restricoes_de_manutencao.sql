-- T-35 (green): restrições do período de manutenção (arquitetura 3.2, restrição 4). Manutenções do mesmo recurso
-- não se sobrepõem, e um recurso com manutenção não pode ser apagado.
ALTER TABLE periodo_manutencao
    ADD CONSTRAINT periodo_manutencao_recurso_fk FOREIGN KEY (recurso_id) REFERENCES recurso (id) ON DELETE RESTRICT,
    ADD CONSTRAINT periodo_manutencao_nao_vazio CHECK (NOT isempty(periodo)),
    ADD CONSTRAINT periodo_manutencao_sem_sobreposicao EXCLUDE USING gist (recurso_id WITH =, periodo WITH &&);
