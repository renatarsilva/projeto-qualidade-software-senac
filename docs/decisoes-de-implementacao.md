# Decisões de implementação

Decisões tomadas durante a implementação para fechar lacunas que as specs e a arquitetura deixaram abertas
(ver a seção 1.8 de [`arquitetura.md`](arquitetura.md)). Continuam a numeração D-xx da arquitetura (D-01 e D-02).
Cada uma diz o que foi decidido, por quê e qual lacuna fecha. Todas ficam com **validação pendente** com o time
e, quando a especificação for omissa, com o professor.

| ID | Decisão | Por quê | Fecha | Onde |
|---|---|---|---|---|
| D-03 | As cópias `inicio`, `fim` e `ocupa` de `reserva_recurso` são mantidas por **gatilhos do banco**: um copia os dados da reserva ao inserir o vínculo e outro propaga cada mudança de horário ou de estado da reserva | A arquitetura apontou o risco de `ocupa` divergir do estado se o código esquecer de atualizar (D-02, R-08). Com o gatilho, a cópia muda na mesma transação e não depende de nenhum chamador | R-08, F10 | `V3__criar_reservas.sql` |
| D-04 | A auditoria só aceita `INSERT`: um gatilho recusa `UPDATE` e `DELETE` | A restrição 6 era garantida só pela aplicação (R-05, L07). O gatilho protege o histórico até de acesso direto ao banco | R-05 | `V4__criar_manutencao_retirada_auditoria.sql` |
| D-05 | Na auditoria, `usuario_id` nulo significa **o sistema** (job periódico), e há uma coluna `motivo` opcional (ex.: "não aprovada a tempo") | O plano TDD pede usuário "sistema" e motivo no cancelamento por prazo, mas o modelo não tinha coluna de motivo nem usuário de sistema (conflito C-12). Criar um usuário falso "sistema" exigiria senha e perfil para quem não faz login | C-12, L07 | `V4__criar_manutencao_retirada_auditoria.sql` |
| D-06 | Tentativas recusadas por sobreposição ou manutenção são gravadas em `conflito_evitado`, numa transação separada da reserva | O RF-12 pede "conflitos evitados", mas a recusa não grava nada (T-20 CA-04), então não havia onde contar | Lacuna do RF-12 | `V4__criar_manutencao_retirada_auditoria.sql` |
