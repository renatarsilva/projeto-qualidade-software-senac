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
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

A sala é o recurso principal das reservas (RF-02). Esta task cria o cadastro e a consulta de salas: o Administrador registra e altera as salas, e qualquer usuário autenticado as consulta. As validações de entrada (capacidade maior que zero, campos obrigatórios) valem aqui porque uma sala inválida contaminaria todas as reservas. Os erros usam o formato da T-22 e a permissão usa a T-15.

## 2. Escopo

**Dentro:**
- Cadastrar, alterar e consultar sala (por id e em lista), com nome, tipo da sala, capacidade, localização e o indicador `restrito`.
- Migração Flyway das tabelas `recurso` e `sala`, conforme o modelo da arquitetura (D-02). **Esta task cria a tabela `recurso` completa**, com todas as restrições da arquitetura (3.2) que tocam `recurso` (P-07). A T-17 (professores) e a T-18 (materiais) só criam as suas tabelas, **não alteram `recurso`** e dependem desta migração e da T-15.
- Leitura do JSON sem coerção de tipos: decimal não vira inteiro e texto não vira número (P-04). A tradução do erro de leitura para `400` é da T-22.
- Validação de entrada, `409` para nome repetido e a ordem dos erros (P-08).
- Cadastro e alteração só do Administrador; `401` e `403` conforme a T-15.

**Fora (e por quê):**
- Pesquisa com filtros por tipo, capacidade, localização e disponibilidade (RF-03): é de outra task.
- Exclusão de sala: o backlog não a pede, e uma sala pode ter reservas e histórico. Ver a premissa P-02.
- Manutenção da sala (RN-04) e reserva (T-20, T-23).
- O que o indicador `restrito` provoca (aprovação do Responsável, RN-07): aqui é só um dado.
- O formato das respostas de erro, a tradução do erro de leitura do JSON e a do `409` vindo do banco (T-22), e a autenticação (T-15).

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** Cadastro e alteração só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **Modelo (arquitetura 3.1):** a sala é um `recurso` do tipo `SALA`, com `nome` e `restrito`, mais `capacidade`, `localizacao` e `tipo_sala`.
- **S-03:** todo recurso tem o indicador booleano `restrito`.
- **RN-16 (PRD, regra do time):** o nome da sala segue o padrão `sala-NN` (P-01). As regras do time RN-14 a RN-17 estão registradas no PRD; a RN-16 é a que esta spec aplica.

**Regras do time desta spec** (o PRD só tem a RN-16; as demais são premissas da spec, definidas pela equipe):

- **P-01 (RN-16):** o nome de **toda** sala segue exatamente o padrão `sala-` seguido de 2 ou 3 dígitos (`sala-01`, `sala-99`, `sala-102`), tudo em minúsculas, sem espaços e sem nenhum outro caractere. O tipo "Laboratório" é só o `tipoSala`: o nome continua `sala-NN`, e não existe nome como `lab-01`. Qualquer nome fora do padrão (espaço na frente ou atrás, `Sala-01`, `SALA-01`, `sala 01`, `saal-01`, `sala-1`, `sala-01x`) é recusado com `400`, e nada é corrigido automaticamente. `sala-01` e `sala-001` são nomes diferentes e convivem. O nome é único entre as salas. No banco, o padrão é um `CHECK` válido só para o tipo `SALA` e a unicidade é um índice único sobre `(tipo, lower(nome))` (P-07). O tipo da sala e a localização têm até 100 caracteres.
- **P-02:** não existe exclusão de sala nesta task. Quando houver, só poderá ocorrer para sala sem nenhuma reserva.
- **P-03:** `restrito` é opcional no cadastro e vale `false` quando não informado. No `PUT` ele é **obrigatório**: se vier ausente, a resposta é `400` com `erros` no campo `restrito`, para uma alteração nunca tirar a exigência de aprovação sem pedido explícito.
- **P-04:** a capacidade é um número inteiro maior que zero até 2.147.483.647 (o inteiro do banco). A leitura do JSON **não converte tipos**: `30.5`, `"30"` (texto), `"abc"` e `2147483648` são recusados com `400`, `erros` no campo `capacidade` e motivo "valor inválido". Capacidade 0 ou negativa dá `400` com o motivo "deve ser maior que zero". A configuração do leitor de JSON desliga a conversão de decimal para inteiro e de texto para número.
- **P-05:** o tipo da sala e a localização perdem os espaços das pontas e têm os espaços repetidos no meio reduzidos a um só antes de validar e gravar. Um valor que fica vazio depois da limpeza (só espaços) é tratado como em branco. O nome **não** passa por limpeza: um espaço no nome é erro (P-01).
- **P-06:** `recurso.nome` é `varchar(100)`. O tipo da sala e a localização também têm até 100 caracteres, contados depois da limpeza (P-05).
- **P-07:** a migração desta task cria `recurso` com todas as restrições da arquitetura 3.2 que tocam essa tabela: `nome varchar(100)`; `CHECK` de tipo (`SALA`, `PROFESSOR` ou `MATERIAL`); índice único `CREATE UNIQUE INDEX ... ON recurso (tipo, lower(nome))` (não é restrição de tabela); `CHECK (tipo <> 'SALA' OR nome ~ '^sala-[0-9]{2,3}$')`; e `CHECK (tipo <> 'PROFESSOR' OR nome ~ '^[a-z]+-[a-z]+-[0-9]{5}$')`. Os dois `CHECK` são condicionados ao tipo porque a coluna `nome` é compartilhada por salas, professores e materiais.
- **P-08:** ordem dos erros no cadastro e na alteração: `401` (sem autenticação), `403` por perfil, `404` (sala não existe, só no `PUT` e no `GET`), `400` (validação) e `409` (regra ou banco). O `409` de nome repetido pega o caso na verificação do serviço; quando dois cadastros simultâneos passam pela verificação e o índice único barra um deles, a resposta é o `409` genérico da T-22.

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`).

`POST /api/salas`: cadastra uma sala (ADMINISTRADOR). `restrito` é opcional.

```json
{ "nome": "sala-102", "tipoSala": "Laboratório", "capacidade": 30, "localizacao": "Bloco B, 1º andar", "restrito": false }
```

`PUT /api/salas/{id}`: altera uma sala, com o mesmo corpo (ADMINISTRADOR). `restrito` é obrigatório (P-03).

`GET /api/salas/{id}` e `GET /api/salas`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | A sala, com `id` |
| Alteração ou consulta | 200 | A sala (ou a lista) |
| Campo ausente, fora do padrão ou inválido | 400 | `ProblemDetail`, `erros` com campo e motivo ("deve seguir o padrão sala-NN", "deve ser maior que zero" ou "valor inválido") |
| Sala não encontrada | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Nome já cadastrado (verificação do serviço) | 409 | `ProblemDetail` em linguagem de negócio, por exemplo "Já existe uma sala com esse nome" |
| Cadastros simultâneos do mesmo nome, barrados pelo banco | 409 | `ProblemDetail` com a mensagem fixa da T-22: "Esta operação conflita com outro registro" |
| Sem autenticação | 401 | `ProblemDetail` (T-15) |
| Perfil sem permissão | 403 | `ProblemDetail` (T-15) |

## 5. Critérios de aceite

### CA-01: Administrador cadastra uma sala válida

- **Dado** um Administrador autenticado
- **Quando** ele cadastra uma sala com nome, tipo, capacidade maior que zero e localização
- **Então** a resposta é `201` com a sala e o seu `id`
- **E** `restrito` vale `false` quando não é informado
- **E** a sala fica gravada e pode ser consultada

### CA-02: Capacidade inválida é recusada, com o motivo certo

- **Dado** o cadastro ou a alteração de sala
- **Quando** o cliente informa capacidade 0 ou negativa, ou decimal, texto ou acima de 2.147.483.647
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `capacidade` com o motivo "deve ser maior que zero" para 0 e negativos, e "valor inválido" para decimal, texto e valor acima do inteiro
- **E** a leitura do JSON não converte `30.5` em `30` nem `"30"` em número (P-04)

### CA-03: Campos obrigatórios ausentes, só com espaços ou grandes demais são recusados

- **Dado** o cadastro ou a alteração de sala
- **Quando** o cliente deixa em branco ou só com espaços o nome, o tipo, a capacidade ou a localização, ou passa de 100 caracteres no tipo ou na localização (depois da limpeza)
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta cada campo inválido

### CA-04: Nome repetido é recusado

- **Dado** uma sala já cadastrada
- **Quando** o cliente cadastra outra sala com o mesmo nome, ou altera o nome de uma sala para o de outra
- **Então** o pedido é recusado com `409` em linguagem de negócio
- **E** se dois cadastros simultâneos passam pela verificação e o índice único do banco barra um deles, a resposta também é `409`, com a mensagem fixa da T-22, e não `500`

### CA-05: Administrador altera uma sala

- **Dado** uma sala cadastrada
- **Quando** o Administrador a altera com dados válidos
- **Então** a resposta é `200` com a sala atualizada
- **E** as mesmas validações do cadastro valem na alteração
- **E** um `PUT` sem `restrito` é recusado com `400`, `erros` no campo `restrito`, e a sala não muda (P-03)

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
- **E** o `403` vem antes do `404` e do `400` (P-08)

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada
- **Quando** uma sala é gravada
- **Então** existe um registro em `recurso` com tipo `SALA` e outro em `sala` ligado a ele
- **E** a capacidade só aceita valores maiores que zero também no banco
- **E** o nome da sala só aceita o padrão `sala-NN` (`CHECK` condicionado ao tipo `SALA`)
- **E** o nome é único por tipo de recurso no banco, pelo índice único sobre `(tipo, lower(nome))` (P-07)

### CA-10: O nome segue o padrão sala-NN

- **Dado** o cadastro ou a alteração de sala
- **Quando** o nome não é exatamente `sala-` seguido de 2 ou 3 dígitos, em minúsculas: com espaço na frente, atrás ou no meio, com maiúscula, com erro na palavra `sala`, com 1 ou 4 dígitos, ou com qualquer outro caractere
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `nome` com o motivo "deve seguir o padrão sala-NN"
- **E** um nome no padrão é aceito (`sala-01`, `sala-99`, `sala-102`, `sala-999`), e `sala-01` convive com `sala-001`
- **E** vale para toda sala, de qualquer `tipoSala`, inclusive "Laboratório"

### CA-11: Tipo e localização perdem os espaços extras

- **Dado** o cadastro ou a alteração de sala
- **Quando** o tipo da sala ou a localização trazem espaços nas pontas ou espaços repetidos no meio
- **Então** a sala é gravada com o tipo e a localização sem os espaços extras (P-05)
- **E** o limite de 100 caracteres conta o texto já limpo

### CA-12: O padrão do nome da sala não atinge professor nem material

- **Dado** a tabela `recurso`, compartilhada por salas, professores e materiais
- **Quando** um professor ou um material é gravado com um nome que não segue o padrão de sala
- **Então** o `CHECK` do padrão `sala-NN` não o recusa, porque só vale para o tipo `SALA`
- **E** o nome de qualquer recurso aceita até 100 caracteres e recusa 101 (`varchar(100)`, P-06)

## 6. Casos de teste

Teste unitário cobre só a regra pura, sem HTTP. Teste que confere status ou corpo da resposta, permissão ou banco é de integração, com o banco de teste (Testcontainers).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome, tipo, capacidade 30 e localização, sem `restrito` | 201; `restrito = false` | Integração |
| TC-02 | CA-02 | Valor limite | Capacidade = 1 | A regra aceita | Unitário |
| TC-03 | CA-02 | Valor limite | Capacidade = 0 | A regra recusa; erro no campo `capacidade`, motivo "deve ser maior que zero" | Unitário |
| TC-04 | CA-02 | Classe inválida | Capacidade = -1 | A regra recusa; mesmo campo e motivo do TC-03 | Unitário |
| TC-05 | CA-02 | Classe inválida | `POST` com capacidade `"abc"` (texto que não é número) | 400; `erros` com `campo = capacidade`, motivo "valor inválido" | Integração |
| TC-06 | CA-03 | Classe inválida | Nome ausente | A regra recusa; erro no campo `nome` | Unitário |
| TC-07 | CA-03 | Classe inválida | Nome só com espaços | A regra recusa; erro no campo `nome` | Unitário |
| TC-08 | CA-03 | Classe inválida | Três pedidos, um por campo ausente: sem `tipoSala`; sem `capacidade`; sem `localizacao`. E um quarto pedido sem os três | 400 em cada um; `erros` aponta só o campo ausente nos três primeiros e os três campos no quarto | Integração |
| TC-09 | CA-10 | Valor limite | Nome `sala-01` (2 dígitos) / `sala-1` (1 dígito) | A regra aceita `sala-01` e recusa `sala-1` | Unitário |
| TC-10 | CA-04 | Classe inválida | Cadastrar `sala-01` depois de `sala-01` | 409 "Já existe uma sala com esse nome"; só uma sala gravada | Integração |
| TC-11 | CA-05 | Classe válida | Alterar o nome e a capacidade de uma sala (com `restrito`) | 200 com os dados novos | Integração |
| TC-12 | CA-05 | Classe inválida | Alterar a capacidade para 0 | 400; a sala não muda | Integração |
| TC-13 | CA-05 | Classe inválida | Alterar mantendo o próprio nome | 200 (não conflita consigo mesma) | Integração |
| TC-14 | CA-06 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três | Integração |
| TC-15 | CA-07 | Classe inválida | `GET` e `PUT` com id inexistente | 404 | Integração |
| TC-16 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST` e `PUT` | 403; nada é gravado | Integração |
| TC-17 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT` e `GET` | 401 | Integração |
| TC-18 | CA-09 | Classe válida | Gravar uma sala | Existe 1 linha em `recurso` (tipo `SALA`) e 1 em `sala` | Integração |
| TC-19 | CA-09 | Valor limite | `INSERT` direto no banco com capacidade 0 | Recusado pelo banco (`CHECK`) | Integração |
| TC-20 | CA-02 | Classe inválida | `POST` com capacidade `30.5` (decimal) | 400; `campo = capacidade`, motivo "valor inválido"; a sala não é gravada com 30 | Integração |
| TC-21 | CA-02 | Valor limite | `POST` com capacidade 2147483647 / 2147483648 | 2147483647 aceita (201); 2147483648 recusa (400, "valor inválido") | Integração |
| TC-22 | CA-03 | Valor limite | Tipo e localização com 100 caracteres / com 101 | A regra aceita 100 e recusa 101, em cada campo | Unitário |
| TC-23 | CA-04 | Classe inválida | Alterar o nome de uma sala para o de outra | 409; a sala não muda | Integração |
| TC-24 | CA-05 | Classe inválida | `PUT` sem o campo `restrito`, em uma sala que era `restrito = true` | 400, `erros` com `campo = restrito`; a sala continua `restrito = true` | Integração |
| TC-25 | CA-09 | Conflito | `INSERT` direto no banco, tipo `SALA`, com o nome `Sala-01` | Recusado pelo banco (`CHECK` do padrão) | Integração |
| TC-26 | CA-09 | Conflito | `INSERT` direto no banco com o mesmo tipo e o mesmo nome de outra sala | Recusado pelo índice único `(tipo, lower(nome))` | Integração |
| TC-27 | CA-10 | Classe inválida | Nomes ` sala-01`, `sala-01 `, `Sala-01`, `SALA-01`, `sala 01`, `saal-01`, `sala-01x`, `sala_01` (teste parametrizado) | A regra recusa todos; erro no campo `nome`, motivo "deve seguir o padrão sala-NN" | Unitário |
| TC-28 | CA-10 | Valor limite | Nome `sala-999` (3 dígitos) / `sala-1000` (4 dígitos) | A regra aceita `sala-999` e recusa `sala-1000` | Unitário |
| TC-29 | CA-11 | Classe válida | Cadastro com tipo `"  Laboratório "` e localização com espaços repetidos | 201; tipo e localização gravados sem os espaços extras (P-05) | Integração |
| TC-30 | CA-02 | Classe inválida | `POST` com capacidade 0 e outro com -1 | 400; `erros` com `campo = capacidade`, motivo "deve ser maior que zero" | Integração |
| TC-31 | CA-03 | Classe inválida | `POST` sem `nome` e outro com `nome` só com espaços | 400; `erros` com `campo = nome` | Integração |
| TC-32 | CA-10 | Classe inválida | `POST` com o nome `Sala-01` | 400; `erros` com `campo = nome`, motivo "deve seguir o padrão sala-NN"; nada gravado | Integração |
| TC-33 | CA-03 | Valor limite | `POST` com tipo de 101 caracteres e outro com localização de 101 | 400; `erros` aponta `tipoSala` e `localizacao`, respectivamente | Integração |
| TC-34 | CA-02 | Classe inválida | `POST` com capacidade `"30"` (texto entre aspas) | 400; `campo = capacidade`, motivo "valor inválido"; o texto não vira número | Integração |
| TC-35 | CA-10 | Classe válida | Cadastrar `sala-01` e depois `sala-001` | 201 nos dois (nomes diferentes convivem) | Integração |
| TC-36 | CA-11 | Valor limite | Tipo de 100 caracteres com espaços nas pontas / de 101 caracteres depois da limpeza | A regra aceita o primeiro (conta 100 depois da limpeza) e recusa o segundo | Unitário |
| TC-37 | CA-03 | Classe inválida | `POST` com tipo só com espaços e outro com localização só com espaços | 400; `erros` aponta `tipoSala` e `localizacao`, respectivamente | Integração |
| TC-38 | CA-04 | Conflito | Dois cadastros simultâneos de `sala-05`, com a verificação do serviço forçada a passar nos dois | Um 201 e um 409 com a mensagem fixa "Esta operação conflita com outro registro"; nunca 500; uma sala gravada | Integração |
| TC-39 | CA-12 | Classe válida | `INSERT` direto no banco: `MATERIAL` com nome `Projetor Epson` e `PROFESSOR` com nome `ana-souza-12345` | Aceitos (o `CHECK` do padrão de sala só vale para `SALA`) | Integração |
| TC-40 | CA-12 | Valor limite | `INSERT` direto no banco: `MATERIAL` com nome de 100 caracteres / de 101 | 100 aceito; 101 recusado (`varchar(100)`) | Integração |
| TC-41 | CA-08 | Tabela de decisão | SOLICITANTE envia `POST` com corpo inválido; ADMINISTRADOR envia `PUT` com id inexistente e corpo inválido | 403 no primeiro; 404 no segundo (a ordem dos erros, P-08) | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Capacidade 0 ou negativa entra no banco por outro caminho | Alto | TC-03 e TC-04 na regra, TC-30 no contrato e TC-19 no banco (`CHECK`) |
| Erro de um na fronteira da capacidade (`>=` no lugar de `>`) | Alto | TC-02 e TC-03 testam 1 e 0 |
| A permissão do Administrador é esquecida em `PUT` | Alto | TC-16 e TC-17 cobrem `POST` e `PUT` por perfil; TC-41 cobre a ordem dos erros |
| Nome fora do padrão, com espaço ou parecido com outro ("Saal 102", "Sala 102") | Médio | O padrão `sala-NN` é obrigatório e nada é corrigido sozinho: TC-27, TC-28 e TC-32, o `CHECK` do banco (TC-25) e a regra P-01 |
| Nome repetido, inclusive em cadastros simultâneos | Médio | TC-10, TC-23, TC-38 e o índice único `(tipo, lower(nome))` (TC-26) |
| O `CHECK` do padrão de sala recusa professor ou material, porque `recurso.nome` é compartilhada | Alto | `CHECK` condicionado ao tipo (P-07) e TC-39 |
| Nome maior que a coluna dá erro `500` | Médio | P-06 (`varchar(100)`) e TC-40 |
| O leitor de JSON converte `30.5` em `30` ou `"30"` em número | Alto | P-04 desliga a conversão; TC-20, TC-21, TC-34 e TC-05 |
| O modelo `recurso` + `sala` fica inconsistente (uma tabela sem a outra) | Médio | TC-18 e gravação na mesma transação |
| Pesquisa com filtros (RF-03) e exclusão ficam sem dono | Médio | Fora desta spec; a exclusão só virá para sala sem reserva (P-02) |
| A T-17 e a T-18 dependem da tabela `recurso` e das suas restrições, criadas aqui | Alto | A T-16 precisa ser mergeada antes das duas; o backlog passa a listar a T-15 e a T-16 como dependências delas |
| A regra RN-16 (padrão `sala-NN`) e os exemplos das outras fontes divergem (`SALA-102` na arquitetura, "sala 102" na T-20) | Médio | A RN-16 está no PRD; a arquitetura e a T-20 passam a usar `sala-102` |
| As regras do time P-01 a P-08 não são do PRD | Baixo | Só a RN-16 é do PRD; mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

A migração do ciclo 1 cria `recurso` e `sala` só com colunas e chaves. As restrições (`CHECK`, índice único) entram na migração do ciclo 7, na mesma entrega da T-16, para os testes do banco terem um red real.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: cadastro válido | Entidade, migração de `recurso` e `sala` (colunas e chaves), repositório e serviço de sala | Separar `recurso` e `sala` em um único agregado |
| 2 | TC-02 a TC-05, TC-20, TC-21, TC-30 e TC-34: capacidade e leitura do JSON | Validação de capacidade maior que zero e leitor de JSON sem conversão de tipos (P-04) | Extrair a validação para o objeto de entrada |
| 3 | TC-06 a TC-09, TC-22, TC-27, TC-28, TC-29, TC-31 a TC-33, TC-35 a TC-37: campos obrigatórios, padrão do nome, tamanhos e limpeza | Validação do padrão `sala-NN`, de obrigatórios e de tamanho de tipo e localização, e limpeza de espaços do tipo e da localização (P-05) | Reaproveitar os textos de erro da T-22 |
| 4 | TC-10 e TC-38: nome único e cadastros simultâneos | Verificação de nome repetido no serviço e tradução do índice único para o `409` genérico | Mover a regra para o serviço |
| 5 | TC-11 a TC-13, TC-15, TC-23 e TC-24: alteração, `restrito` obrigatório e inexistente | Alteração (com o `PUT` substituindo por inteiro e `restrito` obrigatório), nome único na alteração e `404` | Reaproveitar a validação do cadastro |
| 6 | TC-14, TC-16, TC-17 e TC-41: permissão e ordem dos erros | Regra de perfil por operação (T-15) e a ordem `401`, `403`, `404`, `400`, `409` | Declarar o perfil exigido em um só lugar |
| 7 | TC-18, TC-19, TC-25, TC-26, TC-39 e TC-40: estrutura do banco | Migração das restrições: `CHECK` de capacidade, de tipo e de padrão por tipo, índice único `(tipo, lower(nome))` e `varchar(100)` | Revisar a migração |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.
**Comentários:**

Reli a versão atual e os pontos das revisões anteriores foram tratados: `recurso` agora tem o `CHECK` condicionado ao tipo e o índice único `(tipo, lower(nome))`, a leitura do JSON não converte tipos, `restrito` é obrigatório no `PUT`, e os testes de regra estão separados dos testes de contrato. A spec está pronta para seguir.

A aprovação depende do merge do PR #34 antes deste, porque a RN-16 e as dependências novas do backlog só existem lá.

Pontos para acompanhar na implementação, sem bloquear a aprovação:

- TC-38 está no ciclo 4, mas o índice único só entra no ciclo 7. O teste precisa ir para o ciclo 7, ou o índice precisa vir antes.
- A ordem 404 antes de 400 (P-08 e TC-41) exige cuidado, porque a leitura do JSON e o `@Valid` normalmente rodam antes de buscar a sala.
- `restrito` deve ser `Boolean`, para `null` no `PUT` não virar `false`. Vale incluir esse caso.
- A configuração do Jackson sem coerção deve ficar em um só lugar, junto da T-22.
- Os exemplos "sala 101" e "sala 102" que restam na T-20 e na arquitetura devem passar para o padrão `sala-NN`.
- `sala-01` e `sala-001` convivem por decisão do time; vale registrar esse risco na seção 7.

**Parecer:** Aprovada · @Samwel-Merces · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-07
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
