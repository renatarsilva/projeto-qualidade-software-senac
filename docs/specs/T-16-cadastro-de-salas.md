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
| Status | Em revisão |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

A sala é o recurso principal das reservas (RF-02). Esta task cria o cadastro e a consulta de salas: o Administrador registra e altera as salas, e qualquer usuário autenticado as consulta. As validações de entrada (capacidade maior que zero, campos obrigatórios) valem aqui porque uma sala inválida contaminaria todas as reservas. Os erros usam o formato da T-22 e a permissão usa a T-15.

## 2. Escopo

**Dentro:**
- Cadastrar, alterar e consultar sala (por id e em lista), com nome, tipo da sala, capacidade, localização e o indicador `restrito`.
- Migração Flyway das tabelas `recurso` e `sala`, conforme o modelo da arquitetura (D-02). **Esta task cria a tabela `recurso`**; a T-17 (professores) e a T-18 (materiais) só acrescentam as suas tabelas e dependem desta migração.
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

**Propostas desta spec** (o PRD não as define; são decisões do time):

- **P-01:** o nome da sala segue exatamente o padrão `sala-` seguido de 2 ou 3 dígitos (`sala-01`, `sala-99`, `sala-102`), tudo em minúsculas, sem espaços e sem nenhum outro caractere. Qualquer nome fora do padrão (espaço na frente ou atrás, `Sala-01`, `SALA-01`, `sala 01`, `saal-01`, `sala-1`, `sala-01x`) é recusado com `400`, e nada é corrigido automaticamente. O nome é único entre as salas. No banco, o padrão é um `CHECK` e a unicidade é a restrição `UNIQUE (tipo, nome)` da arquitetura (3.2, restrição 8). O tipo da sala e a localização têm até 100 caracteres.
- **P-02:** não existe exclusão de sala nesta task. Quando houver, só poderá ocorrer para sala sem nenhuma reserva.
- **P-03:** `restrito` é opcional e vale `false` quando não informado, no cadastro e na alteração. O `PUT` substitui a sala por inteiro, então um `restrito` omitido volta a `false`.
- **P-04:** a capacidade é um número inteiro maior que zero. Não há limite de negócio, mas o valor precisa caber no inteiro do banco (até 2.147.483.647); acima disso, `400`.
- **P-05:** o tipo da sala e a localização perdem os espaços das pontas e têm os espaços repetidos no meio reduzidos a um só antes de validar e gravar. O nome **não** passa por limpeza: um espaço no nome é erro (P-01).

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/salas`: cadastra uma sala (ADMINISTRADOR).

```json
{ "nome": "sala-102", "tipoSala": "Laboratório", "capacidade": 30, "localizacao": "Bloco B, 1º andar", "restrito": false }
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
- **Quando** o cliente informa capacidade 0, negativa, decimal, acima de 2.147.483.647 ou que não é número
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `capacidade` com o motivo "deve ser maior que zero"

### CA-03: Campos obrigatórios ausentes são recusados

- **Dado** o cadastro ou a alteração de sala
- **Quando** o cliente deixa em branco o nome, o tipo, a capacidade ou a localização, ou passa de 100 caracteres no tipo ou na localização
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta cada campo inválido

### CA-04: Nome repetido é recusado

- **Dado** uma sala já cadastrada
- **Quando** o cliente cadastra outra sala com o mesmo nome, ou altera o nome de uma sala para o de outra
- **Então** o pedido é recusado com `409` em linguagem de negócio

### CA-05: Administrador altera uma sala

- **Dado** uma sala cadastrada
- **Quando** o Administrador a altera com dados válidos
- **Então** a resposta é `200` com a sala atualizada
- **E** as mesmas validações do cadastro valem na alteração
- **E** um `restrito` omitido no `PUT` volta a `false` (P-03)

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
- **E** o nome só aceita o padrão `sala-NN` (`CHECK`) e é único por tipo de recurso também no banco

### CA-10: O nome segue o padrão sala-NN

- **Dado** o cadastro ou a alteração de sala
- **Quando** o nome não é exatamente `sala-` seguido de 2 ou 3 dígitos, em minúsculas: com espaço na frente, atrás ou no meio, com maiúscula, com erro na palavra `sala`, com 1 ou 4 dígitos, ou com qualquer outro caractere
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `nome` com o motivo "deve seguir o padrão sala-NN"
- **E** um nome no padrão é aceito (`sala-01`, `sala-99`, `sala-102`, `sala-999`)

## 6. Casos de teste

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
| TC-09 | CA-10 | Valor limite | Nome `sala-01` (2 dígitos) / `sala-1` (1 dígito) | `sala-01` aceita; `sala-1` recusa (400) | Unitário |
| TC-10 | CA-04 | Classe inválida | Cadastrar `sala-01` depois de `sala-01` | 409 | Integração |
| TC-11 | CA-05 | Classe válida | Alterar o nome e a capacidade de uma sala | 200 com os dados novos | Integração |
| TC-12 | CA-05 | Classe inválida | Alterar a capacidade para 0 | 400; a sala não muda | Integração |
| TC-13 | CA-05 | Classe inválida | Alterar mantendo o próprio nome | 200 (não conflita consigo mesma) | Integração |
| TC-14 | CA-06 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três | Integração |
| TC-15 | CA-07 | Classe inválida | `GET` e `PUT` com id inexistente | 404 | Integração |
| TC-16 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST` e `PUT` | 403; nada é gravado | Integração |
| TC-17 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT` e `GET` | 401 | Integração |
| TC-18 | CA-09 | Classe válida | Gravar uma sala | Existe 1 linha em `recurso` (tipo `SALA`) e 1 em `sala` | Integração |
| TC-19 | CA-09 | Valor limite | `INSERT` direto no banco com capacidade 0 | Recusado pelo banco (`CHECK`) | Integração |
| TC-20 | CA-02 | Classe inválida | Capacidade = 30.5 (decimal) | 400, `erros` com `campo = capacidade` | Integração |
| TC-21 | CA-02 | Valor limite | Capacidade = 2147483647 / 2147483648 | 2147483647 aceita; 2147483648 recusa (400) | Integração |
| TC-22 | CA-03 | Valor limite | Tipo e localização com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Unitário |
| TC-23 | CA-04 | Classe inválida | Alterar o nome de uma sala para o de outra | 409; a sala não muda | Integração |
| TC-24 | CA-05 | Classe válida | `PUT` sem o campo `restrito`, em uma sala que era `restrito = true` | 200; `restrito = false` | Integração |
| TC-25 | CA-09 | Conflito | `INSERT` direto no banco com o nome `Sala-01` | Recusado pelo banco (`CHECK` do padrão) | Integração |
| TC-26 | CA-09 | Conflito | `INSERT` direto no banco com o mesmo tipo e nome de outra sala | Recusado pelo banco (`UNIQUE (tipo, nome)`) | Integração |
| TC-27 | CA-10 | Classe inválida | Nomes ` sala-01`, `sala-01 `, `Sala-01`, `SALA-01`, `sala 01`, `saal-01`, `sala-01x`, `sala_01` (teste parametrizado) | Todos recusados com 400; `erros` com `campo = nome` | Unitário |
| TC-28 | CA-10 | Valor limite | Nome `sala-999` (3 dígitos) / `sala-1000` (4 dígitos) | `sala-999` aceita; `sala-1000` recusa (400) | Unitário |
| TC-29 | CA-03 | Classe válida | Cadastro com tipo `"  Laboratório "` e localização com espaços repetidos | 201; tipo e localização gravados sem os espaços extras (P-05) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Capacidade 0 ou negativa entra no banco por outro caminho | Alto | TC-03 e TC-04 na regra e TC-19 no banco (`CHECK`) |
| Erro de um na fronteira da capacidade (`>=` no lugar de `>`) | Alto | TC-02 e TC-03 testam 1 e 0 |
| A permissão do Administrador é esquecida em `PUT` | Alto | TC-16 e TC-17 cobrem `POST` e `PUT` por perfil |
| Nome fora do padrão, com espaço ou parecido com outro ("Saal 102", "Sala 102") | Médio | O padrão `sala-NN` é obrigatório e nada é corrigido sozinho: TC-27 e TC-28, o `CHECK` do banco (TC-25) e a proposta P-01 |
| Nome repetido | Médio | TC-10, TC-23 e `UNIQUE (tipo, nome)` (TC-26) |
| O modelo `recurso` + `sala` fica inconsistente (uma tabela sem a outra) | Médio | TC-18 e gravação na mesma transação |
| Pesquisa com filtros (RF-03) e exclusão ficam sem dono | Médio | Fora desta spec; a exclusão só virá para sala sem reserva (P-02) |
| As propostas P-01 a P-05 são decisões do time | Baixo | Marcadas como propostas; mudar um limite altera poucos testes |
| A T-17 e a T-18 dependem da tabela `recurso`, criada aqui | Médio | A T-16 precisa ser mergeada antes das duas |
| Capacidade acima do inteiro do banco causa erro 500 | Médio | P-04, TC-21 e TC-20 |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: cadastro válido | Entidade, migração, repositório e serviço de sala | Separar `recurso` e `sala` em um único agregado |
| 2 | TC-02 a TC-05, TC-20 e TC-21: capacidade | Validação de capacidade maior que zero | Extrair a validação para o objeto de entrada |
| 3 | TC-06 a TC-09, TC-22, TC-27 a TC-29: campos obrigatórios, padrão do nome e tamanhos | Validação do padrão `sala-NN`, de obrigatórios e de tamanho de tipo e localização, e limpeza de espaços do tipo e da localização (P-05) | Reaproveitar os textos de erro da T-22 |
| 4 | TC-10: nome único no cadastro | Verificação de nome repetido | Mover a regra para o serviço |
| 5 | TC-11 a TC-13, TC-15, TC-23 e TC-24: alteração e inexistente | Alteração (com o `PUT` substituindo por inteiro), nome único na alteração e `404` | Reaproveitar a validação do cadastro |
| 6 | TC-14, TC-16 e TC-17: permissão | Regra de perfil por operação (T-15) | Declarar o perfil exigido em um só lugar |
| 7 | TC-18, TC-19, TC-25 e TC-26: estrutura do banco | `CHECK` de capacidade, `CHECK` do padrão do nome, `UNIQUE (tipo, nome)` e gravação em duas tabelas | Revisar a migração |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [ ] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Reli a versão atual. A spec melhorou: os limites de tipo e localização, o nome repetido no `PUT` e a responsabilidade pela criação de `recurso` estão resolvidos. O objetivo e o escopo continuam claros, e o contrato traz os códigos 400, 401, 403 e 409.

Regras no PRD: o padrão `sala-NN` é uma regra nova, que não está no PRD, na arquitetura nem no backlog. A arquitetura usa `SALA-102`, recusado pelo padrão, e a T-20 usa "sala 102". É preciso validar a regra e registrá-la no PRD. O padrão também não prevê nomes como `lab-01`, embora o tipo "Laboratório" seja aceito. A seção 3 fala em "propostas" e "decisões do time" ao mesmo tempo.

Critérios: o CA-02 usa o motivo "deve ser maior que zero" para decimal, texto e valor acima do inteiro, o que fica falso para `2147483648`. A P-05 não tem critério próprio: TC-29 está ligado ao CA-03, que trata de recusas.

Testes: `30.5` pode virar `30` na leitura do JSON e `"30"` pode ser aceito como número, então a spec precisa definir essa configuração e o ciclo 2 deve incluí-la. Também é preciso alinhar com a T-22 como esses erros de leitura chegam a `erros` com `campo = capacidade`. Continuam abertos os casos marcados como unitários que esperam HTTP (TC-03, TC-04, TC-06 a TC-09, TC-22, TC-27 e TC-28) e o detalhamento de TC-08. Faltam casos para `sala-01` junto com `sala-001`, para tipo ou localização com 100 caracteres depois da limpeza e para valores só com espaços.

Contrato: o 409 de nome repetido está previsto, mas falta definir a resposta quando dois cadastros simultâneos passam pela verificação e um deles é barrado pelo `UNIQUE`. Sem essa tradução, a resposta seria 500. A escolha de `restrito` omitido voltar a `false` no `PUT` também retira a exigência de aprovação sem pedido explícito. Recomendo tornar o campo obrigatório no `PUT` ou registrar esse risco na seção 7.

Conflitos: o `CHECK` do padrão fica em `recurso.nome`, coluna compartilhada, e precisa valer só para o tipo `SALA`, senão pode recusar professores e materiais. Falta um caso provando isso. A T-16 cria `UNIQUE (tipo, nome)`, mas a T-18 espera `UNIQUE (tipo, lower(nome))`, então é preciso definir a versão final ou a migração que faz a troca. A spec também não define o tamanho de `recurso.nome`. A T-17 e a T-18 agora dependem da T-16, mas o backlog ainda cita só a T-15.

**Parecer:** Ajustes necessários · @Samwel-Merces · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
