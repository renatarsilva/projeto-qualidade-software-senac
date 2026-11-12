# Plano de sprints

O cronograma da disciplina vai da semana 32 à 47, mas o time começou em 15/10 (semana 42 do calendário). Por isso as **15 semanas de conteúdo foram comprimidas em 4 sprints de uma semana**. Cada sprint cobre as semanas do curso indicadas, na ordem das aulas, e todas as tasks estão no [backlog](backlog.md).

| Sprint | Período real | Semanas do curso | Foco |
|---|---|---|---|
| 1 | 15/10 a 21/10 | 32, 33, 34, 35 | Ambiente, diagnóstico, PRD, plano de teste, arquitetura e ATAM |
| 2 | 22/10 a 28/10 | 36, 37, 38, 39 | Autenticação, cadastros, TDD do núcleo de reservas, CI, JaCoCo, SonarCloud, Testcontainers e WireMock |
| 3 | 29/10 a 04/11 | 41, 42, 43 | BDD de aprovação e manutenção, concorrência, carga, auditoria, relatório e requisitos restantes |
| 4 | 05/11 a 11/11 | 44, 45, 46 | Interface, matriz de autorização, mutação, RTM, relatório de qualidade e entrega |
| — | a partir de 16/11 | 47 | Prova oral |

A semana 40 é a prova escrita (individual, sem entrega no projeto). As semanas 48 a 50 acontecem depois da entrega e ficam fora deste plano.

```mermaid
gantt
    dateFormat YYYY-MM-DD
    axisFormat %d/%m
    section Sprints
    Sprint 1 · semanas 32-35   :s1, 2026-10-15, 7d
    Sprint 2 · semanas 36-39   :s2, 2026-10-22, 7d
    Sprint 3 · semanas 41-43   :s3, 2026-10-29, 7d
    Sprint 4 · semanas 44-46   :s4, 2026-11-05, 7d
    section Marcos
    Entrega (semana 46)        :milestone, 2026-11-09, 0d
    Prova oral (semana 47)     :milestone, 2026-11-16, 0d
```

> O site do professor não traz datas exatas, só o número das semanas. As datas de entrega e da prova oral acima são estimativas pela semana do calendário: confirmem em sala ou no Discord.

## Planejado × realizado

O plano abaixo distribui as tasks pelos três integrantes. Na execução, para cumprir o prazo da entrega, a **Renata assumiu a implementação das tasks de código** (regras, testes, banco, CI, carga) e os documentos finais, enquanto o **Samuel e o Caio concentraram-se na documentação de produto, nas specs, nas revisões por pares e no processo**. A distribuição planejada foi mantida em cada sprint por transparência, e a tabela **Realizado** de cada sprint registra o que o histórico do Git mostra.

Os números abaixo saem do repositório e qualquer pessoa pode conferi-los com o comando indicado:

| Medida | Renata | Samuel | Caio | Como conferir |
|---|---|---|---|---|
| Commits (sem merges) | **206** (81 %) | 27 | 20 | `git shortlog -sn --no-merges` |
| Pull requests abertos | **39** | 4 | 10 | `gh pr list --state all --limit 100 --json author` |
| Linhas de código-fonte alteradas em `src/` | **16.209** (todas) | 0 | 0 | `git log --numstat -- src` |
| Specs de tasks criadas (de 15) | **14** | 1 (T-31) | 0 | histórico de `docs/specs/` |
| Pipeline de CI, gate do JaCoCo e análise do SonarCloud | **configurados** | run vermelho intencional | | `git log -- .github/workflows/ci.yml` |
| Commits de revisão e aprovação de specs | | **15** | **10** | `git log --grep "revisa\|aprova"` |

A contribuição do Samuel e do Caio está na **qualidade do processo**: cada spec foi revisada e aprovada por outra pessoa antes de virar código (parecer na seção 9 de cada spec), e o Caio e o Samuel escreveram o PRD, as personas, o diagnóstico, o plano de teste e a spec de notificação externa.

## Rituais

- **Planejamento** (primeiro dia da sprint, 15 min): confirmar o objetivo, quem pega o quê e as dependências.
- **Check-in assíncrono** (diário, no grupo do time): o que fiz, o que vou fazer, o que me bloqueia.
- **Revisão** (último dia, 20 min): demonstrar o que foi mergeado e preencher o "Resultado" da sprint abaixo.

---

## Sprint 1 · 15/10 a 21/10 · semanas 32 a 35

**Objetivo:** ambiente de todos funcionando, projeto no repositório e a documentação de produto, teste e arquitetura que as specs vão citar.

| Pessoa | Tasks |
|---|---|
| Renata | T-02 Jira e proteção da main · T-03 diagnóstico · T-10 arquitetura e C4 · T-11 modelo de dados · T-12 ATAM · T-13 ADRs e matriz |
| Samuel | T-06 plano de teste |
| Caio | T-04 personas · T-05 utility tree (Atividade #2) · T-07 projeto Spring Boot · T-08 AIOX · T-09 PRD |
| Todos | T-01 ambiente de cada integrante |

**Ordem que importa:** T-07 (projeto) e T-02 (Jira e proteção) primeiro. Depois T-01 (cada um roda o projeto). T-04 (personas) antes de T-09 (PRD), e o PRD antes da arquitetura (T-10 a T-13).

**Pronto quando:** os 3 rodam `./mvnw verify` verde; `main` protegida; PRD com RF, RNF e RN numerados; arquitetura com C4, sequências, ATAM e ADRs mergeados.

**Realizado** (conforme o histórico do Git):

| Pessoa | Tasks entregues |
|---|---|
| Renata | T-02 proteção da `main` · T-10 arquitetura e C4 · T-11 modelo de dados · T-12 ATAM · T-13 ADRs e matriz |
| Samuel | T-06 plano de teste |
| Caio | T-03 diagnóstico · T-04 personas · T-05 utility tree · T-07 projeto Spring Boot · T-08 AIOX · T-09 PRD |

**Resultado:** Concluída. O projeto, o PRD, a arquitetura (C4, sequências, ATAM), os ADRs e o plano de teste foram mergeados.

---

## Sprint 2 · 22/10 a 28/10 · semanas 36 a 39

**Objetivo:** primeiras tasks pelo fluxo completo de specs. Autenticação, cadastros e núcleo de reservas por TDD, com CI, cobertura e análise estática ativos.

| Pessoa | Tasks |
|---|---|
| Renata | Semana 36 inteira: T-14 defeitos · T-15 autenticação (crítica) · T-16 salas · T-17 professores · T-18 materiais · T-19 plano TDD · T-20 red intervalo e sobreposição · T-21 red ciclo de vida · T-22 tratamento de erros. Depois: T-23 green · T-28 cobertura de ramo · T-32 RTM |
| Samuel | T-25 CI · T-29 SonarCloud · T-31 notificação WireMock |
| Caio | T-24 green ciclo de vida · T-26 ciclo-01 · T-27 JaCoCo · T-30 Testcontainers |

**Ordem que importa:** T-25 (CI) e T-15 (autenticação) no início, porque quase tudo depende deles. T-19 (plano TDD) antes das fases red. Escrevam as specs no primeiro dia: a revisão leva tempo.

**Pronto quando:** CI com JaCoCo 80/70 e SonarCloud no PR; reserva com intervalo, sobreposição e estados implementada com commits `test(red)` e `feat(green)` no histórico; RTM iniciada.

**Riscos:** quem fizer a T-25 precisa do escopo `workflow` no token do GitHub (`gh auth refresh -h github.com -s workflow`). O secret `SONAR_TOKEN` só pode ser criado pela Renata (admin).

**Realizado** (conforme o histórico do Git):

| Pessoa | Tasks entregues |
|---|---|
| Renata | Semana 36 inteira (T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21, T-22), **mais** T-23 e T-24 (código das reservas e do ciclo de vida), T-25 pipeline de CI, T-27 JaCoCo, T-29 SonarCloud, T-30 Testcontainers (base de toda a suíte de API) e o código e os testes da T-31 (adaptador, WireMock e ligação com as reservas) |
| Samuel | Revisão e aprovação das specs T-16, T-18, T-21 e T-22 · run vermelho e verde do CI (T-25) · spec da T-31 |
| Caio | Revisão e aprovação das specs T-15, T-17, T-20 e T-31 |

**Não realizada:** T-26 (avaliação arquitetural em ciclo).

**Resultado:** Concluída, exceto a T-26. Autenticação, cadastros, núcleo de reservas por TDD (commits `test(red)` e `feat(green)`), CI com JaCoCo 80/70 e SonarCloud, Testcontainers e WireMock.

---

## Sprint 3 · 29/10 a 04/11 · semanas 41 a 43

**Objetivo:** fechar as regras que são limitadores de nota e os requisitos que nenhuma aula cobre.

| Pessoa | Tasks |
|---|---|
| Renata | T-36 Cucumber e evidência TDD · T-38 JMeter · T-39 status dos requisitos · T-40 auditoria (crítica) |
| Samuel | T-35 manutenção (crítica) · T-37 dupla reserva (crítica) · T-42 retirada e devolução · T-44 layout e login |
| Caio | T-33 BDD · T-34 aprovação (crítica) · T-41 relatório de utilização · T-43 pesquisa |

**Ordem que importa:** T-33 (BDD) antes de T-34 e T-35. T-37 (dupla reserva) antes de T-38 (carga) e T-41 (relatório).

**Pronto quando:** aprovação, manutenção, dupla reserva e auditoria com spec liberada, testes e PR mergeado; teste de concorrência estável em 5 execuções; resultado de carga documentado.

**Realizado** (conforme o histórico do Git):

| Pessoa | Tasks entregues |
|---|---|
| Renata | T-34 aprovação (dentro do ciclo de vida) · T-35 manutenção · T-37 dupla reserva e correção do DEF-02 · T-38 JMeter · T-39 status dos requisitos · T-40 auditoria · T-41 relatório de utilização · T-42 retirada e devolução · T-43 pesquisa e consulta de reservas · T-36 evidência de TDD |
| Samuel | Parecer da spec da T-37 |
| Caio | Revisões de PRs e de specs |

**Realizadas depois, na véspera da entrega:** T-33 e T-36 (BDD com 17 cenários rodando no `verify`) e T-44 (layout e login, nas telas).
**Não realizadas:** nenhuma desta sprint.

**Resultado:** Concluída para as regras críticas: aprovação, manutenção, dupla reserva (teste de concorrência estável) e auditoria, com spec liberada, testes e PR mergeado; carga documentada. O BDD e o layout entraram na véspera da entrega.

---

## Sprint 4 · 05/11 a 11/11 · semanas 44 a 46

**Objetivo:** interface completa, autorização provada, RTM fechada e entrega empacotada.

| Pessoa | Tasks |
|---|---|
| Renata | T-45 telas do Solicitante · T-50 mutação e RTM · T-52 relatório de qualidade · T-57 decisão de qualidade e formulário |
| Samuel | T-47 testes E2E · T-48 matriz de autorização · T-49 teste da matriz (crítica) · T-56 tag `v1.0.0-rc1` e evidências |
| Caio | T-46 telas do Responsável e do Administrador · T-51 OpenAPI · T-53 perguntas da prova oral · T-55 clone limpo e checklist |
| Todos | T-54 ensaio e apresentação · T-58 prova oral (semana 47) |

**Pronto quando:** metas 80/70 atingidas, RTM com 100% dos requisitos críticos, tag `v1.0.0-rc1` com CI verde e formulário enviado.

**Realizado** (conforme o histórico do Git):

| Pessoa | Tasks entregues |
|---|---|
| Renata | T-44, T-45, T-46 telas dos três perfis · T-48 matriz de autorização · T-49 teste da matriz · T-50 mutação manual e RTM · T-51 OpenAPI · T-52 relatório de qualidade · T-53 perguntas da prova oral · T-55 checklist e demonstração · T-56 evidências e `CHANGELOG` · T-57 decisão de qualidade · correção do DEF-03 (SonarCloud) |
| Samuel | Revisão e aprovação de PRs |
| Caio | Revisão e aprovação de PRs |

**Também realizadas (na véspera da entrega):** T-44, T-45 e T-46, as telas dos três perfis (spec T-44, 83 testes, roteiro no Chrome real).
**Não realizadas:** T-47 no CI (o roteiro do navegador roda à mão) e T-54 (ensaio da prova oral, que depende da equipe).
**Feito para fechar a entrega:** tag `v1.0.0-rc3` com o CI verde (as `rc1` e `rc2` ficaram em commits anteriores) e clone limpo. **Falta:** o formulário.

**Resultado:** Concluída quanto à API, às regras, às provas (autorização, concorrência, carga, mutação) e aos documentos. 1043 testes, cobertura de 96,0 % de linhas e 85,2 % de branches, SonarCloud com Quality Gate aprovado. As telas responsivas dos três perfis foram entregues na véspera (RF-13 e RNF-11).
