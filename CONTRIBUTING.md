# Guia de contribuição

Este guia define **as convenções** do time. O passo a passo de cada task (spec, revisão, validação, grill, TDD e PR) está no [AI Workflow](docs/processo/ai-workflow.md). Leia os dois antes da primeira task.

## Time

| Pessoa | GitHub | Revisa as tasks de |
|---|---|---|
| Renata | [@renatarsilva](https://github.com/renatarsilva) | Caio |
| Samuel | [@Samwel-Merces](https://github.com/Samwel-Merces) | Renata |
| Caio | [@CaiosAguiar](https://github.com/CaiosAguiar) | Samuel |

**Rodízio temporário:** enquanto a Renata estiver ausente, o Samuel revisa as tasks do Caio (regra da terceira pessoa do [AI Workflow](docs/processo/ai-workflow.md#quem-revisa-quem)), e o Caio continua revisando as do Samuel. A substituição é anotada no PR. Quando a Renata voltar, o rodízio da tabela volta a valer.

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
<tipo>/<ID-DA-TASK>-<descricao-curta>
```

| Tipo | Quando usar | Exemplo |
|---|---|---|
| `feature` | Nova funcionalidade | `feature/T-23-criar-reserva` |
| `fix` | Correção de defeito | `fix/T-23-sobreposicao-meia-noite` |
| `test` | Só testes (sem código de produção) | `test/T-49-matriz-autorizacao` |
| `docs` | Só documentação | `docs/T-09-prd` |
| `chore` | Configuração, build, CI, dependências | `chore/T-25-pipeline-ci` |
| `refactor` | Melhoria sem mudar comportamento | `refactor/T-23-extrai-validador-intervalo` |

Regras:
- a descrição vai em **minúsculas, sem acento, com hífens** e em até 5 palavras;
- **uma branch por task**, sempre criada a partir da `main` atualizada;
- o ID da task (`T-NN`, do [backlog](docs/planejamento/backlog.md)) é obrigatório: é ele que liga a branch, os commits e o PR à task. Trabalho que não é task do backlog usa o número da issue (ex.: `docs/issue-5-retirar-jira`). O Jira não é usado ([ADR-0003](docs/adr/0003-retirar-o-jira-do-fluxo.md)).

## Commits

Padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/), em português, com o ID da task no final (ou `#N`, o número da issue, quando não houver task):

```
<tipo>(<escopo>): <o que mudou, no imperativo> (<ID-DA-TASK>)
```

```
docs(spec): cria spec de criação de reserva (T-23)
feat(sala): adiciona cadastro de salas (T-16)
fix(sobreposicao): considera reservas que cruzam a meia-noite (T-23)
docs(adr): registra a retirada do Jira do fluxo (#5)
```

- Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `ci`, `build`.
- Escopo: a área do sistema (`reserva`, `sala`, `auth`, `auditoria`, `spec`, `ci`...).
- Commits pequenos e frequentes.
- Na sua branch, você **pode commitar e dar push à vontade**.

### Commits de TDD

Nas tasks de TDD, use o formato que o professor procura no histórico (Aula 09), um ciclo por cenário:

```
test(red): término anterior ao início é recusado (T-23)
feat(green): término anterior ao início é recusado (T-23)
refactor: extrai validação de intervalo (T-23)
```

- O `test(red)` só é commitado depois de rodar `./mvnw -B test` e ver o teste **falhar por asserção** (não por erro de compilação).
- Nunca escreva vários testes de uma vez, nunca altere o teste para passar e nunca pule o vermelho.
- Fora do commit `test(red)`, rode `./mvnw -B verify` antes de cada commit.

## Pull requests

- **Só abra o PR quando a task estiver terminada**, com a Definition of Done do [AI Workflow](docs/processo/ai-workflow.md#passo-6-abrir-o-pr-só-com-a-task-terminada) cumprida. Nada de PR em rascunho ou pela metade.
- Título: `<ID-DA-TASK>: <descrição>`. Exemplo: `T-23: criar reserva com validação de intervalo`.
- O corpo segue o [template](.github/pull_request_template.md), que o GitHub preenche automaticamente. Se o PR resolve uma issue, escreva `Closes #N` no corpo.
- **Revisor:** a pessoa do rodízio (tabela acima, incluindo o rodízio temporário).
- **Merge:** feito pelo autor, depois de 1 aprovação e com o CI verde, usando **"Create a merge commit"** (não use *squash*, para preservar os commits red/green). Apague a branch depois do merge.

## A branch `main`

- Ninguém faz push direto na `main`: tudo entra por PR, com **1 aprovação** de outra pessoa e apenas por **merge commit**. A regra é garantida pelo ruleset "Proteger main" do GitHub, sem bypass.
- As regras de proteção da `main` e as configurações do repositório são regras explícitas do projeto. Apenas a Renata pode alterá-las.
- A `main` deve estar sempre executável e com o CI verde.

## Defeitos

Defeitos encontrados durante o trabalho são registrados como **issue no GitHub com o rótulo `bug`** e documentados em `docs/defeitos/` (o processo é criado na task T-14). A correção segue o mesmo fluxo de qualquer task, com branch `fix/...`.

## Uso de IA

O time usa o Claude Code. As regras para a IA estão no [`CLAUDE.md`](CLAUDE.md) e valem para todos. Em resumo: a IA explica, revisa e ajuda, mas **cada pessoa escreve as regras de negócio e os testes da própria task** e faz os próprios commits. Na prova oral, cada um explica o que fez.
