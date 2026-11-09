# Relatório de qualidade

Resume o que o time fez para garantir a qualidade do sistema de Organização de Recursos, com a **fonte de cada número**. Na prova oral o professor pede a origem de qualquer número citado, então nenhum valor entra aqui sem arquivo ou run que o comprove. Task T-52 (depende da T-50).

> **Estado em 2026-11-07:** esqueleto. As seções 1 a 3 e 8 usam o que já está no repositório. Resultados medidos (seções 4 e 5) ainda não existem e ficam `A preencher`.

## 1. Resumo

| Item | Conteúdo |
|---|---|
| O sistema | Aplicação Java 21 e Spring Boot 3.5 que reserva salas, professores e materiais sem conflito de horário |
| O que foi entregue | A preencher |
| O que ficou fora | Ver [decisão de qualidade](decisao-qualidade.md), seção 2 |
| Equipe | Renata (@renatarsilva), Samuel (@Samwel-Merces), Caio (@CaiosAguiar) |

## 2. Problema e riscos

| Item | Onde está |
|---|---|
| Diagnóstico de qualidade (achados pela ISO 25010) | `docs/diagnostico/` |
| Utility tree e cenários | [Utility tree](atam/utility-tree-inicial.md) |
| Riscos da arquitetura (R-01 a R-08) e trade-offs | [Arquitetura](arquitetura.md) |
| Limitadores de nota que o projeto protege | Aplicação executa, CI, autorização e dupla reserva: [Evidências](evidencias/README.md) |

## 3. Arquitetura e decisões

| Item | Onde está |
|---|---|
| Diagramas C4 e sequências críticas | [Arquitetura](arquitetura.md) |
| ADRs 0001 a 0009 | `docs/adr/` |
| Decisões de qualidade | [Decisão de qualidade](decisao-qualidade.md) |

## 4. Estratégia de testes e resultados

| Item | Resultado | Fonte | Task |
|---|---|---|---|
| Plano de teste | `docs/plano-de-teste.md` | arquivo | T-06 |
| Plano TDD (casos por regra) | `docs/testes/plano-tdd.md` | arquivo | T-19 |
| Testes unitários, de integração e de API | A preencher | relatório do CI | T-20, T-23, T-24, T-30 |
| Testes com Testcontainers e WireMock | A preencher | relatório do CI | T-30, T-31 |
| Cobertura de linhas e de branches | A preencher | relatório do JaCoCo | T-27 |
| Análise estática (SonarCloud) | A preencher | painel do SonarCloud | T-29 |
| Teste de concorrência (dupla reserva) | A preencher | teste e run do CI | T-37 |
| Teste de carga (JMeter) | A preencher | `perf/reservas.jmx`; `docs/performance/resultado-01.md` | T-38 |
| TDD ou BDD com evidência | A preencher | `docs/testes/evidencia-tdd.md` | T-34 a T-36 |
| Mutação manual nas regras críticas | A preencher | `docs/testes/mutacao-manual.md` | T-50 |

## 5. Segurança e robustez

| Item | Resultado | Fonte | Task |
|---|---|---|---|
| Matriz de autorização (perfil × operação) | A preencher | `docs/seguranca/matriz-autorizacao.md` | T-48, T-49 |
| Validação de entrada | A preencher | testes das specs T-16 a T-18, T-20, T-22 | T-22 |
| Tratamento seguro de erros (nenhum detalhe interno) | A preencher | testes da spec T-22 | T-22 |
| Senhas em hash e segredos fora do repositório | A preencher | testes da spec T-15 | T-15 |

## 6. Rastreabilidade

| Item | Onde está |
|---|---|
| Matriz requisito → risco → teste → evidência | [RTM](RTM.md) |
| Status dos requisitos | `docs/status-requisitos.md` (T-39) |
| Índice de evidências | [Evidências](evidencias/README.md) |

## 7. Gestão de defeitos

| Item | Onde está |
|---|---|
| Ciclo de vida do defeito, severidade e prioridade | [Defeitos](defeitos/README.md) |
| Defeitos encontrados e fechados | [Registro](defeitos/registro.md) |

## 8. Limitações e riscos aceitos

Ver [decisão de qualidade](decisao-qualidade.md), seções 2 e 3. `A preencher` com o que mudou até a entrega.

## 9. Autoria

Cada integrante explica o próprio código e os próprios testes na prova oral. A tabela abaixo diz quem é dono de quê (conforme o [backlog](planejamento/backlog.md)).

| Integrante | Tasks principais | O que explica na prova oral |
|---|---|---|
| Renata | A preencher | A preencher |
| Samuel | A preencher | A preencher |
| Caio | A preencher | A preencher |
