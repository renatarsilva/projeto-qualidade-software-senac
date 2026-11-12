# RTM: matriz de rastreabilidade

Liga cada requisito ao risco que ele protege, aos casos de teste que o comprovam, à task responsável e à evidência. A meta do RNF-03 é **100 % dos requisitos críticos com evidência** (as linhas em **negrito**, RN-01 a RN-09).

> **Estado em 2026-11-11 (entrega):** 1043 testes automatizados passando; cada linha aponta a classe de teste ou o documento que a comprova. Quem escreve um teste atualiza a linha na mesma PR (task T-32 criou a estrutura; T-39 e T-50 fecham a matriz).

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
| **RN-01** término posterior ao início | — | RN-01-T1 a T15; specs T-20 | T-20, T-23; T-16, T-17, T-18 (cadastro) | green | `IntervaloDaReservaTest` (TC-01 a TC-08), `CriacaoDeReservaApiTest` (TC-29), `CadastroDeSalaValidacaoApiTest` e os testes de cadastro |
| **RN-02** sem sobreposição no mesmo recurso | F1 · CEN-01 · CT-01 · R-02 · R-08 | RN-02-T1 a T20; CT-01 (V01 a V21); specs T-20 | T-20, T-23, T-28, T-30 | green | `SobreposicaoNoMesmoRecursoTest` (V01 a V21 do CT-01), `AlteracaoDeReservaTest`, `EstadosQueOcupamOHorarioTest`, `ConflitoNoBancoDeReservaTest` (EXCLUDE do banco) |
| **RN-03** duas solicitações simultâneas, uma reserva | F2 · CEN-02 · CT-02 | RN-03-T1 a T9; CT-02 | T-37 (base: T-30) | green | `DisputaPelaMesmaSalaConcorrenciaTest` (20 solicitantes, 5 repetições), `ProtecaoDoBancoConcorrenciaTest`, [T-37-vermelho.md](evidencias/T-37-vermelho.md), [T-37-verde.md](evidencias/T-37-verde.md), [ADR-0010](adr/0010-protecao-contra-dupla-reserva-no-banco.md); carga: [resultado-01.md](performance/resultado-01.md) |
| **RN-04** recurso em manutenção não é reservado | F9 · CEN-04 · R-01 | RN-04-T1 a T11 | T-35 | green | `ManutencaoDeRecursosApiTest`, `ManutencaoEReservaSimultaneasApiTest` (T-35) |
| **RN-05** fluxo principal de estados | F3 | RN-05-T1 a T16; specs T-21 | T-21, T-24, T-59 (check-in) | green | `TransicoesDoFluxoPrincipalTest`, `MatrizDeTransicoesTest` (49 combinações, 7 aceitas), `CheckinDaReservaTest`, `CheckinApiTest`. O job periódico que aplica as mudanças por tempo (T-60) ficou pendente |
| **RN-06** estados alternativos | F3 · F6 | RN-06-T1 a T17; specs T-21 | T-21, T-24, T-60 (job) | green | `CaminhosAlternativosTest`, `CancelamentoDaReservaTest`, `PrazosDaReservaTest`. A regra aceita as transições por tempo; o job periódico (T-60) ficou pendente |
| **RN-07** só o Responsável aprova | F4 | RN-07-T1 a T15; specs T-21 | T-21, T-34, T-49 | green | `AutorizacaoDoPatchApiTest` (TC-54, TC-68 a TC-79), `CicloDeVidaApiTest`, matriz de autorização. A feature Cucumber da aprovação (T-33, T-36) ficou pendente |
| **RN-08** reserva iniciada não se apaga | — | RN-08-T1 a T11 | T-21, T-30, T-61 | green | `ExclusaoDeReservaTest`, `CicloDeVidaApiTest` (TC-43, DELETE devolve 409), `ImutabilidadeDaAuditoriaTest` (TC-17, chave estrangeira), `ExclusaoDeProfessorApiTest` e `ExclusaoDeMaterialApiTest` (409 com vínculos) |
| **RN-09** toda mudança de estado gera auditoria | F6 · R-05 | RN-09-T1 a T12 | T-40 | green | `AuditoriaDasMudancasTest`, `AuditoriaApiTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest`; histórico de uma reserva cancelada (TC-18) |

## Regras do time (RN-13 a RN-17)

Não vêm da especificação oficial: foram decididas pelo time e registradas no PRD.

| Requisito | Risco / cenário | Casos de teste | Task | Status | Evidência |
|---|---|---|---|---|---|
| RN-13 cancelamento depois do início | F3 | RN-13-T1 a T22; specs T-21 | T-21, T-59 | green | `CancelamentoDaReservaTest` (TC-19 a TC-39), `CicloDeVidaApiTest` (TC-20, TC-25, TC-44, TC-61) |
| RN-14 prazos de aprovação e de alteração (24 h e 1 h) | F3 · F12 | RN-14-T1 a T11; specs T-21 | T-21, T-60 | green | `PrazosDaReservaTest`, `PrazoDeAlteracaoApiTest` (TC-83), `AutorizacaoDoPatchApiTest` (TC-79). O cancelamento automático por prazo (job, T-60) ficou pendente |
| RN-15 antecedência mínima de 1 hora | F1 | RN-15-T1 a T10; specs T-20 | T-20, T-23 | green | `DonoEAntecedenciaDaReservaTest`, `CriacaoDeReservaApiTest` (TC-47, TC-55) |
| RN-16 nome da sala no padrão `sala-NN` | — | RN-16-T1 a T9; specs T-16 | T-16 | green | `NomeDaSalaTest`, `RestricoesDoBancoDeRecursoTest`, `CadastroDeSalaValidacaoApiTest` |
| RN-17 nome do professor com matrícula | — | RN-17-T1 a T11; specs T-17 | T-17 | green | `NomeDoProfessorTest`, `RestricoesDoBancoDeProfessorTest`, `CadastroDeProfessorApiTest` |

## Requisitos funcionais (RF-01 a RF-14)

| Requisito | Risco / cenário | Casos de teste | Task | Status | Evidência |
|---|---|---|---|---|---|
| RF-01 autenticação e autorização por perfil | F5 · limitador "falha de autorização" | specs T-15 (TC-01 a TC-43) e T-44 (TC-05 a TC-11); matriz perfil × operação, da API e das telas | T-15, T-44, T-48, T-49 | green | `LoginApiTest`, `AutenticacaoDaApiTest`, `AutorizacaoPorPerfilTest`, `GestaoDeUsuariosApiTest`, `MatrizDeAutorizacaoApiTest` (176 casos), [matriz](seguranca/matriz-autorizacao.md), [T-49](evidencias/T-49-matriz-de-autorizacao.md). As telas de login com sessão e CSRF ficaram pendentes |
| RF-02 cadastro e consulta de salas, professores e materiais | — | specs T-16 (TC-01 a TC-41), T-17 (TC-01 a TC-42), T-18 (TC-01 a TC-33) | T-16, T-17, T-18 | green | `CadastroDeSalaApiTest`, `AlteracaoDeSalaApiTest`, `CadastroDeProfessorApiTest`, `CadastroDeMaterialApiTest`, `NomeUnicoDaSalaApiTest`, `NomeEPatrimonioUnicosApiTest` e as restrições do banco |
| RF-03 pesquisa de recursos | — | TC-01 a TC-20 da spec T-43 | T-43, T-45 | green | `PesquisaDeRecursosApiTest` (TC-01 a TC-13), `ConsultaDeReservasApiTest` (TC-14 a TC-20), spec T-43 |
| RF-04 criar, alterar e cancelar reservas | F3 | RN-01, RN-05, RN-06, RN-08, RN-13 a RN-15 | T-20, T-21, T-23, T-24 | green | `CriacaoDeReservaApiTest`, `AlteracaoDeReservaApiTest`, `CicloDeVidaApiTest`, `AlteracaoDeReservaTest`, `CancelamentoDaReservaTest` |
| RF-05 sobreposição, inclusive na agenda do professor | F1 · CEN-01 · CT-01 | RN-02 | T-20, T-23, T-28 | green | `SobreposicaoNoMesmoRecursoTest` (sala, material e professor), `PedidoComVariosRecursosTest`, `ConflitoNoBancoDeReservaTest` |
| RF-06 proteção contra dupla reserva | F2 · CEN-02 · CT-02 | RN-03 | T-37, T-38 | green | Mesmas evidências da RN-03: [T-37-verde.md](evidencias/T-37-verde.md) e [resultado-01.md](performance/resultado-01.md) (50 usuários: 1 reserva ativa por intervalo) |
| RF-07 aprovação de recurso restrito | F4 · CEN-03 · CT-03 | RN-07 | T-33, T-34, T-36 | green | `AutorizacaoDoPatchApiTest`, `CicloDeVidaApiTest`, `NotificacaoDasMudancasApiTest`; só o Responsável aprova e rejeita. BDD executável: `aprovacao.feature` (5 cenários), ver [bdd-aprovacao-bloqueio.md](testes/bdd-aprovacao-bloqueio.md) |
| RF-08 bloqueio por manutenção | F9 · R-01 | RN-04 | T-33, T-35, T-36 | green | `ManutencaoDeRecursosApiTest`, matriz de autorização (MT-01 a MT-03) |
| RF-09 retirada e devolução de materiais | — | sem casos escritos | T-42 | green | `RetiradaDeMateriaisApiTest` (TC-01 a TC-15), matriz de autorização (RT-01 a RT-03) |
| RF-10 histórico auditável | F6 · R-05 | RN-09 | T-40 | green | `AuditoriaDasMudancasTest`, `AuditoriaApiTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest`; histórico de uma reserva cancelada (TC-18) |
| RF-11 notificação simulada ou API externa | F7 · F8 · CEN-03 · CT-03 · R-07 | INT-01 a INT-05 e TC-01 a TC-16 da spec T-31; CT-03 | T-31 | green | `NotificadorHttpWireMockTest`, `NotificacaoDasMudancasApiTest`, `NotificacaoAposOCommitTest` (WireMock, suíte offline) |
| RF-12 relatório de utilização | — | sem casos escritos (oráculo calculado à mão) | T-41 | green | `RelatorioDeUtilizacaoApiTest` (TC-01 a TC-12, oráculo calculado à mão), matriz de autorização (RL-01) |
| RF-13 interface responsiva e mensagens de erro compreensíveis | — | specs T-22 (TC-01 a TC-47) e T-44 (TC-01 a TC-33) | T-22, T-44, T-45, T-46 | green | Mensagens de erro: `ErrosDaApiTest`, `ErrosDasTelasTest`, `FabricaDeProblemasTest`. Telas: `LoginEMenuDasTelasTest`, `AutorizacaoDasTelasTest`, `TelasDoSolicitanteTest`, `TelasDoResponsavelTest`, `TelasDoAdministradorTest`, `FluxoCompletoPelasTelasTest`, `PaginaDeAcessoNegadoTest` (83 testes); navegador real em 375 px: [T-44-telas.md](evidencias/T-44-telas.md) |
| RF-14 documentação da API | — | DocumentacaoDaApiTest; todos os endpoints da matriz de autorização | T-51 | green | `DocumentacaoDaApiTest` (40 casos: todos os endpoints da matriz descritos), Swagger UI e [README](../README.md) |

## Requisitos não funcionais (RNF-01 a RNF-11)

| Requisito | Meta | Risco / cenário | Task | Status | Evidência |
|---|---|---|---|---|---|
| RNF-01 cobertura de linhas | ≥ 80 % | — | T-27 | green | JaCoCo no `./mvnw verify` em 2026-11-11: **96,0 %** de linhas (1734 de 1807); gate de 80 % no `pom.xml` |
| RNF-02 cobertura de branches | ≥ 70 % | — | T-27, T-28 | green | JaCoCo no `./mvnw verify` em 2026-11-11: **85,2 %** de branches (595 de 698); gate de 70 % no `pom.xml` |
| RNF-03 rastreabilidade | 100 % dos críticos na RTM | — | T-32, T-39, T-50 | green | Este arquivo: RN-01 a RN-09 todas com status `green` e evidência (RN-05 e RN-06 com o job T-60 pendente) |
| RNF-04 defeitos críticos | 0 | — | T-29 | green | SonarCloud da `main` em 2026-11-11: 0 bugs, 0 vulnerabilidades, 0 code smells, Quality Gate aprovado; o DEF-03 (2 pendências críticas) foi corrigido ([registro](defeitos/registro.md)) |
| RNF-05 dupla reserva | 1 aceita, 0 duplicidades | F2 · CEN-02 | T-37, T-38 | green | [T-37-verde.md](evidencias/T-37-verde.md): 20 solicitantes, 1 aceita e 19 recusadas (409); banco com 0 duplicidades; [resultado-01.md](performance/resultado-01.md) |
| RNF-06 autorização por perfil | 0 operações não autorizadas | F5 | T-15, T-48, T-49 | green | `MatrizDeAutorizacaoApiTest` (176 casos), `PerfilDoBancoNaHoraApiTest`; mutação: o PUT de salas aberto ao Solicitante deixa o teste vermelho ([T-49](evidencias/T-49-matriz-de-autorizacao.md)) |
| RNF-07 validação de entrada | 0 gravações inválidas | — | T-16 a T-18, T-20, T-22 | green | `EntradaInvalidaSemVazamentoApiTest`, testes de validação das specs T-16 a T-18, T-20 e T-22; restrições `CHECK` e `UNIQUE` do banco |
| RNF-08 tratamento seguro de erros | 0 respostas com detalhe interno | F14 | T-22, T-49 | green | `TermosProibidos` aplicado em `ErrosDaApiTest`, `ErrosDasTelasTest`, `MatrizDeAutorizacaoApiTest` e `EntradaInvalidaSemVazamentoApiTest` |
| RNF-09 desempenho | p95 ≤ 300 ms com 50 usuários (candidata) | p95 ≤ 300 ms com 50 usuários e erro real < 1 % (validada na T-38) | T-38 | green | [rnf.md](performance/rnf.md) → `perf/reservas.jmx` → [resultado-01.md](performance/resultado-01.md): **atende** (p95 de 2 a 7 ms, 0 erro real, 1 reserva ativa por intervalo) |
| RNF-10 resiliência da notificação | 0 reservas desfeitas | F7 · F8 · R-07 | T-31 | green | `NotificacaoDasMudancasApiTest` (a notificação falha e a aprovação continua), `NotificacaoAposOCommitTest`, `NotificadorHttpWireMockTest` (INT-01 a INT-05) |
| RNF-11 responsividade | utilizável a partir de 375 px | — | T-44 | green | Roteiro no Chrome real em 375 px: 14 páginas sem rolagem horizontal e fluxo completo (31 verificações), `e2e/` e [T-44-telas.md](evidencias/T-44-telas.md), seção 5. Não roda no CI |

## Pendências desta matriz

- As regras RN-10 a RN-12 (propostas) não têm task no backlog e por isso não entram na matriz.
- RN-05, RN-06 e RN-14 estão `green` para as regras e os endpoints; o **job periódico** que dispara as mudanças por tempo (T-60) ficou pendente.
- RF-13 e RNF-11 estão `green`: as telas dos três perfis foram entregues na véspera da entrega, com 83 testes no CI e o roteiro do navegador real (que roda à mão, e não no CI).
- Os IDs `RN-xx-Tn` seguem o plano TDD; as specs em `docs/specs/` têm os casos `TC-nn` de cada task.
