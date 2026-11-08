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
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

O material (projetor, notebook, equipamentos) é o terceiro tipo de recurso (RF-02) e a base da retirada e da devolução (RF-09). Esta task cria o cadastro do material com tipo e número de patrimônio. O Administrador escreve; qualquer usuário autenticado consulta.

## 2. Escopo

**Dentro:**
- Cadastrar, alterar, consultar (por id e em lista) e excluir material, com nome, tipo, patrimônio e o indicador `restrito`.
- Migração Flyway da tabela `material` (`recurso_id` PK e FK, `tipo_material`, `patrimonio`) e do índice único sobre `lower(patrimonio)`. A tabela `recurso`, com todas as restrições que a tocam (nome `varchar(100)`, `CHECK` de tipo, índice único `(tipo, lower(nome))` e `CHECK`s de padrão de nome condicionados a sala e professor), é criada pela T-16. Esta task **não altera `recurso`**.
- Validação de entrada e `409` para patrimônio ou nome repetidos.
- Escrita só do Administrador; `401` e `403` conforme a T-15.

**Fora (e por quê):**
- A **situação** do material pedida no backlog: ela se choca com o período de manutenção (RN-04) e com a retirada (RF-09), que já definem se o material está disponível. Ver a premissa P-05.
- O teste da recusa de exclusão de material **com** reserva, retirada ou manutenção: essas tabelas ainda não existem. A regra é garantida pelas chaves estrangeiras `ON DELETE RESTRICT` (arquitetura 3.2, restrição 5, que também cobre `PERIODO_MANUTENCAO`), e a T-22 traduz a violação (SQLState `23503`) para `409`. O teste é a task **T-61** do backlog.
- Retirada, devolução (RF-09) e manutenção (RN-04): são de outras tasks.
- Pesquisa com filtros (RF-03).
- O que o indicador `restrito` provoca (RN-07): aqui é só um dado.
- O formato das respostas de erro (T-22) e a autenticação (T-15).

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** Cadastro e alteração só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **Modelo (arquitetura 3.1):** o material é um `recurso` do tipo `MATERIAL`, com `tipo_material`. O modelo da arquitetura recebe a coluna `patrimonio`, que esta spec define (P-01).

**Decisões do time registradas nesta spec** (as regras novas entram no PRD):

- **P-01:** o material tem `patrimonio`, obrigatório, com até 30 caracteres, único entre os materiais (sem diferenciar maiúsculas), sem espaços nas pontas e com os espaços repetidos no meio reduzidos a um só (P-07). No banco, a unicidade é um índice único sobre `lower(patrimonio)` na tabela `material`, criado por esta task.
- **P-02:** o nome e o tipo têm até 100 caracteres.
- **P-06:** o nome é único entre os materiais, sem diferenciar maiúsculas de minúsculas, como manda a restrição 8 da arquitetura (3.2). Ela é um **índice único** sobre `(tipo, lower(nome))` em `recurso` (o PostgreSQL não aceita `lower()` em restrição de tabela), criado pela T-16 e não por esta task. Dois materiais iguais precisam de nomes diferentes, como "Projetor Epson 01" e "Projetor Epson 02".
- **P-07:** o nome, o tipo e o patrimônio perdem os espaços das pontas e têm os espaços repetidos no meio reduzidos a um só antes de validar e gravar (`"  Projetor   Epson "` vira `"Projetor Epson"`). A unicidade e os limites de tamanho valem sobre o texto já limpo, e o banco guarda o texto limpo. O nome continua texto livre (sem padrão fixo).
- **P-03:** a exclusão só é permitida para material sem nenhuma reserva, retirada nem período de manutenção; com qualquer um deles, é recusada. A garantia vem das chaves estrangeiras `ON DELETE RESTRICT`, inclusive a de `PERIODO_MANUTENCAO`. A violação da chave estrangeira (SQLState `23503`) é traduzida pela T-22 para `409` "Este registro está em uso e não pode ser removido".
- **P-04:** `restrito` é opcional no cadastro (`POST`) e vale `false` quando não informado. No `PUT` ele é **obrigatório**, para a alteração nunca retirar a exigência de aprovação sem pedido explícito: ausente, o pedido é recusado com `400` e `erros` no campo `restrito`.
- **P-05:** a situação do material não é um campo guardado: a disponibilidade vem da manutenção, da reserva e da retirada. Esta é uma decisão do time, registrada aqui: se o time decidir guardar a situação, a spec muda.

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/materiais`: cadastra (ADMINISTRADOR).

```json
{ "nome": "Projetor Epson", "tipoMaterial": "Projetor", "patrimonio": "PAT-000123", "restrito": true }
```

`PUT /api/materiais/{id}`: altera, com o mesmo corpo, mas com `restrito` obrigatório (ADMINISTRADOR). `DELETE /api/materiais/{id}`: exclui (ADMINISTRADOR). `GET /api/materiais/{id}` e `GET /api/materiais`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | O material, com `id` |
| Alteração ou consulta | 200 | O material (ou a lista) |
| Exclusão | 204 | Sem corpo |
| Campo ausente ou inválido, inclusive `restrito` ausente no `PUT` | 400 | `ProblemDetail`, `erros` com campo e motivo |
| Material não encontrado | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Patrimônio ou nome repetidos, pegos pela verificação do serviço | 409 | `ProblemDetail` em linguagem de negócio que diz qual campo repetiu: o nome, o patrimônio ou os dois |
| Patrimônio ou nome repetidos, barrados pelo banco numa corrida entre pedidos | 409 | Mensagem fixa da T-22: "Esta operação conflita com outro registro" |
| Exclusão de material com reserva, retirada ou manutenção | 409 | Mensagem fixa da T-22: "Este registro está em uso e não pode ser removido" (teste na T-61) |
| Sem autenticação ou sem permissão | 401 e 403 | `ProblemDetail` (T-15) |

**Ordem de verificação dos erros:** 401, depois 403 por perfil, depois 404, depois 400 e por último 409. Assim, um Solicitante que envia um `PUT` inválido para um id inexistente recebe `403`.

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

### CA-03: Patrimônio ou nome repetido é recusado

- **Dado** um material cadastrado
- **Quando** o cliente cadastra outro com o mesmo patrimônio ou o mesmo nome (ainda que com maiúsculas ou espaços diferentes, P-06 e P-07)
- **Então** o pedido é recusado com `409` em linguagem de negócio
- **E** quando a verificação do serviço pega o caso, a mensagem diz qual campo já existe: o nome, o patrimônio ou os dois, se os dois se repetem
- **E** se a recusa vier do banco numa corrida entre pedidos, a mensagem é a genérica da T-22 ("Esta operação conflita com outro registro")

### CA-04: Administrador altera um material

- **Dado** um material cadastrado
- **Quando** o Administrador o altera com dados válidos
- **Então** a resposta é `200` com o material atualizado
- **E** as validações do cadastro valem na alteração
- **E** um `PUT` sem `restrito` é recusado com `400` e `erros` no campo `restrito` (P-04)
- **E** alterar o próprio nome ou o próprio patrimônio mudando só maiúsculas ou espaços é aceito, sem `409`

### CA-05: Qualquer usuário autenticado consulta

- **Dado** materiais cadastrados
- **Quando** um usuário de qualquer perfil consulta um material por id ou a lista
- **Então** a resposta é `200`

### CA-06: Material inexistente dá 404

- **Dado** um id que não corresponde a nenhum material
- **Quando** o cliente consulta, altera ou exclui
- **Então** a resposta é `404` com "Recurso não encontrado"

### CA-07: Exclusão de material sem vínculos

- **Dado** um material sem reserva, retirada nem período de manutenção
- **Quando** o Administrador o exclui
- **Então** a resposta é `204` e o material deixa de existir
- **E** as linhas de `material` e de `recurso` saem, e o mesmo nome e o mesmo patrimônio podem ser cadastrados de novo

**Observação (fora do critério):** a recusa com `409` para material com reserva, retirada ou manutenção é garantida pelas chaves estrangeiras e traduzida pela T-22 (SQLState `23503`). Ela não pode ser verificada nesta task, porque essas tabelas ainda não existem. A pendência é a task **T-61** do backlog.

### CA-08: Só o Administrador escreve

- **Dado** as operações de cadastro, alteração e exclusão
- **Quando** um SOLICITANTE ou um RESPONSAVEL as tenta, ou alguém sem autenticação
- **Então** quem está autenticado sem permissão recebe `403`, e quem não está autenticado recebe `401`
- **E** nada é gravado

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada
- **Quando** um material é gravado
- **Então** existe um registro em `recurso` com tipo `MATERIAL` e outro em `material` ligado a ele
- **E** o patrimônio é único também no banco, sem diferenciar maiúsculas (índice único criado por esta task)
- **E** o nome é único por tipo de recurso no banco, sem diferenciar maiúsculas (índice único criado pela T-16)

## 6. Casos de teste

Só a regra pura, sem HTTP (limpeza de texto e limites de tamanho), é teste unitário. Todo caso que confere status, corpo, valor gravado ou permissão é de integração, com o banco de teste (Testcontainers). A exclusão de material com reserva, retirada ou manutenção não é testada aqui (task T-61).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome, tipo e patrimônio | 201; `restrito = false` | Integração |
| TC-02 | CA-02 | Classe inválida | Nome, tipo ou patrimônio ausentes ou só com espaços | 400; cada campo aparece em `erros` | Integração |
| TC-03 | CA-02 | Valor limite | Patrimônio com 30 caracteres / com 31 | 30 aceita; 31 recusa (400) | Integração |
| TC-04 | CA-02 | Valor limite | Nome com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Integração |
| TC-05 | CA-02 | Classe válida | Patrimônio com espaços nas pontas | Aceito, gravado sem os espaços | Integração |
| TC-06 | CA-03 | Classe inválida | Cadastrar "PAT-1" depois de "pat-1" | 409; a mensagem cita o patrimônio | Integração |
| TC-07 | CA-04 | Classe válida | Alterar o nome e o tipo de um material | 200 com os dados novos | Integração |
| TC-08 | CA-04 | Classe válida | Alterar mantendo o próprio patrimônio | 200 (não conflita consigo mesmo) | Integração |
| TC-09 | CA-04 | Classe inválida | Alterar o patrimônio para o de outro material | 409 | Integração |
| TC-10 | CA-05 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três | Integração |
| TC-11 | CA-06 | Classe inválida | `GET`, `PUT` e `DELETE` com id inexistente | 404 | Integração |
| TC-12 | CA-07 | Classe válida | Excluir material sem reserva, retirada nem manutenção | 204; as linhas de `material` e `recurso` saíram; o mesmo nome e patrimônio podem ser cadastrados de novo (201) | Integração |
| TC-14 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST`, `PUT` e `DELETE` | 403; nada é gravado | Integração |
| TC-15 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT`, `DELETE` e `GET` | 401 | Integração |
| TC-16 | CA-09 | Classe válida | Gravar um material | 1 linha em `recurso` (tipo `MATERIAL`) e 1 em `material` | Integração |
| TC-17 | CA-09 | Conflito | `INSERT` direto no banco com patrimônio "PAT-1" depois de "pat-1" | Recusado pelo banco (índice único sobre `lower(patrimonio)`) | Integração |
| TC-18 | CA-02 | Valor limite | Tipo com 100 caracteres / com 101 | 100 aceita; 101 recusa (400) | Integração |
| TC-19 | CA-04 | Classe inválida | `PUT` sem o campo `restrito`, em um material que era `restrito = true` | 400, `erros` com `campo = restrito`; o material não muda | Integração |
| TC-20 | CA-03 | Classe inválida | Cadastrar outro material com o mesmo nome e outro patrimônio | 409; a mensagem cita o nome | Integração |
| TC-21 | CA-04 | Classe inválida | Alterar o nome para o de outro material | 409 | Integração |
| TC-22 | CA-09 | Conflito | `INSERT` direto no banco com o mesmo tipo e nome, em maiúsculas diferentes | Recusado pelo banco (índice único `(tipo, lower(nome))`, criado pela T-16) | Integração |
| TC-23 | CA-01 | Classe válida | Cadastro com nome `"  Projetor   Epson "` | 201; o nome gravado é `"Projetor Epson"` | Integração |
| TC-24 | CA-03 | Classe inválida | Cadastrar `"projetor  epson"` (dois espaços, minúsculas) depois de `"Projetor Epson"` | 409; a mensagem cita o nome | Integração |
| TC-25 | CA-02 | Valor limite | Nome de 100 caracteres mais espaços nas pontas / 101 caracteres depois da limpeza | O primeiro é aceito (conta 100); o segundo é recusado (400) | Integração |
| TC-26 | CA-02 | Classe válida | Patrimônio `"PAT   123"` (espaços repetidos no meio) | Aceito, gravado como `"PAT 123"` | Integração |
| TC-27 | CA-02 | Classe válida | Regra de limpeza de texto: `"  Projetor   Epson "`, `"PAT   123"` e `"a"` | `"Projetor Epson"`, `"PAT 123"` e `"a"` | Unitário |
| TC-28 | CA-02 | Valor limite | Regra de tamanho: nome e tipo com 100 / 101; patrimônio com 30 / 31 | 100 e 30 aceitam; 101 e 31 recusam | Unitário |
| TC-29 | CA-04 | Classe válida | Alterar o próprio nome de `"Projetor Epson"` para `"projetor epson"` | 200; não conflita consigo mesmo | Integração |
| TC-30 | CA-04 | Classe válida | Alterar o próprio patrimônio de `"PAT-1"` para `" pat-1 "` | 200; gravado `"pat-1"`; não conflita consigo mesmo | Integração |
| TC-31 | CA-03 | Classe inválida | Cadastrar material com nome e patrimônio ambos já existentes | 409; a mensagem cita o nome e o patrimônio | Integração |
| TC-32 | CA-03 | Conflito | Violação do índice único simulada depois da verificação do serviço (corrida) | 409 com a mensagem genérica "Esta operação conflita com outro registro" | Integração |
| TC-33 | CA-08 | Tabela de decisão | SOLICITANTE faz `PUT` com corpo inválido em um id inexistente | 403 (o perfil vem antes do 404 e do 400) | Integração |
| TC-34 | CA-06 | Tabela de decisão | ADMINISTRADOR faz `PUT` com corpo inválido em um id inexistente | 404 (o 404 vem antes do 400) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A permissão do Administrador é esquecida em `PUT` ou `DELETE` | Alto | TC-14 e TC-15 cobrem as três operações de escrita |
| Patrimônio duplicado entra por outro caminho | Alto | TC-06 e TC-09 na regra e TC-17 no banco |
| O modelo da arquitetura não tinha a coluna de patrimônio | Baixo | A arquitetura já recebe a coluna `patrimonio` e o índice único (PR da arquitetura) |
| O backlog pede a situação do material e a spec não a guarda | Médio | Decisão explícita em P-05 |
| Excluir material deixa reserva, retirada ou manutenção órfã | Alto | P-03 e as chaves estrangeiras `ON DELETE RESTRICT` (inclusive `PERIODO_MANUTENCAO`); a T-22 traduz o SQLState `23503` para 409; o teste da recusa é a task T-61 |
| Sem a tradução do SQLState `23503`, a exclusão de material em uso devolveria 500 | Alto | Decisão G1: a T-22 traduz para 409; o teste é a T-61 |
| Nome repetido, com maiúsculas ou espaços diferentes, de dois materiais iguais | Médio | P-06 e P-07, TC-20 a TC-24: o índice único `(tipo, lower(nome))` é criado pela T-16 e o nome é limpo antes de gravar; materiais iguais precisam de nomes diferentes |
| Nomes parecidos, como "Projetor Epson" e "Projetor Epsom", são aceitos como materiais diferentes | Baixo | Aceito: o sistema compara só o texto limpo, não a semelhança. O Administrador corrige pela alteração (`PUT`) |
| A T-16, a T-17 e a T-18 criariam a mesma tabela `recurso` | Médio | Só a T-16 a cria, completa e com todas as restrições; esta task só cria `material` e o índice de patrimônio. No backlog, a T-18 depende da T-15 e da T-16 |
| `restrito` obrigatório no `PUT` pode surpreender quem omite o campo | Baixo | Decisão P-04: ausente dá 400 (TC-19); assim a alteração nunca retira a exigência de aprovação sem pedido explícito |
| A mensagem da corrida entre pedidos não diz qual campo repetiu | Baixo | Aceito (decisão G2): só a verificação do serviço cita o campo; a corrida recebe a mensagem genérica da T-22 (TC-32) |
| As decisões P-01 a P-07 são do time e as regras novas entram no PRD | Baixo | Mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01 e TC-16: cadastro válido grava nas duas tabelas | Entidade, migração de `material` (ligada a `recurso`), repositório e serviço de material | Tratar `recurso` e `material` como um agregado |
| 2 | TC-02 a TC-05, TC-18, TC-23, TC-25 a TC-28: campos obrigatórios, tamanhos e limpeza de espaços | Limpeza de espaços (P-07) e validação de obrigatórios e de tamanhos de nome, tipo e patrimônio | Extrair a validação para o objeto de entrada |
| 3 | TC-06, TC-20, TC-24 e TC-31: patrimônio e nome únicos | Verificação de patrimônio e de nome repetidos no cadastro, com a mensagem por campo | Mover a regra para o serviço |
| 4 | TC-07 a TC-11, TC-19, TC-21, TC-29, TC-30 e TC-34: alteração, consulta e 404 | Alteração (com `restrito` obrigatório no `PUT`), consultas, `404` e unicidade na alteração sem conflitar consigo mesmo | Reaproveitar a validação do cadastro |
| 5 | TC-12: exclusão | Exclusão de material sem vínculos e novo cadastro com o mesmo nome | Mover a regra para o serviço |
| 6 | TC-14, TC-15 e TC-33: permissão e ordem dos erros | Regra de perfil por operação (T-15) antes do 404 e do 400 | Declarar o perfil exigido em um só lugar |
| 7 | TC-17: índice único de patrimônio no banco | Índice único sobre `lower(patrimonio)` na migração de `material` | Revisar a migração |

Os testes TC-22 (índice de nome criado pela T-16) e TC-32 (tradução da corrida pela T-22) não têm red nesta task: eles já passam quando a T-16 e a T-22 estão prontas, e ficam como proteção contra regressão.
## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.
**Comentários:**

Reli a versão atual e os pontos das revisões anteriores foram tratados: a recusa da exclusão com vínculos saiu do CA-07 e foi para a T-61, a T-22 traduz a chave estrangeira para 409, o índice de nome ficou com a T-16, `restrito` é obrigatório no `PUT`, e os níveis de teste e os ciclos estão coerentes. A spec está pronta para seguir.

A aprovação depende do merge do PR #34 e da T-16 antes deste PR, porque a T-18 usa a tabela `recurso`, o índice de nome e a T-61 definidos neles.

Pontos para acompanhar na implementação, sem bloquear a aprovação:

- TC-11 deve incluir um id que existe em `recurso`, mas é de sala ou professor. `GET`, `PUT` e `DELETE` em `/api/materiais/{id}` devem responder 404 e não alterar o outro recurso.
- O backlog ainda pede "situação" na T-18; vale ajustar esse texto no PR #34.
- A spec diz que as regras novas entram no PRD, mas o PR #34 não traz regra de patrimônio nem de nome de material. Vale incluir uma RN ou tratá-las como premissas da spec.
- A ordem 404 antes de 400 (TC-34) exige cuidado, porque a leitura do JSON e o `@Valid` normalmente rodam antes de buscar o material.
- `restrito` deve ser `Boolean`, para `null` no `PUT` não virar `false`. Vale incluir esse caso.
- A verificação de nome e patrimônio repetidos deve usar `lower()` no banco, para seguir a mesma regra dos índices.
- Falta um caso de integração para a limpeza de espaços do tipo.

**Parecer:** Aprovada · @Samwel-Merces · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-07
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
