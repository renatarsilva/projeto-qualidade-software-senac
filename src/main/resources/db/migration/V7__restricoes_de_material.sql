-- T-18: o patrimônio é único entre os materiais, sem diferenciar maiúsculas (P-01, arquitetura 3.2, restrição 9).
-- O nome único por tipo (índice de recurso) já foi criado pela T-16 (V3).
CREATE UNIQUE INDEX material_patrimonio_unico ON material (lower(patrimonio));
