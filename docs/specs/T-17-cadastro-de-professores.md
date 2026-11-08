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
| Status | Liberada |
| Regra crítica | Não |
| Requisitos | RF-02 |
| Característica ISO 25010 | Adequação funcional |
| Criada em | 2026-11-04 |

## 1. Contexto e objetivo

O professor também é um recurso reservável (RF-02): a agenda dele não pode ter sobreposição (RN-02). Esta task cria o cadastro do professor com as suas competências, que alimentam a pesquisa por competência (RF-03). O Administrador escreve; qualquer usuário autenticado consulta. O professor é uma entidade separada do usuário que entra no sistema (ADR-0009).

## 2. Escopo

**Dentro:**
- Cadastrar, alterar, consultar (por id e em lista) e excluir professor, com nome, indicador `restrito` e lista de competências.
- Migração Flyway das tabelas `professor` (com a coluna `matricula`) e `competencia`, ligadas a `recurso` (tipo `PROFESSOR`). A tabela `recurso`, com todas as restrições que a tocam, é criada pela T-16 (G8: tamanho do nome, `CHECK` de tipo, índice único `(tipo, lower(nome))` e `CHECK` do padrão do nome por tipo). Esta task **não altera `recurso`**; ela depende da T-15 e da T-16.
- Validação de nome, de matrícula e de competências, e `409` para nome ou matrícula repetidos.
- Comportamento dos campos omitidos (`restrito` e `competencias`) no cadastro e na alteração.
- Escrita só do Administrador; consulta por qualquer usuário autenticado; `401` e `403` conforme a T-15 e a ordem de erros da seção 4.
- O contrato da exclusão de professor com vínculos: `409`, traduzido a partir da violação de chave estrangeira (SQLState `23503`) pela T-22.

**Fora (e por quê):**
- Pesquisa por competência (RF-03): é de outra task.
- O teste da recusa de exclusão de professor **com reserva, retirada ou manutenção reais**: essas tabelas ainda não existem. Ele é da T-61. Aqui se prova o mesmo mecanismo (chave estrangeira `ON DELETE RESTRICT` mais a tradução de `23503` para `409`) com uma tabela que só existe nos testes (TC-12).
- A agenda e a sobreposição do professor (RN-02): são das T-20 e T-23.
- Ligar o professor a um usuário do sistema: o ADR-0009 os mantém separados.
- O formato das respostas de erro e a tradução de `23503`, `23505` e `23P01` para `409` (T-22), e a autenticação (T-15).
- A criação da tabela `recurso` e do padrão do nome no nível de `recurso`: é da T-16.

## 3. Regras de negócio

- **RF-02:** cadastro e consulta de salas, professores e materiais.
- **RF-01 / T-15:** cadastro, alteração e exclusão só pelo ADMINISTRADOR; consulta por qualquer usuário autenticado (RF-01, T-15).
- **RN-17 (PRD):** o nome do professor segue o padrão `primeironome-ultimonome-matricula` (detalhado na P-01).
- **Modelo (arquitetura 3.1):** o professor é um `recurso` do tipo `PROFESSOR`; as competências ficam em `competencia`, com `descricao`. Esta spec acrescenta a coluna `matricula` em `professor` (P-06).

**Propostas desta spec** (decisões do time):

- **P-01 (RN-17):** o nome do professor segue exatamente o padrão `primeironome-ultimonome-matricula`, como `renata-silva-12345`: duas partes de letras minúsculas sem acento (`a` a `z`), uma matrícula de exatamente 5 dígitos e um hífen entre as três partes, sem nenhum espaço. Primeiro e último nome só, sem nome composto. O nome inteiro tem no máximo **100 caracteres**, o limite de `recurso.nome` (varchar(100)). Qualquer nome fora do padrão (espaço na frente, atrás ou no meio, maiúscula, acento, três nomes, hífen repetido ou sobrando, `_` ou `.` no lugar do hífen, matrícula com letra, com menos ou mais de 5 dígitos ou ausente, mais de 100 caracteres) é recusado com `400`, e nada é corrigido automaticamente. No banco, o padrão é um `CHECK` **condicionado ao tipo**: `tipo <> 'PROFESSOR' OR nome ~ '^[a-z]+-[a-z]+-[0-9]{5}$'`, criado pela migração da T-16 (G8). Como o `CHECK` só vale para `PROFESSOR`, ele não afeta os nomes de sala e de material.
- **P-02:** cada competência tem até 80 caracteres, **contados depois da limpeza**. Antes de validar e gravar, cada competência perde os espaços das pontas e tem os espaços repetidos no meio reduzidos a um só (`"  Banco   de Dados "` vira `"Banco de Dados"`). A competência é única dentro do mesmo professor, sem diferenciar maiúsculas de minúsculas, e a comparação vale sobre o texto já limpo. A lista pode ser vazia.
- **P-03:** o `PUT` **substitui o professor por inteiro**: a lista de competências enviada substitui a anterior, e competências omitidas não são preservadas. Por isso, no `PUT`, `restrito` e `competencias` são **obrigatórios** (G3): ausente dá `400` com o campo no erro. Uma lista vazia enviada de propósito (`"competencias": []`) é válida e remove todas as competências. O valor de `restrito` e o de `competencias` precisam ter o tipo certo (booleano e lista de textos); sem coerção, um tipo errado dá `400` com o motivo "valor inválido" (G14).
- **P-04:** a exclusão só é permitida para professor sem nenhuma reserva (nem no histórico), retirada ou período de manutenção. As chaves estrangeiras `ON DELETE RESTRICT` impedem a exclusão no banco, e a T-22 traduz a violação (SQLState `23503`) para `409` com a mensagem "Este registro está em uso e não pode ser removido" (G1). Ao excluir um professor sem vínculos, as competências, o registro de `professor` e o de `recurso` saem junto.
- **P-05:** no **cadastro**, `restrito` é opcional e vale `false` quando não informado, e `competencias` é opcional e vale lista vazia quando não informada.
- **P-06:** a matrícula é única entre os professores. Ela é a terceira parte do nome e fica guardada na coluna `professor.matricula` (`varchar(5) NOT NULL UNIQUE`), preenchida pelo serviço a partir do nome (G8). Dois professores com nomes diferentes não podem ter a mesma matrícula (`409`).
- **P-07:** quando a recusa de nome ou de matrícula repetidos vem da verificação do serviço, a mensagem diz qual dos dois já existe. Quando ela vem do banco, numa corrida entre dois cadastros, a resposta é o `409` genérico da T-22: "Esta operação conflita com outro registro" (G2).

## 4. Contrato da API

Endpoints propostos. Os erros seguem o formato da T-22 (`ProblemDetail`). **Ordem de erros** (G9): `401` (sem autenticação), `403` (perfil sem permissão), `404` (professor inexistente), `403` (só o dono, se existir), `400` (corpo inválido) e `409` (conflito de regra). Por exemplo, um Solicitante que envia um nome inválido recebe `403`, e um `PUT` em um id inexistente com corpo inválido recebe `404`.

`POST /api/professores`: cadastra (ADMINISTRADOR). `restrito` e `competencias` são opcionais.

```json
{ "nome": "ana-souza-12345", "restrito": false, "competencias": ["Banco de Dados", "Java"] }
```

`PUT /api/professores/{id}`: altera, substituindo o professor por inteiro (ADMINISTRADOR). O corpo é o mesmo, com `nome`, `restrito` e `competencias` **obrigatórios**. `DELETE /api/professores/{id}`: exclui (ADMINISTRADOR). `GET /api/professores/{id}` e `GET /api/professores`: consultam (qualquer autenticado).

| Situação | Status HTTP | Corpo |
|---|---|---|
| Cadastro | 201 | O professor, com `id`, `restrito` e competências (lista vazia se omitida) |
| Alteração ou consulta | 200 | O professor (ou a lista) |
| Exclusão | 204 | Sem corpo |
| Nome fora do padrão, competência inválida, campo obrigatório ausente no `PUT` ou tipo errado | 400 | `ProblemDetail`, `erros` com campo e motivo (por exemplo `nome`, `restrito`, `competencias`) |
| Nome repetido (verificação do serviço) | 409 | `ProblemDetail` em linguagem de negócio: "Já existe um professor com esse nome" |
| Matrícula repetida (verificação do serviço) | 409 | `ProblemDetail`: "Já existe um professor com essa matrícula" |
| Nome ou matrícula repetidos barrados pelo banco numa corrida | 409 | `ProblemDetail` genérico da T-22: "Esta operação conflita com outro registro" |
| Professor não encontrado | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Exclusão de professor com reserva, retirada ou manutenção | 409 | `ProblemDetail` da T-22 (SQLState `23503`): "Este registro está em uso e não pode ser removido" |
| Sem autenticação | 401 | `ProblemDetail` (T-15, formato da T-22) |
| Perfil sem permissão | 403 | `ProblemDetail` (T-15, formato da T-22) |

## 5. Critérios de aceite

### CA-01: Administrador cadastra um professor com competências

- **Dado** um Administrador autenticado
- **Quando** ele cadastra um professor com nome no padrão e uma lista de competências
- **Então** a resposta é `201` com o professor e as competências
- **E** o professor fica gravado e pode ser consultado
- **E** se `restrito` não for informado, vale `false`, e se `competencias` não for informada, vale lista vazia (P-05)

### CA-02: O nome segue o padrão primeironome-ultimonome-matricula

- **Dado** o cadastro ou a alteração de professor
- **Quando** o cliente deixa o nome em branco ou fora do padrão da P-01 (RN-17): com espaço na frente, atrás ou no meio, com maiúscula, com acento, com mais de dois nomes, com separador inválido (`_`, `.`, hífen repetido ou sobrando), com matrícula que não tem exatamente 5 dígitos, sem matrícula, ou com mais de 100 caracteres
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `nome` com o motivo "deve seguir o padrão primeironome-ultimonome-matricula"
- **E** um nome no padrão, com até 100 caracteres, é aceito (`renata-silva-12345`)

### CA-03: Competências válidas, limpas e sem repetição

- **Dado** o cadastro ou a alteração de professor
- **Quando** o cliente envia uma competência em branco (ou só com espaços), com mais de 80 caracteres depois da limpeza, ou repetida na mesma lista (sem diferenciar maiúsculas, depois da limpeza)
- **Então** o pedido é recusado com `400` e nada é gravado
- **E** o erro aponta o campo `competencias`
- **E** uma competência válida é gravada sem os espaços das pontas e sem espaços repetidos no meio (P-02)

### CA-04: Alterar substitui o professor por inteiro

- **Dado** um professor com competências
- **Quando** o Administrador o altera com `nome`, `restrito` e uma lista de competências
- **Então** a resposta é `200` e só a lista nova permanece
- **E** alterar mantendo o próprio nome não conflita consigo mesmo
- **E** uma lista vazia enviada de propósito remove todas as competências
- **E** se `restrito` ou `competencias` forem omitidos no `PUT`, o pedido é recusado com `400` e o professor não muda (P-03)

### CA-05: Qualquer usuário autenticado consulta

- **Dado** professores cadastrados
- **Quando** um usuário de qualquer perfil consulta um professor por id ou a lista
- **Então** a resposta é `200`, com as competências

### CA-06: Professor inexistente dá 404

- **Dado** um id que não corresponde a nenhum professor
- **Quando** o cliente consulta, altera ou exclui
- **Então** a resposta é `404` com "Recurso não encontrado"

### CA-07: Exclusão de professor

- **Dado** um professor sem reserva, retirada nem período de manutenção
- **Quando** o Administrador o exclui
- **Então** a resposta é `204`, e o registro de `professor`, as competências e o registro de `recurso` deixam de existir
- **E** o mesmo nome e a mesma matrícula podem ser cadastrados de novo, com `201`
- **E** com um vínculo que impede a exclusão (chave estrangeira `ON DELETE RESTRICT`), a resposta é `409` com "Este registro está em uso e não pode ser removido" (T-22), e o professor continua (P-04)
- **E** o teste com reserva, retirada e manutenção reais é da T-61; aqui se prova o mecanismo com uma tabela de apoio que só existe nos testes

### CA-08: Só o Administrador escreve

- **Dado** as operações de cadastro, alteração e exclusão
- **Quando** um SOLICITANTE ou um RESPONSAVEL as tenta, ou alguém sem autenticação
- **Então** quem está autenticado sem permissão recebe `403`, e quem não está autenticado recebe `401`
- **E** nada é gravado

### CA-09: A estrutura do banco segue o modelo

- **Dado** a migração Flyway aplicada (a da T-16 para `recurso` e a desta task para `professor` e `competencia`)
- **Quando** um professor é gravado
- **Então** existe um registro em `recurso` com `tipo = PROFESSOR`, outro em `professor` com a `matricula` e um em `competencia` por competência
- **E** o nome é único por tipo de recurso também no banco, sem diferenciar maiúsculas
- **E** o banco recusa, para `tipo = PROFESSOR`, um nome fora do padrão, e esse `CHECK` não recusa o nome de uma sala no padrão da sala

### CA-10: Nome repetido é recusado

- **Dado** um professor cadastrado
- **Quando** o cliente cadastra outro professor com o mesmo nome, ou altera o nome de um para o de outro
- **Então** o pedido é recusado com `409` em linguagem de negócio e nada é gravado
- **E** se a recusa vier do banco numa corrida, a resposta é o `409` genérico da T-22 (P-07)

### CA-11: A matrícula é única

- **Dado** um professor cadastrado
- **Quando** o cliente cadastra ou altera outro professor com nome diferente e a mesma matrícula
- **Então** o pedido é recusado com `409` ("Já existe um professor com essa matrícula") e nada é gravado
- **E** o banco também recusa uma matrícula repetida (`UNIQUE` em `professor.matricula`)

### CA-12: A ordem dos erros é a da seção 4

- **Dado** um pedido que reúne mais de um erro
- **Quando** um Solicitante envia um nome inválido, ou um `PUT` com id inexistente traz o corpo inválido
- **Então** a resposta é `403` no primeiro caso e `404` no segundo, e não `400`

## 6. Casos de teste

Testes **unitários** exercitam a regra pura (o validador do nome, das competências e a limpeza) sem HTTP. Testes de **integração** conferem status e corpo HTTP, a permissão e o banco de teste (Testcontainers, PostgreSQL real). Casos de nome indicam `tipo = PROFESSOR` quando falam do banco.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Classe válida | Cadastro com nome e duas competências | 201 com as duas competências | Integração |
| TC-02 | CA-01 | Classe válida | Cadastro com lista de competências vazia | 201 | Integração |
| TC-03 | CA-02 | Classe inválida | Nome ausente ou só com espaços | O validador recusa, com erro no campo `nome` | Unitário |
| TC-04 | CA-02 | Valor limite | Matrícula com 5 dígitos (`renata-silva-12345`) / com 4 (`renata-silva-1234`) | 5 aceita; 4 recusa | Unitário |
| TC-05 | CA-03 | Classe inválida | Competência em branco | O validador recusa, com erro no campo `competencias` | Unitário |
| TC-06 | CA-03 | Valor limite | Competência com 80 caracteres / com 81 | 80 aceita; 81 recusa | Unitário |
| TC-07 | CA-03 | Classe inválida | "Java" e "java" na mesma lista | O validador recusa | Unitário |
| TC-08 | CA-04 | Classe válida | Alterar de [Java, SQL] para [Python] | 200; só "Python" permanece | Integração |
| TC-09 | CA-05 | Classe válida | SOLICITANTE, RESPONSAVEL e ADMINISTRADOR consultam por id e a lista | 200 para os três, com competências | Integração |
| TC-10 | CA-06 | Classe inválida | `GET`, `PUT` e `DELETE` com id inexistente | 404 | Integração |
| TC-11 | CA-07 | Classe válida | Excluir professor sem nenhum vínculo | 204; professor e competências somem | Integração |
| TC-12 | CA-07 | Conflito | Excluir professor cujo `recurso` é referenciado por uma tabela de apoio criada só nos testes (chave estrangeira `ON DELETE RESTRICT`) | 409 "Este registro está em uso e não pode ser removido" (T-22, SQLState `23503`); o professor continua. O teste com reserva, retirada e manutenção reais é da T-61 | Integração |
| TC-13 | CA-08 | Tabela de decisão | SOLICITANTE e RESPONSAVEL em `POST`, `PUT` e `DELETE` | 403; nada é gravado | Integração |
| TC-14 | CA-08 | Tabela de decisão | Anônimo em `POST`, `PUT`, `DELETE` e `GET` | 401 | Integração |
| TC-15 | CA-09 | Classe válida | Gravar um professor com 2 competências | 1 linha em `recurso` (`tipo = PROFESSOR`), 1 em `professor` (com `matricula`), 2 em `competencia` | Integração |
| TC-16 | CA-10 | Classe inválida | Cadastrar outro professor com o mesmo nome | 409 "Já existe um professor com esse nome"; nada é gravado | Integração |
| TC-17 | CA-10 | Classe inválida | Alterar o nome de um professor para o de outro | 409 | Integração |
| TC-18 | CA-09 | Conflito | `INSERT` direto no banco, com `tipo = PROFESSOR`, do mesmo tipo e nome (também em maiúsculas diferentes) | Recusado pelo banco (índice único `(tipo, lower(nome))`) | Integração |
| TC-19 | CA-02 | Valor limite | Matrícula com 5 dígitos / com 6 (`renata-silva-123456`) | 5 aceita; 6 recusa | Unitário |
| TC-20 | CA-02 | Classe inválida | Nomes ` renata-silva-12345`, `renata-silva-12345 `, `Renata-Silva-12345`, `renata silva 12345`, `renata-silva`, `renata-silva-abc`, `renata-maria-silva-12345`, `renata-silvá-12345` (teste parametrizado) | Todos recusados pelo validador, com erro no campo `nome` | Unitário |
| TC-21 | CA-09 | Conflito | `INSERT` direto no banco com `tipo = PROFESSOR` e o nome `Ana Souza` | Recusado pelo banco (`CHECK` do padrão) | Integração |
| TC-22 | CA-10 | Classe válida | Cadastrar `ana-souza-11111` e `ana-souza-22222` | 201 nos dois (homônimos com matrículas diferentes). Teste de proteção: já passa desde o ciclo 1 | Integração |
| TC-23 | CA-11 | Conflito | Cadastrar `ana-souza-12345` e depois `bia-lima-12345` | O segundo recebe 409 "Já existe um professor com essa matrícula"; nada é gravado | Integração |
| TC-24 | CA-11 | Conflito | `INSERT` direto em `professor` com uma `matricula` que já existe | Recusado pelo banco (`UNIQUE` em `matricula`) | Integração |
| TC-25 | CA-09 | Classe válida | `INSERT` direto em `recurso` com `tipo = SALA` e nome `sala-01` | Aceito (o `CHECK` do professor não se aplica a outro tipo) | Integração |
| TC-26 | CA-02 | Valor limite | Nome no padrão com 100 caracteres (primeiro nome de 46 letras, último de 47) / com 101 caracteres | 100 aceita; 101 recusa | Unitário |
| TC-27 | CA-02 | Classe inválida | Nomes `renata_silva-12345`, `renata--silva-12345`, `renata-silva--12345`, `renata.silva-12345`, `-renata-silva-12345`, `renata-silva-12345-` (teste parametrizado) | Todos recusados pelo validador, com erro no campo `nome` | Unitário |
| TC-28 | CA-02 | Classe inválida | `POST /api/professores` com o nome `Renata Silva` | 400 `ProblemDetail`, `erros` com `campo = nome` e o motivo do padrão; nada é gravado | Integração |
| TC-29 | CA-04 | Classe válida | `PUT` mantendo o próprio nome e trocando as competências | 200 (não conflita consigo mesmo) | Integração |
| TC-30 | CA-07 | Classe válida | Excluir um professor sem vínculos e cadastrar de novo o mesmo nome e a mesma matrícula | Depois da exclusão, 0 linhas em `recurso`, `professor` e `competencia` para ele; o recadastro dá 201 | Integração |
| TC-31 | CA-04 | Classe inválida | `PUT` sem o campo `restrito` | 400, `erros` com `campo = restrito`; o professor não muda | Integração |
| TC-32 | CA-04 | Classe inválida | `PUT` sem o campo `competencias` | 400, `erros` com `campo = competencias`; o professor não muda | Integração |
| TC-33 | CA-04 | Classe válida | `PUT` com `"competencias": []` em um professor que tinha duas | 200; o professor fica sem competências | Integração |
| TC-34 | CA-01 | Classe válida | `POST` sem `restrito` e sem `competencias` | 201; `restrito = false` e `competencias = []` | Integração |
| TC-35 | CA-03 | Classe válida | Competência `"  Banco   de Dados "` | O validador a devolve como `"Banco de Dados"`; é gravada limpa | Unitário |
| TC-36 | CA-03 | Classe inválida | `" Java"` e `"java "` na mesma lista | O validador recusa (repetida depois da limpeza) | Unitário |
| TC-37 | CA-03 | Valor limite | Competência só com espaços / com 80 caracteres mais espaços nas pontas | Só com espaços recusa; 80 caracteres limpos aceita | Unitário |
| TC-38 | CA-12 | Classe inválida | SOLICITANTE envia `POST` com nome inválido | 403 (o perfil vem antes da validação) | Integração |
| TC-39 | CA-12 | Classe inválida | ADMINISTRADOR envia `PUT` em id inexistente com corpo inválido | 404 (a existência vem antes da validação) | Integração |
| TC-40 | CA-10 | Conflito | `POST` com um nome que o teste insere direto no banco depois da verificação do serviço (corrida simulada) | 409 genérico da T-22: "Esta operação conflita com outro registro"; nada é gravado. A concorrência real é da T-37 | Integração |
| TC-41 | CA-01 | Classe inválida | `POST` com `"restrito": "talvez"` | 400, `erros` com `campo = restrito` e o motivo "valor inválido"; sem coerção (G14) | Integração |
| TC-42 | CA-01 | Classe inválida | `POST` com `"competencias": "Java"` (texto, não lista) | 400, `erros` com `campo = competencias` e o motivo "valor inválido" | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| A permissão do Administrador é esquecida em `PUT` ou `DELETE` | Alto | TC-13 e TC-14 cobrem as três operações de escrita |
| Excluir professor deixa reserva, retirada ou manutenção órfã | Alto | P-04 e a chave estrangeira `ON DELETE RESTRICT`; a tradução de `23503` para `409` é da T-22 (G1). TC-12 prova o mecanismo com uma tabela de apoio; o teste com tabelas reais é da T-61 |
| A T-22 não traduzir `23503` e a exclusão devolver 500 | Alto | A T-22 inclui o caso (G1); TC-12 falha se isso faltar |
| Competência duplicada por diferença de maiúsculas ou de espaços | Baixo | TC-07, TC-35 e TC-36 |
| Dois professores com o mesmo nome, ou com a mesma matrícula, ou o mesmo professor cadastrado duas vezes | Médio | A matrícula no nome separa os homônimos (TC-22); nome repetido é recusado (TC-16 a TC-18) e matrícula repetida também (CA-11, TC-23 e TC-24) |
| O padrão do nome não admite nome composto nem acento | Baixo | Decisão do time (P-01, RN-17); o nome exibido pode ser formatado nas telas |
| Nome válido pela expressão regular, mas maior que a coluna, causaria erro interno | Médio | P-01 limita a 100 caracteres; TC-26 testa 100 e 101 |
| O `CHECK` do padrão do professor recusar nomes de sala ou de material, por estar em `recurso.nome` | Alto | O `CHECK` é condicionado ao tipo (P-01); TC-25 prova que uma sala no padrão passa; TC-21 usa `tipo = PROFESSOR` |
| A T-16, a T-17 e a T-18 criariam a mesma tabela `recurso` ou alterariam as restrições dela | Médio | Só a T-16 cria `recurso` completa (G8); esta task só acrescenta `professor` e `competencia`; a T-17 depende da T-15 e da T-16 mergeada |
| `PUT` omitido apagar competências ou mudar `restrito` sem pedido explícito | Médio | P-03: no `PUT`, `restrito` e `competencias` são obrigatórios (TC-31 e TC-32), e lista vazia só vale se enviada (TC-33) |
| Pesquisa por competência (RF-03) fica sem dono | Médio | Fora desta spec; a lista de competências já fica pronta para a pesquisa |
| As propostas P-01 a P-07 são decisões do time | Baixo | Registradas no PRD como RN-17 (padrão do nome) e marcadas como propostas aqui; mudar um limite altera poucos testes |

## 8. Mini-plano de implementação (ciclos TDD)

Os testes que **já passam** quando o ciclo começa são de proteção e estão marcados; o red real de cada ciclo é o que vem antes deles.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01, TC-02 e TC-34: cadastro válido, com `restrito` e `competencias` omitidos | Entidade, migração de `professor` (com `matricula`) e `competencia`, repositório e serviço de professor | Tratar `recurso` e `professor` como um agregado |
| 2 | TC-03, TC-04, TC-19, TC-20, TC-26, TC-27, TC-28, TC-41 e TC-42: nome e tipos | Validação do padrão `primeironome-ultimonome-matricula`, do limite de 100 caracteres e dos tipos do corpo | Extrair a validação para o objeto de entrada |
| 3 | TC-05, TC-06, TC-07, TC-35, TC-36 e TC-37: competências | Limpeza de espaços e validação de tamanho, vazio e repetição | Isolar a regra das competências |
| 4 | TC-16, TC-23 e TC-40: nome e matrícula repetidos no cadastro (TC-22 é de proteção: já passa) | Verificação de nome e de matrícula repetidos no serviço e `UNIQUE` de `matricula` | Mover a regra para o serviço |
| 5 | TC-08, TC-09, TC-10, TC-17, TC-29, TC-31, TC-32 e TC-33: alteração, consulta e `404` | `PUT` que substitui por inteiro, com `restrito` e `competencias` obrigatórios, nome único na alteração, consultas e `404` | Reaproveitar a validação do cadastro |
| 6 | TC-11, TC-12 e TC-30: exclusão | Exclusão de professor sem vínculos, remoção do `recurso` e das competências, e a `409` de chave estrangeira (com a T-22) | Mover a regra para o serviço |
| 7 | TC-13, TC-14, TC-38 e TC-39: permissão e ordem de erros | Regra de perfil por operação (T-15) e ordem `401`, `403`, `404`, `400`, `409` | Declarar o perfil exigido em um só lugar |
| 8 | TC-24: matrícula única no banco (TC-15, TC-18, TC-21 e TC-25 são de proteção da migração da T-16: só falham se ela faltar) | `UNIQUE (matricula)` na migração de `professor` | Revisar a migração |

## 9. Revisão por pares

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Realizei uma nova leitura da versão atual da T-17, considerando os pontos levantados nas revisões anteriores e as correções adicionadas posteriormente.

Confirmei que a matrícula do professor passou a ser representada em campo próprio, com formato definido e garantia de unicidade. Também conferi os critérios e casos de teste relacionados ao cadastro com matrícula duplicada, à atualização dos dados do professor e à prevenção de conflitos indevidos durante a alteração do próprio registro.

A validação do nome está limitada ao tipo `PROFESSOR`, evitando impacto sobre salas e materiais armazenados na estrutura compartilhada de recursos. O limite máximo do nome, os formatos inválidos, os separadores incorretos, a atualização mantendo o próprio nome e o recadastro após exclusão também estão contemplados na especificação.

Revisei ainda o comportamento dos campos opcionais no cadastro e na alteração, incluindo `restrito` e `competencias`, bem como os cenários de limpeza, normalização e validação das competências. O tratamento das violações de integridade está relacionado ao contrato de erros da T-22, incluindo a conversão da violação de chave estrangeira para uma resposta de conflito segura.

Os critérios de aceite estão objetivos, os casos de teste permitem determinar aprovação ou falha e os contratos HTTP cobrem os resultados esperados. Não identifiquei inconsistências impeditivas com o PRD, a arquitetura, o ADR-0009 ou as specs relacionadas.

Considero atendidos os pontos necessários para a liberação da especificação.

**Parecer:** Aprovada · @CaiosAguiar · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** 2026-11-07
- **Resultado:** APROVADA na spec validation

## 11. Grill-with-docs

> Obrigatório só para regra crítica. Nas demais, escreva "Não se aplica".

Não se aplica. Esta task não é de regra crítica.
