# Spec T-18: cadastro e consulta de materiais e equipamentos

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-18 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Rascunho |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

O material (projetor, notebook, equipamentos) é o terceiro tipo de recurso (RF-02) e a base da retirada e da devolução (RF-09). Esta task cria o cadastro do material com tipo e número de patrimônio. O Administrador escreve; qualquer usuário autenticado consulta.

## 2. Escopo

**Dentro:**
- Cadastrar, alterar, consultar (por id e em lista) e excluir material, com nome, tipo, patrimônio e o indicador `restrito`.
- Migração Flyway de `recurso` (tipo `MATERIAL`) e `material`.
- Validação de entrada e `409` para patrimônio repetido.
- Escrita só do Administrador; `401` e `403` conforme a T-15.

**Fora (e por quê):**
- A **situação** do material pedida no backlog: ela se choca com o período de manutenção (RN-04) e com a retirada (RF-09), que já definem se o material está disponível. Ver a premissa P-05.
- Retirada, devolução (RF-09) e manutenção (RN-04): são de outras tasks.
- Pesquisa com filtros (RF-03).
- O que o indicador `restrito` provoca (RN-07): aqui é só um dado.
- O formato das respostas de erro (T-22) e a autenticação (T-15).

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** Cadastro e alteração só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **Modelo (arquitetura 3.1):** o material é um `recurso` do tipo `MATERIAL`, com `tipo_material`. O modelo **não tem** a coluna de patrimônio; esta spec a acrescenta (P-01).

**Propostas desta spec** (validação pendente):

- **P-01:** o material tem `patrimonio`, obrigatório, com até 30 caracteres, único entre os materiais (sem diferenciar maiúsculas) e sem espaços nas pontas. **O modelo da arquitetura precisa ganhar essa coluna.**
- **P-02:** o nome e o tipo têm até 100 caracteres.
- **P-03:** a exclusão só é permitida para material sem nenhuma reserva nem retirada; com reserva ou retirada, é recusada.
- **P-04:** `restrito` é opcional e vale `false` quando não informado.
- **P-05:** a situação do material não é um campo guardado: a disponibilidade vem da manutenção, da reserva e da retirada. Se o professor exigir um campo de situação, a spec muda.

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/materiais`: cadastra (ADMINISTRADOR).

```json
{ "nome": "Projetor Epson", "tipoMaterial": "Projetor", "patrimonio": "PAT-000123", "restrito": true }
```

`PUT /api/materiais/{id}`: altera, com o mesmo corpo (ADMINISTRADOR). `DELETE /api/materiais/{id}`: exclui (ADMINISTRADOR). `GET /api/materiais/{id}` e `GET /api/materiais`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | O material, com `id` |
| Alteração ou consulta | 200 | O material (ou a lista) |
| Exclusão | 204 | Sem corpo |
| Campo ausente ou inválido | 400 | `ProblemDetail`, `erros` com campo e motivo |
| Material não encontrado | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Patrimônio repetido, ou exclusão de material com reserva ou retirada | 409 | `ProblemDetail` em linguagem de negócio |
| Sem autenticação ou sem permissão | 401 e 403 | `ProblemDetail` (T-15) |

## 5. Critérios de aceite

### CA-01: Administrador cadastra um material válido

- **Dado** um Administrador autenticado
- **Quando** ele cadastra um material com nome, tipo e patrimônio
- **Então** a resposta é `201` com o material e o seu `id`
- **E** `restrito` vale `false` quando não é informado

### CA-02: Campos obrigatórios e tamanhos

- **Dado** o cadastro ou a alteração de material
- **Quando** o cliente deixa em branco o nome, o tipo ou o patrimônio, ou passa dos tamanhos máximos
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta cada campo inválido

### CA-03: Patrimônio repetido é recusado

- **Dado** um material cadastrado
- **Quando** o cliente cadastra outro com o mesmo patrimônio (ainda que com maiúsculas diferentes)
- **Então** o pedido é recusado com `409` em linguagem de negócio

### CA-04: Administrador altera um material

- **Dado** um material cadastrado
- **Quando** o Administrador o altera com dados válidos
- **Então** a resposta é `200` com o material atualizado
- **E** as validações do cadastro valem na alteração

### CA-05: Qualquer usuário autenticado consulta

- **Dado** materiais cadastrados
- **Quando** um usuário de qualquer perfil consulta um material por id ou a lista
- **Então** a resposta é `200`

### CA-06: Material inexistente dá 404

- **Dado** um id que não corresponde a nenhum material
- **Quando** o cliente consulta, altera ou exclui
- **Então** a resposta é `404` com "Recurso não encontrado"

### CA-07: Exclusão só sem reserva nem retirada

- **Dado** um material
- **Quando** o Administrador o exclui
- **Então** sem reserva nem retirada, a resposta é `204`
- **E** com reserva ou retirada, a resposta é `409` e o material continua

### CA-08: Só o Administrador escreve

- **Dado** as operações de cadastro, alteração e exclusão
- **Quando** um SOLICITANTE ou um RESPONSAVEL as tenta, ou alguém sem autenticação
- **Então** quem está autenticado sem permissão recebe `403`, e quem não está autenticado recebe `401`
- **E** nada é gravado

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada
- **Quando** um material é gravado
- **Então** existe um registro em `recurso` com tipo `MATERIAL` e outro em `material` ligado a ele
- **E** o patrimônio é único também no banco

## 6. Casos de teste

Todo critério de aceite precisa de pelo menos um caso. Use classes de equivalência e valor limite.

As validações são testadas em nível unitário; o contrato HTTP, a permissão e a gravação em integração, com o banco de teste (Testcontainers). O caso de exclusão com reserva ou retirada só roda quando essas tabelas existirem (ver riscos).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome, tipo e patrimônio | 201; `restrito = false` | Integração |
| TC-02 | CA-02 | Classe inválida | Nome, tipo ou patrimônio ausentes ou só com espaços | 400; cada campo aparece em `erros` | Unitário |
| TC-03 | CA-02 | Valor limite | Patrimônio com 30 caracteres / com 31 | 30 aceita; 31 recusa (400) | Unitário |
| TC-04 | CA-02 | Valor limite | Nome com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Unitário |
| TC-05 | CA-02 | Classe válida | Patrimônio com espaços nas pontas | Aceito, gravado sem os espaços | Unitário |
| TC-06 | CA-03 | Classe inválida | Cadastrar "PAT-1" depois de "pat-1" | 409 | Integração |
| TC-07 | CA-04 | Classe válida | Alterar o nome e o tipo de um material | 200 com os dados novos | Integração |
| TC-08 | CA-04 | Classe válida | Alterar mantendo o próprio patrimônio | 200 (não conflita consigo mesmo) | Integração |
| TC-09 | CA-04 | Classe inválida | Alterar o patrimônio para o de outro material | 409 | Integração |
| TC-10 | CA-05 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três | Integração |
| TC-11 | CA-06 | Classe inválida | `GET`, `PUT` e `DELETE` com id inexistente | 404 | Integração |
| TC-12 | CA-07 | Classe válida | Excluir material sem reserva nem retirada | 204 | Integração |
| TC-13 | CA-07 | Conflito | Excluir material que tem reserva ou retirada | 409; o material continua | Integração |
| TC-14 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST`, `PUT` e `DELETE` | 403; nada é gravado | Integração |
| TC-15 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT`, `DELETE` e `GET` | 401 | Integração |
| TC-16 | CA-09 | Classe válida | Gravar um material | 1 linha em `recurso` (tipo `MATERIAL`) e 1 em `material` | Integração |
| TC-17 | CA-09 | Conflito | `INSERT` direto no banco com patrimônio repetido | Recusado pelo banco (`UNIQUE`) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A permissão do Administrador é esquecida em `PUT` ou `DELETE` | Alto | TC-14 e TC-15 cobrem as três operações de escrita |
| Patrimônio duplicado entra por outro caminho | Alto | TC-06 e TC-09 na regra e TC-17 no banco |
| O modelo da arquitetura não tem a coluna de patrimônio | Médio | A revisão deve aprovar a mudança do modelo (P-01) e atualizar `docs/arquitetura.md` |
| O backlog pede a situação do material e a spec não a guarda | Médio | Decisão explícita em P-05; levar ao professor |
| Excluir material deixa reserva ou retirada órfã | Alto | TC-13 e a proposta P-03; depende das tabelas de reserva e retirada, então roda quando existirem |
| As propostas P-01 a P-05 não foram validadas pelo professor | Baixo | Marcadas como propostas; mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: cadastro válido | Entidade, migração, repositório e serviço de material | Tratar `recurso` e `material` como um agregado |
| 2 | TC-02 a TC-05: campos obrigatórios e tamanhos | Validação de obrigatórios, tamanhos e limpeza de espaços | Extrair a validação para o objeto de entrada |
| 3 | TC-06, TC-08 e TC-09: patrimônio único | Verificação de patrimônio repetido sem diferenciar maiúsculas | Mover a regra para o serviço |
| 4 | TC-07, TC-10 e TC-11: alteração, consulta e 404 | Alteração, consultas e `404` | Reaproveitar a validação do cadastro |
| 5 | TC-12 e TC-13: exclusão | Exclusão permitida só sem reserva nem retirada | Mover a regra para o serviço |
| 6 | TC-14 e TC-15: permissão | Regra de perfil por operação (T-15) | Declarar o perfil exigido em um só lugar |
| 7 | TC-16 e TC-17: estrutura do banco | `UNIQUE` do patrimônio e gravação em duas tabelas | Revisar a migração |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:**
O objetivo e o escopo da spec estão claros, e as funcionalidades fora do escopo estão justificadas. As regras citam requisitos que existem no PRD. O contrato da API contempla os erros 400, 401, 403 e 409, e há casos de teste associados aos critérios de aceite.

Critério verificável: a parte do CA-07 "com reserva ou retirada" não pode ser verificada nesta task, porque essas tabelas ainda não existem e o TC-13 não roda no ciclo 5. É preciso indicar qual task fará esse teste ou tirar a exclusão do escopo. A P-03 também não considera material com período de manutenção.

Conflitos: a restrição `UNIQUE (tipo, nome)` da arquitetura (seção 3.2) impede dois materiais com o mesmo nome e patrimônios diferentes, e a spec não prevê esse caso. Além disso, T-16, T-17 e T-18 colocam a migração de `recurso` no próprio escopo, então é preciso definir qual task cria a tabela.

Testes e regras: P-01 define a unicidade do patrimônio sem diferenciar maiúsculas, mas é necessário explicitar como essa regra também é garantida no banco. P-02 estabelece limite de 100 caracteres para nome e tipo, porém TC-04 cobre apenas o nome. O comportamento do campo `restrito` quando omitido no PUT não está definido. No mini-plano, TC-08 e TC-09 estão no ciclo 3, mas dependem do PUT, que só é implementado no ciclo 4.

**Parecer:** Ajustes necessários · @Samwel-Merces · 2026-11-05

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
