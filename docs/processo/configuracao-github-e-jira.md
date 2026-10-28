# Configuração do GitHub e do Jira (task T-02)

> **Aviso (26/10/2026):** o Jira saiu do fluxo do projeto ([ADR-0003](../adr/0003-retirar-o-jira-do-fluxo.md)). A **Parte B (Jira)** está obsoleta e foi mantida só como histórico, até o retorno da Renata. A **Parte A (GitHub)**, com a proteção da `main`, continua valendo e ainda está pendente.

Passo a passo para deixar o repositório e o Jira prontos para o [AI Workflow](ai-workflow.md). Quem faz é a dona do repositório (@renatarsilva), porque essas telas exigem permissão de administrador.

> As telas do GitHub e do Jira mudam com o tempo. Se um nome de botão estiver um pouco diferente, procure a opção equivalente.

---

## Parte A: GitHub

### A.1 Forma de merge

**Settings → General → seção "Pull Requests":**

- ✅ **Allow merge commits**
- ⬜ Allow squash merging (desmarcar)
- ⬜ Allow rebase merging (desmarcar)
- ✅ **Automatically delete head branches**

**Por quê:** o *squash* junta todos os commits da branch em um só, e com isso somem os commits `red` e `green`, que são a evidência de TDD. O merge commit preserva o histórico. Apagar a branch depois do merge mantém o repositório limpo.

### A.2 Proteger a branch `main` (ruleset)

**Settings → Rules → Rulesets → New ruleset → New branch ruleset:**

| Campo | Valor | Por quê |
|---|---|---|
| Ruleset name | `Proteger main` | Nome descritivo |
| Enforcement status | **Active** | Senão a regra só fica registrada, sem efeito |
| Bypass list | **vazia** | Nem a administradora pula a regra. Se você estiver na lista, a revisão por pares deixa de ser garantida |
| Target branches | **Add target → Include default branch** | Aplica à `main` |

Em **Rules**, marque:

- ✅ **Restrict deletions**: ninguém apaga a `main`.
- ✅ **Require a pull request before merging**: ninguém dá push direto na `main`. Dentro dela:
  - **Required approvals: 1**: alguém além do autor precisa aprovar (o autor nunca consegue aprovar o próprio PR);
  - ✅ **Dismiss stale pull request approvals when new commits are pushed**: se o autor mudar o código depois da aprovação, a aprovação cai e o revisor precisa olhar de novo;
  - ✅ **Require conversation resolution before merging**: todo comentário do revisor precisa ser resolvido.
- ✅ **Block force pushes**: ninguém reescreve o histórico da `main`.
- ⏳ **Require status checks to pass**: **deixe para depois da T-25 (CI).** Esse item exige o nome de um check do CI, e o CI ainda não existe. Quando o primeiro PR com o CI rodar, volte aqui, marque a opção e adicione o check do workflow.

Clique em **Create**.

### A.3 Testar a proteção

Na sua máquina, tente um push direto na `main` com um commit qualquer de teste:

```bash
git checkout main
git commit --allow-empty -m "test: verifica proteção da main"
git push
```

O GitHub deve **recusar** o push com uma mensagem parecida com `Changes must be made through a pull request`. Depois desfaça o commit local:

```bash
git reset --hard origin/main
```

Se o push passar, a regra não está ativa: confira o *Enforcement status* e a *Bypass list*.

### A.4 Conferir os colaboradores

**Settings → Collaborators:** Samuel (@Samwel-Merces) e Caio (@CaiosAguiar) com o papel **Write**. O convite do Caio precisa ser aceito por ele.

---

## Parte B: Jira

### B.1 O projeto

O projeto já existe: https://renatarsilva.atlassian.net/jira/software/projects/KAN/boards/1, com a chave **`KAN`**. Falta:

1. **Convidar o time:** ⚙️ → *User management* (ou o botão **Invite** no topo) → convide o Samuel e o Caio pelo e-mail deles.
2. **Ligar as sprints, se o board for Kanban.** A chave `KAN` é a padrão do modelo Kanban, que não tem sprints. Em **Project settings → Features**, ative **Sprints** (projetos *team-managed*). Se preferir manter Kanban puro, use os rótulos `sprint-N` como filtro rápido.

### B.2 Colunas do board

**Board → ⋯ → Configure board → Columns** (ou *Project settings → Board*). Crie as colunas, nesta ordem:

`A fazer` · `Spec` · `Revisão da spec` · `Pronta para dev` · `Em desenvolvimento` · `Em revisão` · `Concluída`

O significado de cada uma está no [AI Workflow](ai-workflow.md#colunas-do-board-no-jira).

### B.3 Importar o backlog

O arquivo [`docs/planejamento/backlog-jira.csv`](../planejamento/backlog-jira.csv) tem as **58 tasks**, da semana 32 à 47. Cada uma traz título, área, semana, o que fazer, explicação (por quê), critérios de aceite e arquivos.

1. **⚙️ (canto superior direito) → System → External System Import → CSV.** Precisa ser administradora do site, o que você é.
2. Envie o CSV, com encoding **UTF-8** e separador **vírgula**.
3. Escolha o projeto **KAN**.
4. Mapeie as colunas:

| Coluna do CSV | Campo do Jira |
|---|---|
| Summary | Summary |
| Issue Type | Issue Type |
| Priority | Priority |
| Description | Description |
| Labels (as 5 colunas) | Labels |
| Assignee | Assignee |

5. No mapeamento de valores do **Assignee**, ligue `Renata`, `Samuel` e `Caio` aos usuários do Jira. Se eles ainda não aceitaram o convite, deixe sem responsável e atribua depois.
6. Clique em **Begin Import** e confira se as 58 tasks apareceram.

> A descrição usa a marcação do Jira (`h3.` para títulos e `*` para listas), que vira formatação na importação. Se aparecer como texto puro, o conteúdo continua legível.

### B.4 Organizar com os rótulos

O título de cada task já começa com o ID, a semana e a área, por exemplo: `[T-20] S36 · Testes · Fase red: ...`. Os rótulos (*labels*) permitem filtrar:

| Rótulo | Significado |
|---|---|
| `semana-32` ... `semana-47` | Semana do cronograma da disciplina |
| `sprint-1` ... `sprint-4` | Sprint real em que a task deve ser feita |
| `area-backend`, `area-frontend`, `area-testes`, `area-infra`, `area-documentacao` | Área da task |
| `sem-spec`, `spec`, `regra-critica` | Fluxo da task no ai-workflow |

Para montar as sprints: no backlog, crie as sprints 1 a 4 com as datas de [sprints.md](../planejamento/sprints.md), filtre por `labels = sprint-1`, selecione todas e mova para a Sprint 1. Repita para as outras.

### B.5 Ligar o Jira ao GitHub

1. No Jira: **Apps → Explore more apps → "GitHub for Jira"** (app gratuito da Atlassian) → **Get app**.
2. Conecte a conta do GitHub **renatarsilva** e dê acesso ao repositório `projeto-qualidade-software-senac`.
3. Teste: crie uma branch com uma chave existente (ex.: `docs/KAN-1-teste`) e dê push. Em alguns minutos, a branch deve aparecer no painel **Development** da task KAN-1. Depois apague a branch de teste.

Isso só funciona porque o padrão de branch, de commit e de PR inclui a chave do Jira.

---

## Checklist final da T-02

- [ ] Merge commit como única forma de merge; branches apagadas automaticamente
- [ ] Ruleset `Proteger main` ativo, sem bypass, testado com push recusado
- [ ] Caio aceitou o convite do GitHub
- [ ] Samuel e Caio convidados para o Jira
- [ ] Board com as 7 colunas e sprints ativadas
- [ ] 58 tasks importadas, nas sprints certas e com responsáveis
- [ ] GitHub for Jira conectado e testado
- [ ] Lembrete na T-25: adicionar o check do CI ao ruleset
