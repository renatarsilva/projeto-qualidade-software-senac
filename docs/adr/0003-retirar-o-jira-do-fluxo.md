# ADR-0003: Retirar o Jira do fluxo e identificar o trabalho pelo ID da task

- **Status:** Aceita
- **Data:** 2026-10-26
- **Autor(a):** @Samwel-Merces
- **Task:** issue #5 (sem task no backlog)

## Contexto

- O Jira não é obrigatório: a [especificação do projeto](https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html) não exige nenhuma ferramenta de gestão de tarefas. Ela exige práticas e evidências: "repositório com histórico de trabalho e revisão por pares", "registro de defeitos encontrados, priorizados e corrigidos" e rastreabilidade. As ferramentas obrigatórias são técnicas (Java 21, Spring Boot 3.x, JUnit 5, Testcontainers, WireMock, JaCoCo, GitHub Actions, SonarCloud e JMeter).
- O fluxo do time ([ADR-0002](0002-adotar-spec-driven-development.md) e [AI Workflow](../processo/ai-workflow.md)) previa o Jira (projeto `KAN`) para acompanhar as tasks, e a chave `KAN-NN` em branches, commits e títulos de PR.
- Na prática, o Jira deixou de ser usado: o PR #1 foi aberto com a chave em branco (`KAN-`), e a T-05 (PR #3) já registra que a branch e os commits não seguem o padrão `KAN-NN`.
- Até esta data, nenhum registro dizia quem decidiu a retirada nem quando. As regras escritas (`CONTRIBUTING.md`, `CLAUDE.md`, template de PR, template de spec e `scripts/validar-spec.sh`) continuam exigindo a chave.
- Nas primeiras etapas, a Renata, dona do projeto Jira e do repositório, está ausente. Samuel e Caio seguem o trabalho.

## Decisão

O Jira sai do fluxo do projeto, por decisão de Samuel e Caio em 26/10/2026. A partir daí:

- o trabalho é identificado pelo **ID estável da task** (`T-NN`) em branches, commits e títulos de PR;
- o [`backlog.md`](../planejamento/backlog.md) continua como a fonte das tasks;
- **issues do GitHub** registram pendências e defeitos (rótulo `bug`);
- os **PRs** guardam os registros que antes iam para o Jira, como a substituição de revisor.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter o Jira | Ninguém o atualiza. Manter a regra escrita sem praticá-la deixa o processo documentado diferente do real, e isso aparece na avaliação de autoria e rastreabilidade. |
| Trocar por um quadro no GitHub Projects | É mais uma ferramenta para manter. O `backlog.md`, as issues e os PRs já cobrem o acompanhamento de um time de duas a três pessoas. |

## Consequências

- **Positivas:** as regras escritas voltam a descrever o processo real; a rastreabilidade task → branch → commit → PR fica toda no repositório; não depende de acesso a uma ferramenta externa.
- **Negativas e riscos:** perde-se o quadro visual do Jira e a ligação automática entre branch e task. O trabalho que não é task do backlog (como esta mudança) não tem `T-NN` e é identificado pelo número da issue.
- **Pendente:** a Renata ainda não foi informada. Quando voltar, ela pode confirmar a decisão ou propor um ADR que a substitua. Ficam com ela a task T-02 (configuração do Jira e proteção da `main`) e o `backlog-jira.csv`.
- **Como vamos verificar:** nenhuma regra escrita exige `KAN-NN` (`git grep -n "KAN-"` só encontra registros históricos), e os PRs novos usam `T-NN` no título.
