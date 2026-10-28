# Documentação

Índice de toda a documentação do projeto. A documentação é **viva**: cada PR que muda o comportamento ou uma decisão atualiza o documento correspondente.

## Comece por aqui

1. [Contexto da disciplina](projeto/contexto-da-disciplina.md): o que o professor pede e como avalia.
2. [AI Workflow](processo/ai-workflow.md): o caminho de toda task, do backlog ao merge.
3. [CONTRIBUTING](../CONTRIBUTING.md): branches, commits, PRs e o rodízio de revisão.
4. [Backlog](planejamento/backlog.md) e [sprints](planejamento/sprints.md): quem faz o quê e quando.

## Estrutura

Os caminhos seguem os que o professor usa nas aulas. ✅ = existe; T-NN = task que cria ou completa.

| Caminho | Conteúdo | Situação |
|---|---|---|
| `projeto/contexto-da-disciplina.md` | Especificação resumida, cronograma das aulas, avaliação e entrega | ✅ |
| `processo/ai-workflow.md` | Fluxo SDD: spec → revisão → validação → grill → TDD → PR | ✅ |
| `processo/configuracao-github-e-jira.md` | Proteção da `main` (Parte A). A parte do Jira está obsoleta ([ADR-0003](adr/0003-retirar-o-jira-do-fluxo.md)) | ✅ (Parte A pendente, T-02) |
| `planejamento/backlog.md` | 58 tasks, semana 32 a 47: a fonte das tasks | ✅ |
| `planejamento/backlog-jira.csv` | Importação para o Jira, que não é mais usado | obsoleto · mantido até o retorno da Renata |
| `planejamento/sprints.md` | Mini-plano e resultado de cada sprint | ✅ |
| `specs/` | Uma spec por task ([template](specs/_TEMPLATE.md)) | ✅ template |
| `adr/` | Decisões de arquitetura e processo ([template](adr/_TEMPLATE.md)) | ✅ 0001, 0002, 0003 · T-13, T-37 |
| `prd.md` | Visão, personas, RF, RNF e RN numerados, escopo | esqueleto · T-04, T-09 |
| `plano-de-teste.md` | Plano de teste (ISO/IEC/IEEE 29119-3) | esqueleto · T-06 |
| `arquitetura.md` | Descoberta, C4, modelo de dados, sequências, ATAM e matriz | esqueleto · T-10 a T-13 |
| `diagnostico/diagnostico-inicial.md` | Diagnóstico de qualidade (Atividade #1) | T-03 |
| `atam/utility-tree-inicial.md` | Utility tree e casos de teste (Atividade #2) | T-05 |
| `defeitos/` | Processo e registro de defeitos | T-14 |
| `testes/plano-tdd.md` | Inventário das regras e matriz regra → caso → teste | T-19 |
| `avaliacao-arquitetural/ciclo-01.md` | Arquitetura planejada × código | T-26, T-29 |
| `rtm.md` | Requisito → risco → teste → evidência | T-32 (atualizada a cada PR), fechada na T-50 |
| `testes/bdd-aprovacao-bloqueio.md` e `testes/evidencia-tdd.md` | BDD e evidência de TDD | T-33, T-36 |
| `performance/rnf.md` e `performance/resultado-01.md` | RNF de desempenho e resultado de carga | T-38 |
| `status-requisitos.md` | Situação dos 14 RFs e das regras críticas | T-39 |
| `seguranca/matriz-autorizacao.md` | Operação × perfil × status esperado | T-48 |
| `testes/mutacao-manual.md` | Mutações manuais nas regras críticas | T-50 |
| `relatorio-qualidade.md` | Relatório final de qualidade | T-52 |
| `prova-oral/` | Perguntas, ensaio e apresentação | T-53, T-54 |
| `entrega/checklist.md`, `evidencias/README.md`, `decisao-qualidade.md` | Pacote de entrega | T-55 a T-57 |

Fora de `docs/`: `.github/workflows/ci.yml` (T-25), `perf/` com o plano JMeter (T-38), `src/test/resources/features/` com o Cucumber (T-36) e `CHANGELOG.md` (T-56).

## Ferramentas do processo

| Ferramenta | Onde |
|---|---|
| Template de spec | `docs/specs/_TEMPLATE.md` |
| Spec validation | `./scripts/validar-spec.sh docs/specs/<arquivo>.md` |
| Gerar o backlog (.md e .csv) | `python3 scripts/gerar-backlog.py` |
| Template de PR | `.github/pull_request_template.md` |
| Comandos do Claude | `.claude/skills/`: `/iniciar-task`, `/revisar-spec`, `/grill-with-docs`, `/preparar-pr` |
