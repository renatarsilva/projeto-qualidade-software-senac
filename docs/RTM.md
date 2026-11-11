# RTM: matriz de rastreabilidade

Liga cada requisito ao risco que ele protege, aos casos de teste que o comprovam, à task responsável e à evidência. A meta do RNF-03 é **100 % dos requisitos críticos com evidência** (as linhas em **negrito**, RN-01 a RN-09).

> **Estado em 2026-11-07:** nenhum teste foi escrito ainda. Toda linha está `sem teste` e toda evidência está "sem evidência ainda". Quem escreve um teste atualiza a linha na mesma PR (task T-32 criou a estrutura; T-39 e T-50 fecham a matriz).

## Como ler

| Coluna | O que vai nela |
|---|---|
| Requisito | ID do [PRD](prd.md) (RF, RN ou RNF) |
| Risco / cenário | Fatores de qualidade (F-xx) e riscos (R-xx) da [arquitetura](arquitetura.md); cenários CEN e casos CT da [utility tree](atam/utility-tree-inicial.md) |
| Casos de teste | IDs do [plano TDD](testes/plano-tdd.md) (`RN-xx-Tn`) e casos `TC-nn` das specs em `docs/specs/` |
| Task | Task do [backlog](planejamento/backlog.md) que escreve ou fecha o teste |
| Status | `sem teste` → `red` (teste escrito e falhando por asserção) → `green` (teste passando) |
| Evidência | Arquivo, run do CI ou comando que prova. Enquanto não existir, fica "sem evidência ainda" |

## Regras de negócio críticas (RN-01 a RN-09)

| Requisito | Risco / cenário | Casos de teste | Task | Status | Evidência |
|---|---|---|---|---|---|
| **RN-01** término posterior ao início | — | RN-01-T1 a T15; specs T-20 | T-20, T-23; T-16, T-17, T-18 (cadastro) | sem teste | sem evidência ainda (T-20) |
| **RN-02** sem sobreposição no mesmo recurso | F1 · CEN-01 · CT-01 · R-02 · R-08 | RN-02-T1 a T20; CT-01 (V01 a V21); specs T-20 | T-20, T-23, T-28, T-30 | sem teste | sem evidência ainda (T-20, T-28, T-30) |
| **RN-03** duas solicitações simultâneas, uma reserva | F2 · CEN-02 · CT-02 | RN-03-T1 a T9; CT-02 | T-37 (base: T-30) | sem teste | sem evidência ainda (T-37) |
| **RN-04** recurso em manutenção não é reservado | F9 · CEN-04 · R-01 | RN-04-T1 a T11 | T-35 | green | `ManutencaoDeRecursosApiTest`, `ManutencaoEReservaSimultaneasApiTest` (T-35) |
| **RN-05** fluxo principal de estados | F3 | RN-05-T1 a T16; specs T-21 | T-21, T-24, T-59 (check-in) | sem teste | sem evidência ainda (T-21) |
| **RN-06** estados alternativos | F3 · F6 | RN-06-T1 a T17; specs T-21 | T-21, T-24, T-60 (job) | sem teste | sem evidência ainda (T-21) |
| **RN-07** só o Responsável aprova | F4 | RN-07-T1 a T15; specs T-21 | T-21, T-34, T-49 | sem teste | sem evidência ainda (T-34, T-49) |
| **RN-08** reserva iniciada não se apaga | — | RN-08-T1 a T11 | T-21, T-30, T-61 | sem teste | sem evidência ainda (T-21, T-61) |
| **RN-09** toda mudança de estado gera auditoria | F6 · R-05 | RN-09-T1 a T12 | T-40 | green | `AuditoriaDasMudancasTest`, `AuditoriaApiTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest`; histórico de uma reserva cancelada (TC-18) |

## Regras do time (RN-13 a RN-17)

Não vêm da especificação oficial: foram decididas pelo time e registradas no PRD.

| Requisito | Risco / cenário | Casos de teste | Task | Status | Evidência |
|---|---|---|---|---|---|
| RN-13 cancelamento depois do início | F3 | RN-13-T1 a T22; specs T-21 | T-21, T-59 | sem teste | sem evidência ainda (T-21) |
| RN-14 prazos de aprovação e de alteração (24 h e 1 h) | F3 · F12 | RN-14-T1 a T11; specs T-21 | T-21, T-60 | sem teste | sem evidência ainda (T-21, T-60) |
| RN-15 antecedência mínima de 1 hora | F1 | RN-15-T1 a T10; specs T-20 | T-20, T-23 | sem teste | sem evidência ainda (T-20) |
| RN-16 nome da sala no padrão `sala-NN` | — | RN-16-T1 a T9; specs T-16 | T-16 | sem teste | sem evidência ainda (T-16) |
| RN-17 nome do professor com matrícula | — | RN-17-T1 a T11; specs T-17 | T-17 | sem teste | sem evidência ainda (T-17) |

## Requisitos funcionais (RF-01 a RF-14)

| Requisito | Risco / cenário | Casos de teste | Task | Status | Evidência |
|---|---|---|---|---|---|
| RF-01 autenticação e autorização por perfil | F5 · limitador "falha de autorização" | specs T-15 (TC-01 a TC-43); matriz perfil × operação | T-15, T-48, T-49 | sem teste | sem evidência ainda (T-15, T-49) |
| RF-02 cadastro e consulta de salas, professores e materiais | — | specs T-16 (TC-01 a TC-41), T-17 (TC-01 a TC-42), T-18 (TC-01 a TC-33) | T-16, T-17, T-18 | sem teste | sem evidência ainda (T-16, T-17, T-18) |
| RF-03 pesquisa de recursos | — | sem casos escritos | T-43, T-45 | sem teste | sem evidência ainda (T-43) |
| RF-04 criar, alterar e cancelar reservas | F3 | RN-01, RN-05, RN-06, RN-08, RN-13 a RN-15 | T-20, T-21, T-23, T-24 | sem teste | sem evidência ainda (T-23, T-24) |
| RF-05 sobreposição, inclusive na agenda do professor | F1 · CEN-01 · CT-01 | RN-02 | T-20, T-23, T-28 | sem teste | sem evidência ainda (T-20) |
| RF-06 proteção contra dupla reserva | F2 · CEN-02 · CT-02 | RN-03 | T-37, T-38 | sem teste | sem evidência ainda (T-37) |
| RF-07 aprovação de recurso restrito | F4 · CEN-03 · CT-03 | RN-07 | T-33, T-34, T-36 | sem teste | sem evidência ainda (T-34) |
| RF-08 bloqueio por manutenção | F9 · R-01 | RN-04 | T-33, T-35, T-36 | green | `ManutencaoDeRecursosApiTest`, matriz de autorização (MT-01 a MT-03) |
| RF-09 retirada e devolução de materiais | — | sem casos escritos | T-42 | sem teste | sem evidência ainda (T-42) |
| RF-10 histórico auditável | F6 · R-05 | RN-09 | T-40 | green | `AuditoriaDasMudancasTest`, `AuditoriaApiTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest`; histórico de uma reserva cancelada (TC-18) |
| RF-11 notificação simulada ou API externa | F7 · F8 · CEN-03 · CT-03 · R-07 | INT-01 a INT-05 e TC-01 a TC-16 da spec T-31; CT-03 | T-31 | green | `NotificadorHttpWireMockTest`, `NotificacaoDasMudancasApiTest`, `NotificacaoAposOCommitTest` (WireMock, suíte offline) |
| RF-12 relatório de utilização | — | sem casos escritos (oráculo calculado à mão) | T-41 | sem teste | sem evidência ainda (T-41) |
| RF-13 interface responsiva e mensagens de erro compreensíveis | — | specs T-22 (TC-01 a TC-47) | T-22, T-44, T-45, T-46, T-47 | sem teste | sem evidência ainda (T-22) |
| RF-14 documentação da API | — | sem casos | T-51 | sem teste | sem evidência ainda (T-51) |

## Requisitos não funcionais (RNF-01 a RNF-11)

| Requisito | Meta | Risco / cenário | Task | Status | Evidência |
|---|---|---|---|---|---|
| RNF-01 cobertura de linhas | ≥ 80 % | — | T-27 | sem teste | sem evidência ainda (T-27) |
| RNF-02 cobertura de branches | ≥ 70 % | — | T-27, T-28 | sem teste | sem evidência ainda (T-27) |
| RNF-03 rastreabilidade | 100 % dos críticos na RTM | — | T-32, T-39, T-50 | sem teste | este arquivo (completa em T-50) |
| RNF-04 defeitos críticos | 0 | — | T-29 | sem teste | sem evidência ainda (T-29) |
| RNF-05 dupla reserva | 1 aceita, 0 duplicidades | F2 · CEN-02 | T-37, T-38 | sem teste | sem evidência ainda (T-37) |
| RNF-06 autorização por perfil | 0 operações não autorizadas | F5 | T-15, T-48, T-49 | sem teste | sem evidência ainda (T-49) |
| RNF-07 validação de entrada | 0 gravações inválidas | — | T-16 a T-18, T-20, T-22 | sem teste | sem evidência ainda (T-22) |
| RNF-08 tratamento seguro de erros | 0 respostas com detalhe interno | F14 | T-22, T-49 | sem teste | sem evidência ainda (T-22) |
| RNF-09 desempenho | p95 ≤ 300 ms com 50 usuários (candidata) | F11 · R-04 | T-38 | sem teste | sem evidência ainda (T-38) |
| RNF-10 resiliência da notificação | 0 reservas desfeitas | F7 · F8 · R-07 | T-31 | sem teste | sem evidência ainda (T-31) |
| RNF-11 responsividade | utilizável a partir de 375 px (candidata) | — | T-44 | sem teste | sem evidência ainda (T-44) |

## Pendências desta matriz

- Os IDs `RN-xx-Tn` seguem o plano TDD **atualizado** (PR #33); se o PR ainda não foi mergeado, confira o intervalo de cada regra lá.
- As regras RN-10 a RN-12 (propostas) não têm task no backlog e por isso não entram na matriz.
- Atualizar cada linha para `red` e `green` conforme os testes são escritos (T-20, T-21, T-23, T-24, T-34 a T-37, T-40, T-49).
