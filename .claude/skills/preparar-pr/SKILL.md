---
name: preparar-pr
description: Confere a Definition of Done de uma task terminada e preenche o template de pull request deste projeto (passo 6 do ai-workflow). Use quando alguém disser que terminou a task e quer abrir o PR.
---

# Preparar PR (passo 6 do ai-workflow)

Fale em português. Você **não abre o PR**: prepara o texto e a pessoa abre.

1. Descubra a branch atual (`git branch --show-current`) e extraia a chave do Jira do nome. Se a branch não seguir o padrão do `CONTRIBUTING.md`, avise.
2. Localize a spec em `docs/specs/<CHAVE>-*.md`, se a task tiver spec. Confira se o status é `Liberada`. Se não for, pare: o PR não pode ser aberto.
3. Confira a **Definition of Done** (seção 6 do ai-workflow):
   - rode `./mvnw verify` e mostre o resultado (se o projeto Maven já existir);
   - para cada critério de aceite (`CA-NN`) da spec, encontre o teste correspondente e liste-o. Critério sem teste bloqueia o PR;
   - verifique se `docs/rtm.md`, quando existir, tem a linha do requisito;
   - verifique se a branch está atualizada com a `main` (`git fetch && git log HEAD..origin/main --oneline`).
4. Leia os commits da branch (`git log origin/main..HEAD --oneline`) e confira se os ciclos red/green aparecem.
5. Preencha `.github/pull_request_template.md` com informações reais: só marque um item do checklist se ele foi verificado. Salve em um arquivo temporário fora do repositório.
6. Mostre o texto e o comando que a pessoa deve rodar:
   `gh pr create --title "<CHAVE>: <descrição>" --body-file <arquivo> --base main`
   Lembre de mover a task no Jira para "Em revisão" e avisar o revisor do rodízio.
