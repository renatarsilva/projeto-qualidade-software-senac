# CLAUDE.md

## Regras de git (obrigatórias)

- **Nunca faça commits automaticamente.** Não execute `git commit`, `git commit --amend`, `git push`, `git tag`, `git merge` ou `git rebase`, nem crie pull requests, mesmo que pareça o próximo passo natural.
- Quando houver algo pronto para commit, **avise a usuária** e entregue:
  1. a lista dos arquivos alterados;
  2. uma mensagem de commit curta e explicativa, no padrão Conventional Commits em português (ex.: `docs: adiciona diagnóstico de qualidade da semana 32`).
- A usuária faz o commit e o push manualmente.
- Comandos de leitura (`git status`, `git diff`, `git log`) podem ser usados livremente.

## Contexto do projeto

- Projeto semestral de Qualidade de Software (TADS Senac 2026.2): sistema de Organização de Recursos.
- Especificação: https://afonsolelis.github.io/aulas_senac/pages/qualidade2/especificacao-projeto.html
- Projeto individual. A usuária está aprendendo: explique cada conceito e deixe que ela escreva as regras de negócio e os testes; revise e explique em vez de entregar pronto.
