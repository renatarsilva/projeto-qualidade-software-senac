# Spec T-16: cadastro e consulta de salas

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-16 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Rascunho |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

A sala é o recurso principal das reservas (RF-02). Esta task cria o cadastro e a consulta de salas: o Administrador registra e altera as salas, e qualquer usuário autenticado as consulta. As validações de entrada (capacidade maior que zero, campos obrigatórios) valem aqui porque uma sala inválida contaminaria todas as reservas. Os erros usam o formato da T-22 e a permissão usa a T-15.

## 2. Escopo

**Dentro:**
- Cadastrar, alterar e consultar sala (por id e em lista), com nome, tipo da sala, capacidade, localização e o indicador `restrito`.
- Migração Flyway das tabelas `recurso` e `sala`, conforme o modelo da arquitetura (D-02).
- Validação de entrada e `409` para nome repetido.
- Cadastro e alteração só do Administrador; `401` e `403` conforme a T-15.

**Fora (e por quê):**
- Pesquisa com filtros por tipo, capacidade, localização e disponibilidade (RF-03): é de outra task.
- Exclusão de sala: o backlog não a pede, e uma sala pode ter reservas e histórico. Ver a premissa P-02.
- Manutenção da sala (RN-04) e reserva (T-20, T-23).
- O que o indicador `restrito` provoca (aprovação do Responsável, RN-07): aqui é só um dado.
- O formato das respostas de erro (T-22) e a autenticação (T-15).

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** Cadastro e alteração só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **Modelo (arquitetura 3.1):** a sala é um `recurso` do tipo `SALA`, com `nome` e `restrito`, mais `capacidade`, `localizacao` e `tipo_sala`.
- **S-03:** todo recurso tem o indicador booleano `restrito`.

**Propostas desta spec** (o PRD não as define; validação pendente):

- **P-01:** o nome da sala é único entre as salas (sem diferenciar maiúsculas de minúsculas) e tem até 100 caracteres. Localização e tipo da sala também têm até 100.
- **P-02:** não existe exclusão de sala nesta task. Quando houver, só poderá ocorrer para sala sem nenhuma reserva.
- **P-03:** `restrito` é opcional e vale `false` quando não informado.
- **P-04:** a capacidade é um número inteiro maior que zero, sem limite máximo definido.

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/salas`: cadastra uma sala (ADMINISTRADOR).

```json
{ "nome": "Sala 102", "tipoSala": "Laboratório", "capacidade": 30, "localizacao": "Bloco B, 1º andar", "restrito": false }
```

`PUT /api/salas/{id}`: altera uma sala, com o mesmo corpo (ADMINISTRADOR).

`GET /api/salas/{id}` e `GET /api/salas`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | A sala, com `id` |
| Alteração ou consulta | 200 | A sala (ou a lista) |
| Campo ausente ou inválido | 400 | `ProblemDetail`, `erros` com campo e motivo |
| Sala não encontrada | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Nome já cadastrado | 409 | `ProblemDetail` em linguagem de negócio |
| Sem autenticação | 401 | `ProblemDetail` (T-15) |
| Perfil sem permissão | 403 | `ProblemDetail` (T-15) |

## 5. Critérios de aceite

### CA-01: Administrador cadastra uma sala válida

- **Dado** um Administrador autenticado
- **Quando** ele cadastra uma sala com nome, tipo, capacidade maior que zero e localização
- **Então** a resposta é `201` com a sala e o seu `id`
- **E** `restrito` vale `false` quando não é informado
- **E** a sala fica gravada e pode ser consultada

### CA-02: Capacidade menor que 1 é recusada

- **Dado** o cadastro ou a alteração de sala
- **Quando** o cliente informa capacidade 0, negativa ou que não é número inteiro
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `capacidade` com o motivo "deve ser maior que zero"

### CA-03: Campos obrigatórios ausentes são recusados

- **Dado** o cadastro ou a alteração de sala
- **Quando** o cliente deixa em branco o nome, o tipo, a capacidade ou a localização
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta cada campo ausente

### CA-04: Nome repetido é recusado

- **Dado** uma sala já cadastrada
- **Quando** o cliente cadastra outra sala com o mesmo nome (ainda que com maiúsculas diferentes)
- **Então** o pedido é recusado com `409` em linguagem de negócio

### CA-05: Administrador altera uma sala

- **Dado** uma sala cadastrada
- **Quando** o Administrador a altera com dados válidos
- **Então** a resposta é `200` com a sala atualizada
- **E** as mesmas validações do cadastro valem na alteração

### CA-06: Qualquer usuário autenticado consulta

- **Dado** salas cadastradas
- **Quando** um usuário de qualquer perfil consulta uma sala por id ou a lista
- **Então** a resposta é `200`

### CA-07: Sala inexistente dá 404

- **Dado** um id que não corresponde a nenhuma sala
- **Quando** o cliente consulta ou altera essa sala
- **Então** a resposta é `404` com "Recurso não encontrado"

### CA-08: Só o Administrador cadastra e altera

- **Dado** as operações de cadastro e alteração
- **Quando** um SOLICITANTE ou um RESPONSAVEL as tenta, ou alguém sem autenticação
- **Então** quem está autenticado sem permissão recebe `403`, e quem não está autenticado recebe `401`
- **E** nada é gravado

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada
- **Quando** uma sala é gravada
- **Então** existe um registro em `recurso` com tipo `SALA` e outro em `sala` ligado a ele
- **E** a capacidade só aceita valores maiores que zero também no banco

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

As regras de validação são testadas em nível unitário; o contrato HTTP, a permissão e a gravação em integração, com o banco de teste (Testcontainers).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome, tipo, capacidade 30 e localização | 201; `restrito = false` | Integração |
| TC-02 | CA-02 | Valor limite | Capacidade = 1 | Aceita | Unitário |
| TC-03 | CA-02 | Valor limite | Capacidade = 0 | 400, `erros` com `campo = capacidade` | Unitário |
| TC-04 | CA-02 | Classe inválida | Capacidade = -1 | 400 | Unitário |
| TC-05 | CA-02 | Classe inválida | Capacidade = "abc" (não é número) | 400 | Integração |
| TC-06 | CA-03 | Classe inválida | Nome ausente | 400, `erros` com `campo = nome` | Unitário |
| TC-07 | CA-03 | Classe inválida | Nome só com espaços | 400 | Unitário |
| TC-08 | CA-03 | Classe inválida | Tipo, capacidade ou localização ausentes | 400; cada campo ausente aparece em `erros` | Unitário |
| TC-09 | CA-03 | Valor limite | Nome com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Unitário |
| TC-10 | CA-04 | Classe inválida | Cadastrar "Sala 102" depois de "sala 102" | 409 | Integração |
| TC-11 | CA-05 | Classe válida | Alterar o nome e a capacidade de uma sala | 200 com os dados novos | Integração |
| TC-12 | CA-05 | Classe inválida | Alterar a capacidade para 0 | 400; a sala não muda | Integração |
| TC-13 | CA-05 | Classe inválida | Alterar mantendo o próprio nome | 200 (não conflita consigo mesma) | Integração |
| TC-14 | CA-06 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três | Integração |
| TC-15 | CA-07 | Classe inválida | `GET` e `PUT` com id inexistente | 404 | Integração |
| TC-16 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST` e `PUT` | 403; nada é gravado | Integração |
| TC-17 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT` e `GET` | 401 | Integração |
| TC-18 | CA-09 | Classe válida | Gravar uma sala | Existe 1 linha em `recurso` (tipo `SALA`) e 1 em `sala` | Integração |
| TC-19 | CA-09 | Valor limite | `INSERT` direto no banco com capacidade 0 | Recusado pelo banco (`CHECK`) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Capacidade 0 ou negativa entra no banco por outro caminho | Alto | TC-03 e TC-04 na regra e TC-19 no banco (`CHECK`) |
| Erro de um na fronteira da capacidade (`>=` no lugar de `>`) | Alto | TC-02 e TC-03 testam 1 e 0 |
| A permissão do Administrador é esquecida em `PUT` | Alto | TC-16 e TC-17 cobrem `POST` e `PUT` por perfil |
| Nome repetido por diferença de maiúsculas | Médio | TC-10 e a proposta P-01 |
| O modelo `recurso` + `sala` fica inconsistente (uma tabela sem a outra) | Médio | TC-18 e gravação na mesma transação |
| Pesquisa com filtros (RF-03) e exclusão ficam sem dono | Médio | Fora desta spec; a exclusão só virá para sala sem reserva (P-02) |
| As propostas P-01 a P-04 não foram validadas pelo professor | Baixo | Marcadas como propostas; mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: cadastro válido | Entidade, migração, repositório e serviço de sala | Separar `recurso` e `sala` em um único agregado |
| 2 | TC-02 a TC-05: capacidade | Validação de capacidade maior que zero | Extrair a validação para o objeto de entrada |
| 3 | TC-06 a TC-09: campos obrigatórios | Validação de obrigatórios e de tamanho | Reaproveitar os textos de erro da T-22 |
| 4 | TC-10 e TC-13: nome único | Verificação de nome repetido sem diferenciar maiúsculas | Mover a regra para o serviço |
| 5 | TC-11, TC-12 e TC-15: alteração e inexistente | Alteração e `404` | Reaproveitar a validação do cadastro |
| 6 | TC-14, TC-16 e TC-17: permissão | Regra de perfil por operação (T-15) | Declarar o perfil exigido em um só lugar |
| 7 | TC-18 e TC-19: estrutura do banco | `CHECK` de capacidade e gravação em duas tabelas | Revisar a migração |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [ ] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:** _preencher_

**Parecer:** _preencher_ (Aprovada / Ajustes necessários) · @_preencher_ · AAAA-MM-DD

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
