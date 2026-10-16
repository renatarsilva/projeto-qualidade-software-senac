---
name: iniciar-task
description: Inicia uma task do Jira neste projeto - confere dependências, sugere o nome da branch e cria a spec a partir do template, conduzindo o dono da task no preenchimento. Use quando alguém disser que vai começar uma task (ex.: "vou pegar a KAN-23", "/iniciar-task T-17").
---

# Iniciar task (passos 0 e 1 do ai-workflow)

Siga `docs/processo/ai-workflow.md`. Fale em português.

1. **Identifique a task.** Peça a chave do Jira (ex.: `KAN-23`) e o ID do backlog (`T-NN`) se não foram informados. Leia a task em `docs/planejamento/backlog.md`.
2. **Confira as dependências** do campo "Depende de". Se alguma não estiver concluída na `main` (veja `git log` e os arquivos), avise antes de continuar.
3. **Sugira a branch** no padrão do `CONTRIBUTING.md` (`<tipo>/<CHAVE>-<descricao-curta>`) e mostre os comandos `git checkout main && git pull && git checkout -b ...`. Pode executá-los se a pessoa pedir; nunca faça commit nem push.
4. **Veja o fluxo da task** no backlog:
   - `—` (sem spec): diga que ela vai direto para "Em desenvolvimento" e ajude com um mini-plano curto no próprio Jira. Pare aqui.
   - `spec` ou `spec + grill`: continue.
5. **Crie a spec** copiando `docs/specs/_TEMPLATE.md` para `docs/specs/<CHAVE>-<descricao-curta>.md`. Preencha só o que é factual: chave, T-NN, autor (pergunte o usuário do GitHub), revisor pelo rodízio do ai-workflow, data, se é regra crítica e os requisitos (do backlog e do PRD).
6. **Conduza o preenchimento com perguntas**, seção por seção (1 a 8). Quem decide as regras, os critérios de aceite e os casos de teste é a pessoa. Você:
   - pergunta ("O que acontece se o término for igual ao início?");
   - aponta lacunas (valor limite faltando, erro 403 sem critério);
   - lembra das regras críticas da disciplina em `docs/projeto/contexto-da-disciplina.md`;
   - **não inventa** regra de negócio que não esteja no PRD. Se aparecer uma regra nova, ela entra primeiro no PRD.
7. **Ao terminar**, rode `./scripts/validar-spec.sh` só para mostrar o que falta (vai reprovar pelo status, o que é esperado), mude o status para `Em revisão` e sugira a mensagem de commit (`docs(spec): cria spec de <assunto> (<CHAVE>)`). Lembre a pessoa de mover a task no Jira para "Revisão da spec" e avisar o revisor.
