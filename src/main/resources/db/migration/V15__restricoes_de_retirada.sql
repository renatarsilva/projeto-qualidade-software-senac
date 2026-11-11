-- T-42 (green): restrições da retirada de material (arquitetura 3.2). Uma retirada aberta por material e reserva,
-- e a devolução nunca é anterior à retirada. Reserva com retirada não pode ser apagada.
ALTER TABLE retirada
    ADD CONSTRAINT retirada_reserva_fk FOREIGN KEY (reserva_id) REFERENCES reserva (id) ON DELETE RESTRICT,
    ADD CONSTRAINT retirada_material_fk FOREIGN KEY (material_id) REFERENCES recurso (id) ON DELETE RESTRICT,
    ADD CONSTRAINT retirada_usuario_fk FOREIGN KEY (registrada_por) REFERENCES usuario (id),
    ADD CONSTRAINT retirada_devolucao_valida CHECK (devolvida_em IS NULL OR devolvida_em >= retirada_em);

CREATE UNIQUE INDEX retirada_aberta_unica ON retirada (reserva_id, material_id) WHERE devolvida_em IS NULL;
