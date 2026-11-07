# Spec T-17: cadastro e consulta de professores e competências

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-17 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @CaiosAguiar |
| Status | Em revisão |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

O professor também é um recurso reservável (RF-02): a agenda dele não pode ter sobreposição (RN-02). Esta task cria o cadastro do professor com as suas competências, que alimentam a pesquisa por competência (RF-03). O Administrador escreve; qualquer usuário autenticado consulta. O professor é uma entidade separada do usuário que entra no sistema (ADR-0009).

## 2. Escopo

**Dentro:**
- Cadastrar, alterar, consultar (por id e em lista) e excluir professor, com nome, indicador `restrito` e lista de competências.
- Migração Flyway das tabelas `professor` e `competencia`, ligadas a `recurso` (tipo `PROFESSOR`). A tabela `recurso` é criada pela T-16, a primeira das três tasks de cadastro; esta task só acrescenta as suas tabelas.
- Validação de nome e de competências, e `409` para nome repetido.
- Escrita só do Administrador; `401` e `403` conforme a T-15.

**Fora (e por quê):**
- Pesquisa por competência (RF-03): é de outra task.
- O teste da recusa de exclusão de professor **com** reserva ou manutenção: essas tabelas ainda não existem. A regra é garantida pela chave estrangeira `ON DELETE RESTRICT` (arquitetura 3.2, restrição 5) e testada na T-23 (reserva) e na T-35 (manutenção).
- A agenda e a sobreposição do professor (RN-02): são das T-20 e T-23.
- Ligar o professor a um usuário do sistema: o ADR-0009 os mantém separados.
- O formato das respostas de erro (T-22) e a autenticação (T-15).

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** Cadastro e alteração só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **Modelo (arquitetura 3.1):** o professor é um `recurso` do tipo `PROFESSOR`; as competências ficam em `competencia`, com `descricao`.

**Propostas desta spec** (validação pendente):

- **P-01:** o nome tem até 100 caracteres e é único entre os professores, como manda a restrição `UNIQUE (tipo, nome)` da arquitetura (3.2, restrição 8). Homônimos usam um nome distinto, como "Ana Souza (Dados)" e "Ana Souza (Redes)".
- **P-02:** cada competência tem até 80 caracteres e é única dentro do mesmo professor, sem diferenciar maiúsculas de minúsculas. A lista pode ser vazia.
- **P-03:** ao alterar, a lista de competências enviada **substitui** a anterior.
- **P-04:** a exclusão só é permitida para professor sem nenhuma reserva (nem no histórico) nem período de manutenção; com qualquer um deles, é recusada. A garantia vem das chaves estrangeiras `ON DELETE RESTRICT`. Ao excluir, as competências saem junto.
- **P-05:** `restrito` é opcional e vale `false` quando não informado.

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/professores`: cadastra (ADMINISTRADOR).

```json
{ "nome": "Ana Souza", "restrito": false, "competencias": ["Banco de Dados", "Java"] }
```

`PUT /api/professores/{id}`: altera, com o mesmo corpo (ADMINISTRADOR). `DELETE /api/professores/{id}`: exclui (ADMINISTRADOR). `GET /api/professores/{id}` e `GET /api/professores`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | O professor, com `id` e competências |
| Alteração ou consulta | 200 | O professor (ou a lista) |
| Exclusão | 204 | Sem corpo |
| Nome ou competência inválidos | 400 | `ProblemDetail`, `erros` com campo e motivo |
| Nome repetido | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Já existe um professor com esse nome" |
| Professor não encontrado | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Exclusão de professor com reserva ou manutenção | 409 | `ProblemDetail` em linguagem de negócio |
| Sem autenticação ou sem permissão | 401 e 403 | `ProblemDetail` (T-15) |

## 5. Critérios de aceite

### CA-01: Administrador cadastra um professor com competências

- **Dado** um Administrador autenticado
- **Quando** ele cadastra um professor com nome e uma lista de competências
- **Então** a resposta é `201` com o professor e as competências
- **E** o professor fica gravado e pode ser consultado

### CA-02: Nome obrigatório e dentro do tamanho

- **Dado** o cadastro ou a alteração de professor
- **Quando** o cliente deixa o nome em branco ou passa de 100 caracteres
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `nome`

### CA-03: Competências válidas e sem repetição

- **Dado** o cadastro ou a alteração de professor
- **Quando** o cliente envia uma competência em branco, com mais de 80 caracteres ou repetida na mesma lista
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `competencias`

### CA-04: Alterar substitui as competências

- **Dado** um professor com competências
- **Quando** o Administrador o altera com uma lista nova
- **Então** a resposta é `200` e só a lista nova permanece

### CA-05: Qualquer usuário autenticado consulta

- **Dado** professores cadastrados
- **Quando** um usuário de qualquer perfil consulta um professor por id ou a lista
- **Então** a resposta é `200`, com as competências

### CA-06: Professor inexistente dá 404

- **Dado** um id que não corresponde a nenhum professor
- **Quando** o cliente consulta, altera ou exclui
- **Então** a resposta é `404` com "Recurso não encontrado"

### CA-07: Exclusão de professor sem vínculos

- **Dado** um professor sem reserva nem período de manutenção
- **Quando** o Administrador o exclui
- **Então** a resposta é `204` e as competências saem junto
- **E** a recusa com `409` para professor com reserva ou manutenção é garantida pelo banco e testada na T-23 e na T-35 (P-04)

### CA-08: Só o Administrador escreve

- **Dado** as operações de cadastro, alteração e exclusão
- **Quando** um SOLICITANTE ou um RESPONSAVEL as tenta, ou alguém sem autenticação
- **Então** quem está autenticado sem permissão recebe `403`, e quem não está autenticado recebe `401`
- **E** nada é gravado

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada
- **Quando** um professor é gravado
- **Então** existe um registro em `recurso` com tipo `PROFESSOR`, outro em `professor` e um em `competencia` por competência
- **E** o nome é único por tipo de recurso também no banco

### CA-10: Nome repetido é recusado

- **Dado** um professor cadastrado
- **Quando** o cliente cadastra outro professor com o mesmo nome, ou altera o nome de um para o de outro
- **Então** o pedido é recusado com `409` em linguagem de negócio e nada é gravado

## 6. Casos de teste

As validações são testadas em nível unitário; o contrato HTTP, a permissão e a gravação em integração, com o banco de teste (Testcontainers). A exclusão de professor com reserva ou manutenção não é testada aqui (ver escopo).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome e duas competências | 201 com as duas competências | Integração |
| TC-02 | CA-01 | Classe válida | Cadastro com lista de competências vazia | 201 | Integração |
| TC-03 | CA-02 | Classe inválida | Nome ausente ou só com espaços | 400, `erros` com `campo = nome` | Unitário |
| TC-04 | CA-02 | Valor limite | Nome com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Unitário |
| TC-05 | CA-03 | Classe inválida | Competência em branco | 400, `erros` com `campo = competencias` | Unitário |
| TC-06 | CA-03 | Valor limite | Competência com 80 caracteres / com 81 | 80 aceita; 81 recusa (400) | Unitário |
| TC-07 | CA-03 | Classe inválida | "Java" e "java" na mesma lista | 400 | Unitário |
| TC-08 | CA-04 | Classe válida | Alterar de [Java, SQL] para [Python] | 200; só "Python" permanece | Integração |
| TC-09 | CA-05 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três, com competências | Integração |
| TC-10 | CA-06 | Classe inválida | `GET`, `PUT` e `DELETE` com id inexistente | 404 | Integração |
| TC-11 | CA-07 | Classe válida | Excluir professor sem nenhuma reserva | 204; professor e competências somem | Integração |
| TC-13 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST`, `PUT` e `DELETE` | 403; nada é gravado | Integração |
| TC-14 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT`, `DELETE` e `GET` | 401 | Integração |
| TC-15 | CA-09 | Classe válida | Gravar um professor com 2 competências | 1 linha em `recurso` (tipo `PROFESSOR`), 1 em `professor`, 2 em `competencia` | Integração |
| TC-16 | CA-10 | Classe inválida | Cadastrar outro professor com o mesmo nome | 409; nada é gravado | Integração |
| TC-17 | CA-10 | Classe inválida | Alterar o nome de um professor para o de outro | 409 | Integração |
| TC-18 | CA-09 | Conflito | `INSERT` direto no banco com o mesmo tipo e nome | Recusado pelo banco (`UNIQUE (tipo, nome)`) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A permissão do Administrador é esquecida em `PUT` ou `DELETE` | Alto | TC-13 e TC-14 cobrem as três operações de escrita |
| Excluir professor deixa reserva ou manutenção órfã | Alto | P-04 e a chave estrangeira `ON DELETE RESTRICT`; o teste da recusa fica na T-23 e na T-35 |
| Competência duplicada por diferença de maiúsculas | Baixo | TC-07 |
| Dois professores com o mesmo nome (homônimos) são recusados pela restrição `UNIQUE (tipo, nome)` | Médio | P-01, TC-16 a TC-18: a spec mantém a restrição da arquitetura e exige um nome distinto |
| A T-16, a T-17 e a T-18 criariam a mesma tabela `recurso` | Médio | Só a T-16 a cria; a T-17 e a T-18 só acrescentam as suas tabelas (a T-17 depende da T-16 mergeada) |
| Pesquisa por competência (RF-03) fica sem dono | Médio | Fora desta spec; a lista de competências já fica pronta para a pesquisa |
| As propostas P-01 a P-05 são decisões do time | Baixo | Marcadas como propostas; mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-02: cadastro válido | Entidade, migração, repositório e serviço de professor | Tratar `recurso` e `professor` como um agregado |
| 2 | TC-03, TC-04 e TC-16: nome | Validação de nome e recusa de nome repetido no cadastro | Extrair a validação para o objeto de entrada |
| 3 | TC-05 a TC-07: competências | Validação de tamanho, vazio e repetição | Isolar a regra das competências |
| 4 | TC-08 a TC-10 e TC-17: alteração, consulta e 404 | Alteração que substitui a lista, nome único na alteração, consultas e `404` | Reaproveitar a validação do cadastro |
| 5 | TC-11: exclusão | Exclusão de professor sem vínculos | Mover a regra para o serviço |
| 6 | TC-13 e TC-14: permissão | Regra de perfil por operação (T-15) | Declarar o perfil exigido em um só lugar |
| 7 | TC-15 e TC-18: estrutura do banco | Gravação em três tabelas e `UNIQUE (tipo, nome)` | Revisar a migração |

## 9. Revisão por pares

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:** Conferi objetivo, escopo, regras de negócio, critérios de aceite, contrato da API e casos de teste.

Os critérios permitem validar aprovação e falha de forma objetiva. Os casos de teste cobrem entradas válidas, inválidas, limites e cenários de autorização. Não encontrei inconsistências com o PRD, a arquitetura ou o ADR-0009.

**Parecer:** Aprovada · @CaiosAguiar · 2026-11-05

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
