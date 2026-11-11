-- T-41 (green): índice do período do relatório. Sem chave estrangeira de propósito: o registro é gravado em transação
-- própria enquanto a reserva ainda segura bloqueios do recurso, e uma chave estrangeira bloquearia nessa linha.
CREATE INDEX conflito_evitado_ocorrido_idx ON conflito_evitado (ocorrido_em);
