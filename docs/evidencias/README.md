# Índice de evidências

Aponta, para cada limitador de nota e para cada requisito crítico, **o arquivo ou comando que prova** que ele foi atendido. Enquanto a prova não existir, a linha diz "sem evidência ainda" e a task que a produz. Ligado à [RTM](../RTM.md).

> **Estado em 2026-11-11 (entrega):** todas as linhas abaixo têm evidência, exceto o que está dito como não entregue. Quem produz uma evidência preenche a linha na mesma PR.

## 1. Limitadores de nota

| Limitador | Como provar | Arquivo ou comando | Task | Evidência |
|---|---|---|---|---|
| A aplicação executa | Clone limpo, seguir o README e abrir a aplicação | `README.md`; comando de subida descrito nele; run da tag `v1.0.0-rc3` | T-55, T-56 | `./mvnw spring-boot:run` sobe a aplicação com o PostgreSQL em contêiner e responde no Swagger UI (verificado na carga de 2026-11-11, ver [resultado-01.md](../performance/resultado-01.md)); clone limpo conferido na T-55 |
| CI presente | Pipeline roda em cada pull request e na tag | `.github/workflows/ci.yml`; run verde da tag; run vermelho intencional registrado | T-25, T-56 | `.github/workflows/ci.yml` (workflow `qualidade`, job `build`); runs verdes na `main` e nos PRs; run vermelho intencional registrado (PR #44, T-25) |
| Sem falha de autorização | Matriz perfil × operação com teste parametrizado | `docs/seguranca/matriz-autorizacao.md`; teste parametrizado; `docs/testes/mutacao-manual.md` | T-48, T-49, T-50 | [matriz](../seguranca/matriz-autorizacao.md); `MatrizDeAutorizacaoApiTest` (176 casos); [T-49](T-49-matriz-de-autorizacao.md); mutação M5 em [mutacao-manual.md](../testes/mutacao-manual.md); telas: `AutorizacaoDasTelasTest` (29 rotas × perfis), mutação M6, [T-44-telas.md](T-44-telas.md) |
| Sem dupla reserva | Teste de 20 solicitações simultâneas e restrição do banco | Teste de concorrência; migração com `EXCLUDE`; ADR da proteção; `perf/reservas.jmx` | T-30, T-37, T-38 | [T-37-vermelho.md](T-37-vermelho.md), [T-37-verde.md](T-37-verde.md), [ADR-0010](../adr/0010-protecao-contra-dupla-reserva-no-banco.md); carga com 50 usuários em [resultado-01.md](../performance/resultado-01.md) |

## 2. Regras críticas (RN-01 a RN-09)

| Regra | O que prova | Onde procurar | Task | Evidência |
|---|---|---|---|---|
| RN-01 término após o início | Testes unitários e do cadastro | Pacote de testes de reserva | T-20, T-23 | `IntervaloDaReservaTest`, `CriacaoDeReservaApiTest` |
| RN-02 sem sobreposição | Testes de regra e cobertura de ramo; restrição no banco | Testes de reserva; relatório do JaCoCo; teste com Testcontainers | T-20, T-23, T-28, T-30 | `SobreposicaoNoMesmoRecursoTest`, `ConflitoNoBancoDeReservaTest` |
| RN-03 solicitações simultâneas | Teste de concorrência | Teste com 20 solicitações; ADR da proteção | T-37 | [T-37-verde.md](T-37-verde.md); [ADR-0010](../adr/0010-protecao-contra-dupla-reserva-no-banco.md) |
| RN-04 manutenção bloqueia | Testes TDD estritos | `docs/testes/evidencia-tdd.md`; commits `test(red)` e `feat(green)` | T-35, T-36 | `ManutencaoDeRecursosApiTest`, `ManutencaoEReservaSimultaneasApiTest`; mutação M3 |
| RN-05 e RN-06 estados | Tabela de transições (teste parametrizado) | Testes do ciclo de vida | T-21, T-24, T-59, T-60 | `MatrizDeTransicoesTest` (49 combinações), `CaminhosAlternativosTest`; o job periódico (T-60) ficou pendente |
| RN-07 só o Responsável aprova | Testes de aprovação e matriz de autorização | `docs/testes/bdd-aprovacao-bloqueio.md`; `aprovacao.feature` | T-33, T-34, T-36, T-49 | `AutorizacaoDoPatchApiTest`, matriz de autorização, `aprovacao.feature` (5 cenários) |
| RN-08 reserva iniciada não se apaga | Testes de exclusão e chave estrangeira | Testes do ciclo de vida; teste de integração com o banco | T-21, T-30, T-61 | `ExclusaoDeReservaTest`, `ImutabilidadeDaAuditoriaTest` (chave estrangeira) |
| RN-09 auditoria | Teste de auditoria com rollback | Testes de auditoria | T-40 | `AuditoriaDasMudancasTest`, `RollbackDaAuditoriaApiTest`, `ImutabilidadeDaAuditoriaTest` |

## 3. Metas mensuráveis

| Meta | Como provar | Onde | Task | Evidência |
|---|---|---|---|---|
| Cobertura de linhas ≥ 80 % e de branches ≥ 70 % | Gate do JaCoCo no `verify` (o build falha abaixo da meta) | Relatório do JaCoCo; run do CI | T-27 | JaCoCo no `./mvnw verify` de 2026-11-12: **96,0 %** de linhas (1734/1807) e **85,2 %** de branches (595/698); o build falha abaixo de 80 % e 70 %. O SonarCloud mede 93,0 % |
| 0 bugs ou vulnerabilidades críticas | Quality Gate do SonarCloud no pull request | Painel do SonarCloud | T-29 | SonarCloud da `main` em 2026-11-11: **0 bugs, 0 vulnerabilidades, 0 code smells, 0 hotspots**, duplicação 0,0 % e Quality Gate aprovado; o DEF-03 (2 pendências críticas) foi corrigido ([registro](../defeitos/registro.md)) |
| Requisitos críticos na RTM a 100 % | Cada linha da RTM aponta uma evidência | [RTM](../RTM.md) | T-32, T-50 | [RTM](../RTM.md): RN-01 a RN-09 com status `green` e evidência |
| Desempenho (candidata): p95 ≤ 300 ms com 50 usuários | Plano e execução do JMeter | `perf/reservas.jmx`; `docs/performance/rnf.md`; `docs/performance/resultado-01.md` | T-38 | [rnf.md](../performance/rnf.md), `perf/reservas.jmx`, [resultado-01.md](../performance/resultado-01.md): **atende** (p95 de 2 a 7 ms, 0 erro real) |

## 4. Estratégia de qualidade obrigatória

| Item | Como provar | Task | Evidência |
|---|---|---|---|
| Pelo menos uma funcionalidade com TDD ou BDD | Commit vermelho no histórico, feature Cucumber, `docs/testes/evidencia-tdd.md` | T-34 a T-36 | [evidencia-tdd.md](../testes/evidencia-tdd.md): 21 pares `test(red)` → `feat(green)` no histórico; BDD executável com Cucumber: 17 cenários em `src/test/resources/features/`, com mutação provada ([bdd-aprovacao-bloqueio.md](../testes/bdd-aprovacao-bloqueio.md)) |
| Testcontainers para o banco | Testes de integração com PostgreSQL real | T-30 | Toda a suíte de API usa PostgreSQL real (`@TesteDeApiComBanco`, `TestcontainersConfiguration`) |
| WireMock para integração externa | Casos INT-01 a INT-05 | T-31 | `NotificadorHttpWireMockTest` (INT-01 a INT-05, token que não vaza, timeouts) |
| Validação de entrada e tratamento seguro de erros | Testes de erro sem termo proibido | T-22 | `ErrosDaApiTest`, `ErrosDasTelasTest`, `EntradaInvalidaSemVazamentoApiTest` com a lista `TermosProibidos` |
| Defeitos registrados | [Registro de defeitos](../defeitos/registro.md) | T-14 | [Registro de defeitos](../defeitos/registro.md): DEF-01 a DEF-03 |

## 5. Como acrescentar uma evidência

1. Produza o arquivo, o run ou o comando que prova.
2. Troque "sem evidência ainda" pelo caminho do arquivo ou pelo link do run.
3. Atualize o `Status` da linha correspondente na [RTM](../RTM.md).
