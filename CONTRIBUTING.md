# Guia de contribuição

Este guia define **as convenções** do time. O passo a passo de cada task (spec, revisão, validação, grill, TDD e PR) está no [AI Workflow](docs/processo/ai-workflow.md). Leia os dois antes da primeira task.

## Time

| Pessoa | GitHub | Revisa as tasks de |
|---|---|---|
| Renata | [@renatarsilva](https://github.com/renatarsilva) | Caio |
| Samuel | [@Samwel-Merces](https://github.com/Samwel-Merces) | Renata |
| Caio | [@CaiosAguiar](https://github.com/CaiosAguiar) | Samuel |

## Primeira configuração

```bash
git clone https://github.com/renatarsilva/projeto-qualidade-software-senac.git
cd projeto-qualidade-software-senac
git config user.name "Seu Nome"
git config user.email "o-email-da-sua-conta-do-github@exemplo.com"
```

O e-mail precisa ser o mesmo da sua conta do GitHub. Sem isso, seus commits não aparecem no seu nome, e a **autoria** vale nota.

Ferramentas: Java 21, Docker e GitHub CLI (`gh`). O Maven não precisa ser instalado: use `./mvnw`.

## Nomes de branch

```
<tipo>/<CHAVE-JIRA>-<descricao-curta>
```

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feature` | Nova funcionalidade | `feature/KAN-23-criar-reserva` |
| `fix` | Correção de defeito | `fix/KAN-41-sobreposicao-meia-noite` |
| `test` | Só testes (sem código de produção) | `test/KAN-38-matriz-autorizacao` |
| `docs` | Só documentação | `docs/KAN-7-prd` |
| `chore` | Configuração, build, CI, dependências | `chore/KAN-5-pipeline-ci` |
| `refactor` | Melhoria sem mudar comportamento | `refactor/KAN-44-extrai-validador-intervalo` |

Regras:
- a descrição vai em **minúsculas, sem acento, com hífens** e em até 5 palavras;
- **uma branch por task**, sempre criada a partir da `main` atualizada;
- a chave do Jira é obrigatória: é ela que liga a branch, os commits e o PR à task no Jira.

## Commits

Padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português, com a chave do Jira no final:

```
<tipo>(<escopo>): <o que mudou, no imperativo> (<CHAVE-JIRA>)
```

```
docs(spec): cria spec de criação de reserva (KAN-23)
feat(sala): adiciona cadastro de salas (KAN-16)
fix(sobreposicao): considera reservas que cruzam a meia-noite (KAN-41)
```

- Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `ci`, `build`.
- Escopo: a área do sistema (`reserva`, `sala`, `auth`, `auditoria`, `spec`, `ci`...).
- Commits pequenos e frequentes.
- Na sua branch, você **pode commitar e dar push à vontade**.

### Commits de TDD

Nas tasks de TDD, use o formato que o professor procura no histórico (Aula 09), um ciclo por cenário:

```
test(red): término anterior ao início é recusado (KAN-23)
feat(green): término anterior ao início é recusado (KAN-23)
refactor: extrai validação de intervalo (KAN-23)
```

- O `test(red)` só é commitado depois de rodar `./mvnw -B test` e ver o teste **falhar por asserção** (não por erro de compilação).
- Nunca escreva vários testes de uma vez, nunca altere o teste para passar e nunca pule o vermelho.
- Fora do commit `test(red)`, rode `./mvnw -B verify` antes de cada commit.

## Pull requests

- **Só abra o PR quando a task estiver terminada**, com a Definition of Done do [AI Workflow](docs/processo/ai-workflow.md#passo-6-abrir-o-pr-só-com-a-task-terminada) cumprida. Nada de PR em rascunho ou pela metade.
- Título: `<CHAVE-JIRA>: <descrição>`. Exemplo: `KAN-23: criar reserva com validação de intervalo`.
- O corpo segue o [template](.github/pull_request_template.md), que o GitHub preenche automaticamente.
- **Revisor:** a pessoa do rodízio (tabela acima).
- **Merge:** feito pelo autor, depois de 1 aprovação e com o CI verde, usando **"Create a merge commit"** (não use *squash*, para preservar os commits red/green). Apague a branch depois do merge.

## A branch `main`

- Ninguém faz push direto na `main`: tudo entra por PR. A regra é garantida pela proteção de branch no GitHub.
- A `main` deve estar sempre executável e com o CI verde.

## Defeitos

Defeitos encontrados durante o trabalho são registrados como **Bug** no Jira e documentados em `docs/defeitos/` (o processo é criado na task T-14). A correção segue o mesmo fluxo de qualquer task, com branch `fix/...`.

## Uso de IA

O time usa o Claude Code. As regras para a IA estão no [`CLAUDE.md`](CLAUDE.md) e valem para todos. Em resumo: a IA explica, revisa e ajuda, mas **cada pessoa escreve as regras de negócio e os testes da própria task** e faz os próprios commits. Na prova oral, cada um explica o que fez.
