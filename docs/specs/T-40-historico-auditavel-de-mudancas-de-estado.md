# Spec T-40: histórico auditável de mudanças de estado

| Campo | Valor |
|---|---|
| Task | T-40 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Liberada |
| Regra crítica | Sim |
| Requisitos | RF-10, RN-09 |
| Característica ISO 25010 | Adequação funcional (rastreabilidade) |
| Criada em | 2026-11-10 |

## 1. Contexto e objetivo

Toda mudança de estado de uma reserva precisa deixar um registro que não se altera nem se apaga (RN-09, regra crítica da especificação). Esta spec define **o que é gravado, quando, em qual transação, como o banco impede a alteração** e quem consulta o histórico (Responsável e Administrador). Na mesa, o professor pede o registro de auditoria de uma reserva cancelada.

## 2. Escopo

**Dentro:**
- Registro de cada mudança de estado: criação, aprovação, rejeição, check-in, cancelamento (as três formas), encerramento pelo sistema e a volta de `APROVADA` a `SOLICITADA` pela alteração.
- Gravação na **mesma transação** da mudança; o rollback não deixa registro órfão.
- Imutabilidade: o banco recusa `UPDATE` e `DELETE` na tabela de auditoria, e a API não tem nenhum endpoint de escrita.
- `GET /api/reservas/{id}/auditoria` (Responsável e Administrador).
- Uma reserva com auditoria não pode ser apagada do banco (chave estrangeira, RN-08-T9).

**Fora (e por quê):**
- A notificação que falha depois da mudança (RN-09-T12): é da T-31.
- O job que muda estados por tempo (T-60): quando existir, usa o mesmo registro, com usuário vazio (sistema).
- Alterar só o horário, sem mudar o estado, não é mudança de estado e não gera registro.

## 3. Regras de negócio

- **RN-09 (oficial):** toda mudança de estado gera auditoria.
- **L07 (conteúdo mínimo, decisão desta spec):** reserva, usuário que fez a mudança, instante (do `Clock`, ADR-0008), estado anterior (vazio na criação), estado novo e um motivo opcional.
- **Usuário vazio = sistema:** mudanças feitas pelo job (T-60) não têm usuário.
- **Mudança de estado inclui a criação** (anterior vazio, novo `SOLICITADA`) e a volta de `APROVADA` a `SOLICITADA` na alteração. Alteração de uma `SOLICITADA` que continua `SOLICITADA` não gera registro.
- **Motivo:** texto curto e fixo, sem dados pessoais: "não aprovada a tempo" (cancelamento pelo sistema por prazo), "encerramento antecipado pelo dono" (`EM_USO → CONCLUIDA` pelo cancelamento), "cancelamento sem check-in" (`APROVADA → NAO_COMPARECEU` pelo dono) e "alteração da reserva aprovada" (`APROVADA → SOLICITADA`). Nos demais casos, vazio.
- **O que é recusado não grava:** transição proibida, pedido recusado por conflito ou por regra não geram registro (RN-09-T6 e T7).
- **Imutável:** a tabela só recebe inserções. A garantia é do banco (gatilho que recusa `UPDATE` e `DELETE`), e não só da aplicação (R-05).
- **DP-02:** só o Responsável e o Administrador consultam o histórico. O Solicitante recebe 403, inclusive o dono.

## 4. Contrato da API

`GET /api/reservas/{id}/auditoria` (Responsável e Administrador): o histórico da reserva, do mais antigo ao mais novo.

```json
[{ "id": 1, "reservaId": 15, "usuarioId": 7, "estadoAnterior": null, "estadoNovo": "SOLICITADA",
   "ocorridaEm": "2026-11-09T08:00:00Z", "motivo": null }]
```

| Situação | Status HTTP | Corpo |
|---|---|---|
| Sucesso | 200 | A lista de registros (nunca vazia para uma reserva existente) |
| Sem autenticação | 401 | `ProblemDetail` neutro (T-22) |
| Solicitante | 403 | `ProblemDetail` |
| Reserva que não existe | 404 | "Recurso não encontrado" |
| `POST`, `PUT`, `PATCH` ou `DELETE` em qualquer rota de auditoria | 403 (401 sem token) | Não existem endpoints de escrita: negado por padrão |

## 5. Critérios de aceite

### CA-01: Criar a reserva grava o primeiro registro

- **Dado** um pedido de reserva aceito
- **Quando** a reserva é gravada
- **Então** existe 1 registro com estado anterior vazio, estado novo `SOLICITADA`, o dono como usuário e o instante do `Clock`

### CA-02: Cada transição grava um registro

- **Dado** uma reserva em um estado de origem
- **Quando** ocorre cada transição permitida (aprovar, rejeitar, check-in, cancelar até o início, cancelar sem check-in depois do início, encerrar antecipado, encerrar pelo sistema, não comparecimento e cancelamento por prazo)
- **Então** existe 1 registro por transição, com o estado anterior, o novo, o usuário que fez (vazio quando foi o sistema) e o motivo da seção 3

### CA-03: A volta de `APROVADA` a `SOLICITADA` conta

- **Dado** uma reserva `APROVADA`
- **Quando** o dono a altera
- **Então** existe 1 registro `APROVADA → SOLICITADA` com o motivo "alteração da reserva aprovada"
- **E** alterar uma `SOLICITADA` sem mudar o estado não gera registro

### CA-04: O que é recusado não grava

- **Dado** uma transição proibida, um pedido recusado por conflito de horário ou por qualquer regra
- **Quando** a recusa acontece
- **Então** nenhum registro de auditoria é gravado

### CA-05: Mesma transação, sem registro órfão

- **Dado** uma mudança de estado em andamento
- **Quando** a gravação da auditoria falha
- **Então** a mudança é desfeita, a reserva mantém o estado anterior e nenhuma auditoria fica gravada
- **E** quando a mudança é que falha, não sobra registro de auditoria

### CA-06: O instante vem do `Clock`

- **Dado** o `Clock` fixo de teste
- **Quando** uma mudança é registrada
- **Então** `ocorrida_em` é o instante do `Clock`, em UTC

### CA-07: O registro é imutável

- **Dado** um registro gravado
- **Quando** alguém tenta alterá-lo ou apagá-lo no banco
- **Então** o banco recusa
- **E** a API não tem endpoint que escreva na auditoria (as quatro escritas recebem 403)

### CA-08: Só o Responsável e o Administrador consultam

- **Dado** a rota `GET /api/reservas/{id}/auditoria`
- **Quando** o Responsável ou o Administrador consulta uma reserva existente
- **Então** recebe 200 com os registros em ordem do mais antigo ao mais novo
- **E** o Solicitante (inclusive o dono) recebe 403, sem token recebe 401 e uma reserva inexistente recebe 404

### CA-09: Reserva com auditoria não se apaga

- **Dado** uma reserva com registros de auditoria
- **Quando** alguém tenta apagá-la direto no banco
- **Então** o banco recusa, e a reserva e a auditoria continuam

### CA-10: O histórico de uma reserva cancelada

- **Dado** uma reserva criada, aprovada e cancelada
- **Quando** o Responsável consulta o histórico
- **Então** vê 3 registros em ordem: criação, `SOLICITADA → APROVADA` e `APROVADA → CANCELADA`, cada um com o usuário certo

## 6. Casos de teste

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 (RN-09-T1) | CA-01 | Classe válida | Criar uma reserva | 1 registro: anterior vazio, novo `SOLICITADA`, dono, instante | Integração |
| TC-02 (RN-09-T2) | CA-02 | Classe válida | O Responsável aprova | 1 registro `SOLICITADA → APROVADA` com o Responsável | Integração |
| TC-03 (RN-09-T3) | CA-02 | Tabela de decisão | Check-in, encerramento, rejeição e cancelamentos (teste parametrizado) | 1 registro por transição, com anterior, novo, usuário e motivo | Unitário |
| TC-04 (RN-09-T4) | CA-02 | Classe válida | `NAO_COMPARECEU`, `CONCLUIDA` e cancelamento por prazo pelo sistema | 1 registro cada, usuário vazio | Unitário |
| TC-05 (RN-09-T5) | CA-02 | Classe válida | Cancelamento por falta de aprovação | Motivo "não aprovada a tempo", usuário vazio | Unitário |
| TC-06 (sem caso no plano) | CA-03 | Classe válida | O dono altera uma `APROVADA` | 1 registro `APROVADA → SOLICITADA` com o motivo | Integração |
| TC-07 (sem caso no plano) | CA-03 | Classe equivalente | O dono altera o horário de uma `SOLICITADA` | Nenhum registro novo | Integração |
| TC-08 (RN-09-T6) | CA-04 | Estado proibido | Transição proibida (`SOLICITADA → EM_USO`) | Recusada; nenhum registro | Unitário |
| TC-09 (RN-09-T7) | CA-04 | Conflito | Pedido recusado por conflito de horário | Nenhuma reserva e nenhum registro | Integração |
| TC-10 (RN-09-T8) | CA-05 | Integração | A gravação da auditoria falha ao aprovar | A reserva continua `SOLICITADA` e não há registro | Integração |
| TC-11 (sem caso no plano) | CA-05 | Integração | A gravação da auditoria falha ao criar | Nenhuma reserva e nenhum registro | Integração |
| TC-12 (RN-09-T9) | CA-06 | Classe válida | Mudança com o `Clock` em 08:00 | `ocorrida_em` = 08:00 UTC | Integração |
| TC-13 (RN-09-T10) | CA-07 | Estado proibido | `UPDATE` e `DELETE` direto na tabela de auditoria | O banco recusa os dois | Integração |
| TC-14 (sem caso no plano) | CA-07 | Classe inválida | `POST`, `PUT`, `PATCH` e `DELETE` em `/api/reservas/{id}/auditoria` | 403 (401 sem token) | Integração |
| TC-15 (sem caso no plano) | CA-08 | Tabela de decisão | Responsável e Administrador consultam o histórico | 200 com os registros em ordem | Integração |
| TC-16 (sem caso no plano) | CA-08 | Conflito | O Solicitante dono consulta; sem token; id inexistente | 403; 401; 404 | Integração |
| TC-17 (RN-08-T9) | CA-09 | Integração | `DELETE` direto no banco de uma reserva com auditoria | Recusado pela chave estrangeira | Integração |
| TC-18 (RN-07-T11, RN-13-T13) | CA-10 | Classe válida | Reserva criada, aprovada e cancelada; consulta do histórico | 3 registros em ordem, com os usuários certos | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Uma mudança de estado fica sem registro (caminho esquecido) | Alto | O registro entra no ponto único da mudança (`grava` do ciclo de vida) e na criação e alteração; TC-03 e TC-04 percorrem todas as transições |
| O registro é gravado e a mudança falha (ou o contrário) | Alto | Mesma transação; TC-10 e TC-11 provam o rollback |
| A auditoria é alterada ou apagada | Alto | Gatilho do banco (TC-13) e nenhum endpoint de escrita (TC-14) |
| O Solicitante lê o histórico dos outros | Alto | CA-08 e TC-16; a matriz de autorização ganha a linha |
| O instante vem do relógio do banco ou da JVM | Médio | `Clock` injetado (CA-06, TC-12) |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-13 e TC-17: o banco (tabela, gatilho, chave estrangeira) | Migração V10 da tabela `auditoria`, com o gatilho que recusa `UPDATE` e `DELETE` | Nomear as restrições |
| 2 | TC-01 e TC-12: criação e instante | Registro da criação com o `Clock` | Extrair o registrador de auditoria |
| 3 | TC-02 a TC-05: transições | Registro no ponto único da mudança de estado | Reaproveitar o ponto único |
| 4 | TC-06 e TC-07: alteração | Registro só quando o estado muda | Nomear a regra |
| 5 | TC-08 e TC-09: o que é recusado | Nada a gravar na recusa | Reaproveitar o cenário |
| 6 | TC-10 e TC-11: rollback | Gravação na mesma transação | Confirmar a fronteira da transação |
| 7 | TC-14 a TC-16 e TC-18: consulta | `GET /api/reservas/{id}/auditoria` com acesso por perfil | Atualizar a matriz de autorização |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Revisão feita pela autora (autoaprovação liberada pelo professor em 2026-11-10; revisor formal pelo rodízio: @Samwel-Merces). Conferi que cada critério tem caso de teste, que a imutabilidade é garantida pelo banco e que o escopo separa a notificação (T-31) e o job (T-60).

**Parecer:** Aprovada · @renatarsilva · 2026-11-10

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-10
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

| # | Pergunta | Resposta / decisão | Documento afetado |
|---|---|---|---|
| 1 | Quem é o usuário de uma mudança feita pelo job? | Usuário vazio significa sistema (o job da T-60 usará isso) | Seção 3 |
| 2 | A volta de `APROVADA` a `SOLICITADA` pela alteração é mudança de estado? | Sim, gera registro. Alterar só o horário, sem mudar o estado, não gera | CA-03 |
| 3 | Onde a auditoria é gravada? | No ponto único da mudança de estado e na criação, na mesma transação: se a auditoria falha, a mudança é desfeita | CA-05, ciclo de vida |
| 4 | Como garantir a imutabilidade? | Gatilho do banco que recusa `UPDATE` e `DELETE`, e nenhum endpoint de escrita; só a aplicação não basta (R-05) | CA-07 |
| 5 | Quem consulta o histórico? | Só o Responsável e o Administrador (DP-02); o Solicitante recebe 403, inclusive o dono | CA-08, matriz |
| 6 | O que vai no motivo? | Texto curto e fixo, sem dados pessoais; vazio na maioria dos casos | Seção 3 |
| 7 | A falha da notificação apaga a auditoria? | Não (RN-09-T12); fica fora desta spec, é da T-31 | Seção 2 |
| 8 | Qual linha da RTM isto preenche? | RN-09 e RF-10: de `sem teste` para `red` e `green`, com a consulta do histórico de uma reserva cancelada como evidência | `docs/RTM.md` |

Decisões confirmadas em bloco por @renatarsilva (dona da task) em 2026-11-10.

**Resultado:** Liberada · 2026-11-10
