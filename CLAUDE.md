# CLAUDE.md

Instruções para o Claude Code neste repositório. Valem para **todas as pessoas do time**.

## Projeto

- Projeto semestral de Qualidade de Software (TADS Senac 2026.2, Prof. Afonso Lelis): sistema de **Organização de Recursos** (reserva de salas, professores e materiais sem conflito).
- Time de 3 pessoas: Renata (@renatarsilva), Samuel (@Samwel-Merces) e Caio (@CaiosAguiar).
- Stack: Java 21, Spring Boot 3.5 (a especificação exige 3.x: não atualize para o 4), PostgreSQL com Flyway, telas Thymeleaf + Bootstrap, Maven (`./mvnw`), JUnit 5, Testcontainers, WireMock, Cucumber, JaCoCo, SonarCloud, GitHub Actions, JMeter.
- Sem Jira ([ADR-0003](docs/adr/0003-retirar-o-jira-do-fluxo.md)): branches, commits e títulos de PR levam o ID da task (`T-NN`, do backlog) ou, se não houver task, o número da issue (`#N`). Pendências e defeitos ficam em issues do GitHub. Nunca sugira chaves `KAN-NN`.
- Fale sempre em **português**.

## Leia antes de agir

| Documento | Para quê |
|---|---|
| `docs/processo/ai-workflow.md` | **O fluxo obrigatório de toda task.** Siga-o à risca. |
| `CONTRIBUTING.md` | Nomes de branch, commits, PR e rodízio de revisão |
| `docs/projeto/contexto-da-disciplina.md` | Requisitos, regras críticas, critérios de avaliação e limitadores de nota |
| `docs/planejamento/backlog.md` | Tasks semana a semana (32 a 47), donos, sprints e dependências. Cada task diz o que o professor confere |
| `docs/adr/` | Decisões de arquitetura já tomadas. Não contradiga um ADR sem propor um novo |

## Regras de git (obrigatórias)

- **Nunca** execute `git commit`, `git commit --amend`, `git push`, `git tag`, `git merge` ou `git rebase`, nem abra pull requests (`gh pr create`), mesmo que pareça o próximo passo natural.
- Quando algo estiver pronto para commit, avise e entregue a lista de arquivos alterados e uma mensagem no padrão do `CONTRIBUTING.md` (Conventional Commits em português, com o ID da task no final).
- A pessoa faz o commit, o push e abre o PR manualmente.
- Permitidos: `git status`, `git diff`, `git log`, `git fetch`, `git add`, `git checkout`/`git switch` para criar ou trocar de branch.

## Regras do fluxo

- **Não escreva código de produção** para uma task cuja spec não esteja com status `Liberada` (tasks com fluxo `—` no backlog não têm spec). Se pedirem, explique em qual etapa do ai-workflow a task está.
- Não pule etapas, mesmo que peçam para "ir mais rápido". Explique o motivo.
- Não invente regra de negócio. Regras vêm do PRD e da spec; se faltar, pergunte e sugira registrar no PRD.
- Em TDD, o teste vem antes do código e precisa falhar pelo motivo certo antes do green.
- Use `Clock` injetado em regras que dependem do horário atual. Mocks só em testes unitários isolados; o banco é testado com Testcontainers e integrações externas com WireMock.

## Autoria (prova oral)

Na prova oral, cada pessoa explica o próprio código e os próprios testes. Por isso:

- **O dono da task escreve as regras de negócio, os critérios de aceite e os testes.** Você explica, faz perguntas, revisa e aponta melhorias.
- Você pode gerar partes repetitivas (configuração, YAML de CI, Docker, DTOs, mapeamentos), explicando cada trecho.
- Na revisão por pares, o parecer é escrito pelo revisor. Você levanta pontos, mas não escreve o parecer por ele.
- Ao final de uma explicação importante, faça uma ou duas perguntas para checar o entendimento.
