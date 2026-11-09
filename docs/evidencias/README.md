# Índice de evidências

Aponta, para cada limitador de nota e para cada requisito crítico, **o arquivo ou comando que prova** que ele foi atendido. Enquanto a prova não existir, a linha diz "sem evidência ainda" e a task que a produz. Ligado à [RTM](../RTM.md).

> **Estado em 2026-11-07:** nenhuma evidência existe ainda. Quem produz uma evidência preenche a linha na mesma PR (task T-56 fecha o pacote).

## 1. Limitadores de nota

| Limitador | Como provar | Arquivo ou comando | Task | Evidência |
|---|---|---|---|---|
| A aplicação executa | Clone limpo, seguir o README e abrir a aplicação | `README.md`; comando de subida descrito nele; run da tag `v1.0.0-rc1` | T-55, T-56 | sem evidência ainda |
| CI presente | Pipeline roda em cada pull request e na tag | `.github/workflows/ci.yml`; run verde da tag; run vermelho intencional registrado | T-25, T-56 | sem evidência ainda |
| Sem falha de autorização | Matriz perfil × operação com teste parametrizado | `docs/seguranca/matriz-autorizacao.md`; teste parametrizado; `docs/testes/mutacao-manual.md` | T-48, T-49, T-50 | sem evidência ainda |
| Sem dupla reserva | Teste de 20 solicitações simultâneas e restrição do banco | Teste de concorrência; migração com `EXCLUDE`; ADR da proteção; `perf/reservas.jmx` | T-30, T-37, T-38 | sem evidência ainda |

## 2. Regras críticas (RN-01 a RN-09)

| Regra | O que prova | Onde procurar | Task | Evidência |
|---|---|---|---|---|
| RN-01 término após o início | Testes unitários e do cadastro | Pacote de testes de reserva | T-20, T-23 | sem evidência ainda |
| RN-02 sem sobreposição | Testes de regra e cobertura de ramo; restrição no banco | Testes de reserva; relatório do JaCoCo; teste com Testcontainers | T-20, T-23, T-28, T-30 | sem evidência ainda |
| RN-03 solicitações simultâneas | Teste de concorrência | Teste com 20 solicitações; ADR da proteção | T-37 | sem evidência ainda |
| RN-04 manutenção bloqueia | Testes TDD estritos | `docs/testes/evidencia-tdd.md`; commits `test(red)` e `feat(green)` | T-35, T-36 | sem evidência ainda |
| RN-05 e RN-06 estados | Tabela de transições (teste parametrizado) | Testes do ciclo de vida | T-21, T-24, T-59, T-60 | sem evidência ainda |
| RN-07 só o Responsável aprova | Testes de aprovação e matriz de autorização | `docs/testes/bdd-aprovacao-bloqueio.md`; feature Cucumber | T-34, T-36, T-49 | sem evidência ainda |
| RN-08 reserva iniciada não se apaga | Testes de exclusão e chave estrangeira | Testes do ciclo de vida; teste de integração com o banco | T-21, T-30, T-61 | sem evidência ainda |
| RN-09 auditoria | Teste de auditoria com rollback | Testes de auditoria | T-40 | sem evidência ainda |

## 3. Metas mensuráveis

| Meta | Como provar | Onde | Task | Evidência |
|---|---|---|---|---|
| Cobertura de linhas ≥ 80 % e de branches ≥ 70 % | Gate do JaCoCo no `verify` (o build falha abaixo da meta) | Relatório do JaCoCo; run do CI | T-27 | sem evidência ainda |
| 0 bugs ou vulnerabilidades críticas | Quality Gate do SonarCloud no pull request | Painel do SonarCloud | T-29 | sem evidência ainda |
| Requisitos críticos na RTM a 100 % | Cada linha da RTM aponta uma evidência | [RTM](../RTM.md) | T-32, T-50 | sem evidência ainda |
| Desempenho (candidata): p95 ≤ 300 ms com 50 usuários | Plano e execução do JMeter | `perf/reservas.jmx`; `docs/performance/rnf.md`; `docs/performance/resultado-01.md` | T-38 | sem evidência ainda |

## 4. Estratégia de qualidade obrigatória

| Item | Como provar | Task | Evidência |
|---|---|---|---|
| Pelo menos uma funcionalidade com TDD ou BDD | Commit vermelho no histórico, feature Cucumber, `docs/testes/evidencia-tdd.md` | T-34 a T-36 | sem evidência ainda |
| Testcontainers para o banco | Testes de integração com PostgreSQL real | T-30 | sem evidência ainda |
| WireMock para integração externa | Casos INT-01 a INT-05 | T-31 | sem evidência ainda |
| Validação de entrada e tratamento seguro de erros | Testes de erro sem termo proibido | T-22 | sem evidência ainda |
| Defeitos registrados | [Registro de defeitos](../defeitos/registro.md) | T-14 | existe (atualizar a cada defeito) |

## 5. Como acrescentar uma evidência

1. Produza o arquivo, o run ou o comando que prova.
2. Troque "sem evidência ainda" pelo caminho do arquivo ou pelo link do run.
3. Atualize o `Status` da linha correspondente na [RTM](../RTM.md).
