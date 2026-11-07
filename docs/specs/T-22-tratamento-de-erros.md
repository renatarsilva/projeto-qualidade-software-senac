# Spec T-22: tratamento padronizado de erros e validação de entrada

<!--
Como usar: copie este arquivo para docs/specs/<ID-DA-TASK>-<descricao-curta>.md
e substitua todo trecho _preencher_. O fluxo completo está em docs/processo/ai-workflow.md.

Status possíveis, nesta ordem:
Rascunho → Em revisão → Aprovada → Validada → Liberada
(tasks não críticas vão de Validada direto para Liberada, sem grill)
-->

| Campo | Valor |
|---|---|
| Task | T-22 |
| Autor(a) | @renatarsilva |
| Revisor(a) | @Samwel-Merces |
| Status | Em revisão |
| Regra crítica | Não |
| Requisitos | RF-13, RNF-07, RNF-08 |
| Característica ISO 25010 | Segurança |
| Criada em | 2026-11-02 |

## 1. Contexto e objetivo

Hoje cada endpoint poderia devolver erros em um formato diferente, e uma falha inesperada poderia expor detalhes internos, como stack trace, SQL ou nome de classe. Esta task cria **um jeito único** de responder a erros, na API e nas telas, para os três perfis (Solicitante, Responsável e Administrador). O objetivo é que toda mensagem seja compreensível (RF-13) e que nenhum erro vaze detalhe interno (RNF-08).

## 2. Escopo

**Dentro:**
- Formato único de erro da API (`ProblemDetail`, RFC 9457), com mensagem compreensível em português.
- `400` para entrada inválida, com o campo e o motivo (RNF-07), inclusive tipo ou formato inválido de campo na leitura do JSON (por exemplo `capacidade` com `"abc"`, `30.5`, `"30"` ou `2147483648`).
- `404` para recurso não encontrado e `409` para conflito de regra, inclusive a violação de restrição do banco: `UNIQUE` e `EXCLUDE` (conflito genérico) e chave estrangeira, SQLState `23503` (registro em uso). Nenhuma delas vira `500`.
- Os erros que o Spring já trata (JSON malformado, id não numérico, rota inexistente, método não permitido e formato não suportado) mantêm o código 4xx certo e não viram `500`.
- O campo do erro de uma regra de negócio ou de um valor inválido de enum (por exemplo `estado`, `termino`).
- O formato de `401` e `403`, com a fábrica de `ProblemDetail` que os filtros de segurança da T-15 usam, e a exceção de negócio de **acesso negado** lançada pelo serviço (por exemplo, quando quem pede não é o dono da reserva, T-21), que vira `403` neutro pela mesma fábrica.
- Erro inesperado vira `500` neutro, sem stack trace, SQL, nome de classe nem caminho interno (RNF-08). O detalhe técnico fica só no log.
- Um **ponto único** que separa a resposta de API (JSON) da de telas (HTML) pelo caminho da requisição original, inclusive em rota inexistente, `405`, `415` e falha em filtro.
- Página de erro amigável nas telas, para falhas, `404` e `409`, inclusive falha durante a renderização.
- A ordem em que os erros são verificados (seção 4).

**Fora (e por quê):**
- A implementação dos filtros que devolvem `401` e `403` (autenticação e autorização): é da T-15, que usa o formato e a fábrica definidos aqui. A **prova da resposta real** do filtro (`Content-Type`, `title` e `instance`) também é da T-15 (CA-10). A T-15 depende da T-22, e o backlog registra isso.
- As regras de negócio que geram o `409`, como a sobreposição de horários: são da T-23 e da T-24. Esta task só define o **formato** da resposta.
- As validações de cada campo de cada endpoint: cada task de funcionalidade define as suas. Esta task entrega o mecanismo.
- O conteúdo e o visual das telas: são da T-44 e da T-45.

## 3. Regras de negócio

Esta task não cria regra de negócio: ela aplica requisitos do PRD.

- **RF-13:** interface responsiva e **mensagens de erro compreensíveis**.
- **RNF-07 (validação de entrada):** toda entrada inválida catalogada é recusada, e nenhuma gravação inválida acontece. A exigência é oficial; a medida (100 % do catálogo) é candidata, com validação pendente.
- **RNF-08 (tratamento seguro de erros):** nenhuma resposta de erro contém stack trace, SQL, nome de classe nem caminho interno.

## 4. Contrato da API

Esta task não cria um endpoint: ela define **como todos os endpoints respondem a erro**. Toda resposta de erro de `/api/**` tem o tipo `application/problem+json` (RFC 9457), com mensagens em português, e vale para os três perfis, **mesmo quando o cliente envia `Accept: application/json`**. O tratamento em JSON vale só para a API: as demais rotas (telas Thymeleaf) recebem página HTML.

**Formato:**

| Campo | Conteúdo |
|---|---|
| `type` | Identifica o tipo do erro |
| `title` | Título curto, em português, do catálogo abaixo |
| `status` | O código HTTP |
| `detail` | A explicação para a pessoa, sem detalhe interno |
| `instance` | O caminho da requisição |
| `erros` | Só no `400`: a lista de campos inválidos, cada um com `campo` e `motivo`. O `campo` é o nome do campo do JSON, ou o campo que a regra de negócio indica (por exemplo `termino` quando a regra compara início e término) |

**Títulos:** 400 "Dados inválidos", 401 "Autenticação necessária", 403 "Acesso negado", 404 "Recurso não encontrado", 405 "Método não permitido", 409 "Conflito", 415 "Formato não suportado" e 500 "Erro inesperado".

**Exemplo de entrada inválida (`400`):**

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/testes-de-erro/cadastros",
  "erros": [
    { "campo": "capacidade", "motivo": "deve ser maior que zero" }
  ]
}
```

**Leitura do JSON (sem coerção).** Um campo com tipo ou formato inválido na leitura devolve `400` com `erros` apontando o campo e o motivo "valor inválido". Para isso o Jackson é configurado **sem coerção**: um decimal não vira inteiro (`30.5` não vira `30`; `ACCEPT_FLOAT_AS_INT` desligado) e um texto não vira número (`"30"` e `"abc"` são recusados; coerção de escalares desligada). Valores acima do inteiro (`2147483648`) também dão `400` no mesmo campo.

**Separação entre API e telas.** `@RestControllerAdvice` escolhe por controlador, não por URL, e rota inexistente, `405`, `415` e falha em filtro acontecem sem controlador escolhido (ou chegam ao `/error`). Por isso há **um ponto único** (o `ErrorController` do `/error`, mais um tratamento que consulta o caminho original da requisição): se o caminho original começa com `/api/`, responde `ProblemDetail` `application/problem+json`; em qualquer outro caminho, responde a página HTML amigável, qualquer que seja o `Accept`.

**Mensagem do `409`: precedência.** A verificação do **serviço** vem antes do banco. Quando ela pega o caso (nome ou patrimônio repetido, horário sobreposto), a resposta traz a mensagem **específica** da regra, como "A sala já está reservada neste horário" (T-18 e T-20). Quando a verificação passa e o banco recusa na corrida entre pedidos, a resposta traz a mensagem **fixa e genérica** "Esta operação conflita com outro registro". Qualquer outra falha de acesso a dados continua `500` neutro.

**Ordem dos erros nos endpoints.** Cada pedido é verificado nesta ordem, e a primeira falha responde: `401` (autenticação), `403` por perfil, `404` (o recurso existe?), `403` por dono (é o dono?), `400` (entrada válida?) e `409` (regra de negócio e banco).

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Entrada inválida | 400 | `ProblemDetail` com a lista `erros` (campo e motivo) |
| Campo com tipo ou formato inválido na leitura do JSON (`"abc"`, `30.5`, `"30"`, `2147483648`) | 400 | `ProblemDetail` com `erros`: `campo` = nome do campo e `motivo = "valor inválido"` |
| Valor de enum inexistente ou regra entre campos | 400 | `ProblemDetail` com `erros` apontando o campo certo (por exemplo `estado`, `termino`) |
| JSON malformado, ou id que não é número | 400 | `ProblemDetail`: "Corpo da requisição inválido" ou `erros` com o campo `id` |
| Recurso ou rota não encontrados | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Método não permitido | 405 | `ProblemDetail`: "Método não permitido" |
| Formato não suportado | 415 | `ProblemDetail`: "Formato não suportado" |
| Conflito de regra pego pelo serviço | 409 | `ProblemDetail` com a mensagem específica da regra, em linguagem de negócio |
| Violação de `UNIQUE` ou `EXCLUDE` no banco | 409 | `ProblemDetail` com a mensagem fixa "Esta operação conflita com outro registro" |
| Violação de chave estrangeira no banco (SQLState `23503`) | 409 | `ProblemDetail` com a mensagem fixa "Este registro está em uso e não pode ser removido" |
| Erro inesperado, inclusive falha de acesso a dados que não é conflito | 500 | `ProblemDetail` neutro: "Ocorreu um erro inesperado. Tente novamente mais tarde." Sem stack trace, SQL, nome de classe nem caminho |
| Sem autenticação, ou perfil sem permissão (filtro de segurança, T-15) | 401 e 403 | `ProblemDetail` neutro, mesmo formato, criado pela fábrica desta task |
| Acesso negado lançado pelo serviço (por exemplo, quem pede não é o dono) | 403 | `ProblemDetail` neutro "Você não tem permissão para esta operação", criado pela mesma fábrica |

Nas telas, os mesmos erros aparecem como uma página de erro amigável, em português, sem detalhe técnico e com um link de volta para a página inicial.

## 5. Critérios de aceite

### CA-01: Entrada inválida (400)

- **Dado** que existe a rota de teste de cadastro (`POST /api/testes-de-erro/cadastros`, só em `src/test`), com um campo `capacidade`
- **Quando** o cliente envia uma requisição informando uma capacidade menor ou igual a 0 (ex.: 0 ou -1)
- **Então** o sistema retorna o status HTTP 400 Bad Request
- **E** o corpo traz, na lista `erros`, o campo inválido e a mensagem de negócio correspondente (ex.: "deve ser maior que zero")

### CA-02: Recurso inexistente (404)

- **Dado** que a rota de teste de consulta por ID está disponível (`GET /api/testes-de-erro/cadastros/{id}`)
- **Quando** o cliente consulta um identificador inexistente
- **Então** o sistema retorna o status HTTP 404 Not Found
- **E** o corpo traz a mensagem padrão em português: "Recurso não encontrado"
- **E** uma rota que não existe devolve o mesmo 404

### CA-03: Conflito de regra de negócio (409)

- **Dado** que existe o mecanismo de tratamento de conflitos de negócio (como uma reserva com horário sobreposto ou uma exceção de conflito de teste)
- **Quando** o cliente executa a operação que gera o conflito de domínio
- **Então** o sistema retorna o status HTTP 409 Conflict
- **E** o corpo traz a mensagem específica da regra, em linguagem de negócio, sem expor rastros técnicos do banco de dados

### CA-04: Erro inesperado (500 neutro)

- **Dado** que ocorre uma falha ou exceção não tratada no servidor durante o processamento da requisição
- **Quando** o sistema intercepta o erro inesperado
- **Então** o sistema retorna o status HTTP 500 Internal Server Error
- **E** o corpo traz obrigatoriamente o detalhe fixo: "Ocorreu um erro inesperado. Tente novamente mais tarde."
- **E** o corpo não contém nenhum dos termos proibidos da lista única (seção 6), que vão exclusivamente para o log do servidor
- **E** esse tratamento genérico não transforma em 500 os erros que o Spring já trata (CA-07)

### CA-05: Tela de erro amigável (Thymeleaf)

- **Dado** que a pessoa usuária acessa uma rota da interface web renderizada pelo servidor
- **Quando** ocorre uma falha ou erro não tratado durante a requisição ou a renderização da página
- **Então** o servidor responde com uma página HTML de erro amigável, em português, com uma frase que diz o que houve e um link para a página inicial
- **E** isso vale para falha, `404` e `409`, inclusive quando a falha acontece durante a renderização da página
- **E** a página não exibe nenhum termo proibido da lista única
- **E** a resposta das telas nunca é JSON

### CA-06: Formato de erro padronizado (RFC 9457, Problem Details)

- **Dado** que qualquer resposta de erro da API (400, 403, 404, 405, 409, 415 ou 500) é disparada em `/api/**`
- **Quando** o cliente recebe o corpo de erro do servidor, com qualquer cabeçalho `Accept` (inclusive `Accept: application/json`)
- **Então** o cabeçalho `Content-Type` é sempre `application/problem+json`
- **E** o corpo JSON contém os campos do contrato: `type`, `title`, `status`, `detail` e `instance`
- **E** o `title` é o do catálogo em português da seção 4, nunca o texto padrão em inglês (como "Bad Request")

### CA-07: Erros que o Spring já trata mantêm o código certo

- **Dado** o tratamento genérico de erros
- **Quando** o cliente envia um JSON malformado, um id que não é número, uma rota que não existe, um método não permitido ou um formato não suportado
- **Então** o sistema devolve 400, 400, 404, 405 e 415, respectivamente, e não 500
- **E** cada resposta segue o formato do CA-06 e não contém termo proibido

### CA-08: Violação de `UNIQUE` ou `EXCLUDE` vira conflito genérico

- **Dado** uma operação cuja verificação do serviço passa, mas que viola uma restrição `UNIQUE` ou `EXCLUDE` do banco (por exemplo, quem perde uma corrida entre pedidos)
- **Quando** o banco recusa a gravação
- **Então** o sistema devolve 409 com a mensagem fixa "Esta operação conflita com outro registro"
- **E** quando a verificação do serviço pega o caso antes do banco, a resposta traz a mensagem específica da regra (CA-03)
- **E** qualquer outra falha de acesso a dados continua 500 neutro

### CA-09: O campo do erro vem da regra

- **Dado** um valor de enum inexistente, ou uma regra que compara dois campos
- **Quando** o cliente envia esse pedido
- **Então** o sistema devolve 400 com `erros` apontando o campo certo (por exemplo `estado` para o enum e `termino` para a regra de início e término)

### CA-10: 401 e 403 seguem o mesmo formato

- **Dado** o formato de erro desta task
- **Quando** a fábrica cria o erro de autenticação ou de permissão
- **Então** o `ProblemDetail` tem `status` 401 ou 403, os `title` "Autenticação necessária" ou "Acesso negado", `type`, `detail` e `instance`, e não contém termo proibido
- **E** a **resposta real do filtro de segurança** (com `Content-Type = application/problem+json`, `title` e `instance`) é provada na T-15, que depende desta fábrica

### CA-11: Violação de chave estrangeira vira conflito de "em uso"

- **Dado** um registro referenciado por outro (por exemplo, um material ou professor com reserva)
- **Quando** o cliente tenta apagá-lo e o banco recusa com violação de chave estrangeira (SQLState `23503`)
- **Então** o sistema devolve 409 com a mensagem fixa "Este registro está em uso e não pode ser removido"
- **E** o corpo não contém termo proibido, e o registro continua no banco

### CA-12: Tipo ou formato inválido na leitura do JSON

- **Dado** um campo numérico inteiro na requisição (como `capacidade`)
- **Quando** o cliente envia `"abc"`, `30.5`, `"30"` ou `2147483648`
- **Então** o sistema devolve 400 com `erros` apontando o campo (`capacidade`) e o motivo "valor inválido"
- **E** `30.5` não é arredondado para `30`, e `"30"` não é convertido em número (Jackson sem coerção)

### CA-13: Acesso negado pelo serviço vira 403

- **Dado** uma regra de serviço que nega o acesso (por exemplo, quem pede não é o dono da reserva, T-21)
- **Quando** o serviço lança a exceção de acesso negado
- **Então** o sistema devolve 403 com `ProblemDetail` neutro, criado pela mesma fábrica do CA-10, e não 500
- **E** a resposta não revela o dono nem outro dado da reserva

### CA-14: API e telas são separadas pelo caminho original

- **Dado** o ponto único de tratamento de erro
- **Quando** ocorre um erro em um caminho que começa com `/api/` (inclusive rota inexistente, 405, 415 e falha em filtro)
- **Então** a resposta é `ProblemDetail` `application/problem+json`, qualquer que seja o `Accept`
- **E** em qualquer outro caminho (inclusive com `Accept: application/json` e falha em filtro) a resposta é a página HTML amigável

### CA-15: A ordem dos erros é fixa

- **Dado** um pedido que falha por mais de um motivo
- **Quando** o sistema o verifica
- **Então** responde pelo primeiro motivo, nesta ordem: `401`, `403` por perfil, `404`, `403` por dono, `400` e `409`

## 6. Casos de teste

Os testes de integração usam um controller de teste (só em `src/test`) em uma rota própria, `/api/testes-de-erro/**`, que lança as exceções de cada situação e não colide com nenhuma rota real. A configuração de segurança de teste (também só em `src/test`) é uma cadeia `SecurityFilterChain` de **prioridade mais alta** (`@Order(Ordered.HIGHEST_PRECEDENCE)`), restrita a `/api/testes-de-erro/**`, que **desabilita o CSRF só nessa rota**, exige autenticação e usa usuários simulados (`@WithMockUser`) para os perfis, para a T-22 não depender das cadeias da T-15. O tratamento de erros da API fica limitado a `/api/**` pelo ponto único da seção 4, e as telas têm o tratamento próprio. Os casos de violação **real** do banco rodam com Testcontainers (PostgreSQL), com uma tabela de teste criada só em `src/test`.

**Lista única de termos proibidos**, compartilhada por todos os testes (a busca ignora maiúsculas): `Exception`, `SELECT`, `INSERT`, `UPDATE`, `DELETE FROM`, `org.postgresql`, `org.springframework`, `org.hibernate`, `com.fasterxml`, `br.senac.recursos`, `java.`, `duplicate key`, `violates`, `constraint`, ` at ` seguido de nome de classe e linha (rastro de pilha), `.java` e caminhos de diretório (`/src/` e qualquer caminho que comece com `/` e tenha uma pasta de código, como `/Users/` ou `/home/`).

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Valor limite | `POST` com `capacidade = 0` | 400; `erros` com `campo = capacidade` e `motivo = "deve ser maior que zero"` | Integração |
| TC-02 | CA-01 | Valor limite | `POST` com `capacidade = 1` | Sem erro: requisição aceita (limite válido) | Integração |
| TC-03 | CA-01 | Classe inválida | `POST` com `capacidade = -1` | 400; mesmo `motivo` do TC-01 | Integração |
| TC-04 | CA-01 | Classe inválida | `POST` sem o campo `capacidade` | 400; `erros` aponta `capacidade` | Integração |
| TC-05 | CA-02 | Classe inválida | `GET` com id inexistente | 404; `detail = "Recurso não encontrado"` | Integração |
| TC-06 | CA-03 | Classe inválida | Operação que lança a exceção de conflito de teste | 409; `detail` com a mensagem específica da regra, sem termo da lista única | Integração |
| TC-07 | CA-04 | Classe inválida | Falha com `RuntimeException` cuja mensagem contém `SELECT * FROM salas` | 500; `detail` fixo; corpo sem termo da lista única | Integração |
| TC-08 | CA-04 | Classe inválida | Falha de acesso a dados que não é conflito (SQLState `22001`, valor longo demais), com SQL na mensagem | 500; `detail` fixo; corpo sem SQL nem nome de classe | Integração |
| TC-09 | CA-04 | Classe inválida | Mesma falha do TC-07, com o log capturado | O log registra o detalhe técnico; a resposta não | Integração |
| TC-10 | CA-05 | Classe inválida | Rota web de teste que falha, com `Accept: text/html` | Página HTML de erro amigável, sem termo da lista única | Integração |
| TC-11 | CA-06 | Classe válida | Um erro de cada tipo: 400, 404, 409 e 500 (teste parametrizado) | `Content-Type = application/problem+json` e campos `type`, `title`, `status`, `detail`, `instance` | Integração |
| TC-12 | CA-05 | Classe inválida | Rota web que não existe, com `Accept: text/html` | Página HTML amigável com link para a página inicial; sem termo da lista única | Integração |
| TC-13 | CA-05 | Classe inválida | Rota web de teste que lança conflito, com `Accept: text/html` | Página HTML amigável; nunca JSON | Integração |
| TC-14 | CA-05 | Classe inválida | Rota web de teste cujo modelo falha durante a renderização | Página HTML amigável; sem termo da lista única | Integração |
| TC-15 | CA-06 | Classe válida | Um erro de cada tipo (teste parametrizado) | O `title` é o do catálogo em português, nunca "Bad Request" nem "Not Found" | Integração |
| TC-16 | CA-07 | Classe inválida | `POST` com JSON malformado | 400 `ProblemDetail`; não é 500; corpo sem termo da lista única | Integração |
| TC-17 | CA-07 | Classe inválida | `GET /api/testes-de-erro/abc`, em rota que espera um número | 400 com `erros` no campo `id`; corpo sem termo da lista única | Integração |
| TC-18 | CA-07 | Classe inválida | `GET` de uma rota que não existe em `/api` | 404 `ProblemDetail`: "Recurso não encontrado"; corpo sem termo da lista única | Integração |
| TC-19 | CA-07 | Classe inválida | `DELETE` em uma rota que só aceita `GET` | 405 `ProblemDetail`; corpo sem termo da lista única | Integração |
| TC-20 | CA-07 | Classe inválida | `POST` com `Content-Type` não suportado | 415 `ProblemDetail`; corpo sem termo da lista única | Integração |
| TC-21 | CA-08 | Conflito | Violação simulada de `UNIQUE` (SQLState `23505`) | 409 com a mensagem fixa "Esta operação conflita com outro registro"; sem termo da lista única | Integração |
| TC-22 | CA-08 | Conflito | Violação simulada de `EXCLUDE` (SQLState `23P01`) | 409 com a mensagem fixa; sem termo da lista única | Integração |
| TC-23 | CA-09 | Classe inválida | Valor de enum inexistente (`"ABC"`) em um campo `estado` | 400; `erros` com `campo = estado` | Integração |
| TC-24 | CA-09 | Classe inválida | Regra que compara início e término e falha | 400; `erros` com `campo = termino` | Integração |
| TC-25 | CA-10 | Classe válida | A fábrica de erro cria o `ProblemDetail` de 401 | `status 401`, `title "Autenticação necessária"`, sem termo da lista única | Unitário |
| TC-26 | CA-10 | Classe válida | A fábrica de erro cria o `ProblemDetail` de 403 | `status 403`, `title "Acesso negado"`, sem termo da lista única | Unitário |
| TC-27 | CA-11 | Conflito | Violação simulada de chave estrangeira (SQLState `23503`) | 409 com a mensagem fixa "Este registro está em uso e não pode ser removido"; sem termo da lista única | Integração |
| TC-28 | CA-11 | Conflito | Violação **real** no banco de teste: apagar o registro pai referenciado por um filho | 409 com a mensagem fixa; o registro continua no banco | Integração |
| TC-29 | CA-08 | Conflito | Violação **real** de `UNIQUE` no banco de teste (dois cadastros com o mesmo valor, o segundo passando pelo serviço) | 409 com a mensagem fixa; só um registro gravado | Integração |
| TC-30 | CA-08 | Conflito | Violação **real** de `EXCLUDE` no banco de teste (dois períodos que se sobrepõem) | 409 com a mensagem fixa; só um registro gravado | Integração |
| TC-31 | CA-08 | Conflito | Verificação do serviço pega o repetido antes do banco | 409 com a mensagem **específica** da regra, e não a genérica | Integração |
| TC-32 | CA-03 | Classe inválida | `POST` na rota de teste de conflito, com `Accept: application/json` | 409 com `Content-Type = application/problem+json` | Integração |
| TC-33 | CA-12 | Classe inválida | `capacidade = "abc"` | 400; `erros` com `campo = capacidade` e `motivo = "valor inválido"` | Integração |
| TC-34 | CA-12 | Classe inválida | `capacidade = 30.5` | 400; `erros` com `campo = capacidade`; não vira `30` | Integração |
| TC-35 | CA-12 | Classe inválida | `capacidade = "30"` (texto) | 400; `erros` com `campo = capacidade`; não vira número | Integração |
| TC-36 | CA-12 | Valor limite | `capacidade = 2147483647` / `2147483648` | 2147483647 aceita; 2147483648 recusa (400, `campo = capacidade`) | Integração |
| TC-37 | CA-13 | Classe inválida | O serviço lança a exceção de acesso negado (não é o dono) | 403 `ProblemDetail` neutro; não é 500; corpo sem termo da lista única | Integração |
| TC-38 | CA-14 | Classe inválida | `GET` de uma rota web que não existe, com `Accept: application/json` | Página HTML amigável, e não JSON | Integração |
| TC-39 | CA-14 | Classe inválida | Falha em um filtro de teste em `/api/testes-de-erro/**` | `ProblemDetail` `application/problem+json`, com `title` do catálogo | Integração |
| TC-40 | CA-14 | Classe inválida | Falha em um filtro de teste fora de `/api` | Página HTML amigável, sem termo da lista única | Integração |
| TC-41 | CA-06 | Classe válida | Um erro de cada tipo (400, 403, 404, 405, 409, 415, 500) com `Accept: application/json` | `Content-Type = application/problem+json` em todos | Integração |
| TC-42 | CA-10 | Classe válida | A fábrica de erro cria o 401 e o 403 com um caminho `/api/x` | `ProblemDetail` com `type`, `title`, `status`, `detail` e `instance = /api/x`; sem termo da lista única | Unitário |
| TC-43 | CA-15 | Conflito | Pedido sem autenticação e com corpo inválido | 401 | Integração |
| TC-44 | CA-15 | Conflito | Perfil sem permissão e id inexistente | 403 por perfil | Integração |
| TC-45 | CA-15 | Conflito | Id inexistente e corpo inválido | 404 | Integração |
| TC-46 | CA-15 | Conflito | Quem não é o dono e corpo inválido | 403 por dono | Integração |
| TC-47 | CA-15 | Conflito | Corpo inválido e conflito de regra ao mesmo tempo | 400 | Integração |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Detalhe interno vaza porque a mensagem da exceção é copiada para o `detail` | Alto | O `detail` do 500 é texto fixo; TC-07 e TC-08 verificam os termos proibidos |
| A lista de termos proibidos fica incompleta | Médio | Lista única compartilhada pelos testes, ampliada a cada novo vazamento |
| A página de erro padrão do Spring (Whitelabel) aparece nas telas | Médio | TC-10 e TC-12; configurar o Spring para nunca incluir stack trace nem mensagem de exceção nas respostas |
| A T-23 (regra de sobreposição) ainda não existe | Médio | TC-06 usa exceção de conflito de teste; a regra real é testada na T-20 |
| Mensagens de validação inconsistentes entre endpoints | Médio | Esta task entrega o mecanismo; cada task de funcionalidade define as suas mensagens |
| Respostas 401 e 403 fora do formato, porque saem dos filtros de segurança e não passam pelo tratamento da API | Médio | A fábrica é desta task (CA-10, TC-25, TC-26 e TC-42); a resposta real do filtro é provada na T-15, que depende da T-22 |
| O tratamento genérico de `Exception` transforma erros do Spring em 500 | Alto | CA-07 e TC-16 a TC-20 |
| A rota de teste colide com uma rota real | Médio | Rota própria `/api/testes-de-erro/**`, só em `src/test` |
| O tratamento em JSON responde também nas telas, ou o HTML responde na API | Médio | Ponto único pelo caminho original (CA-14); TC-13, TC-38, TC-39 e TC-40 |
| Quem perde a corrida entre pedidos recebe 500 | Alto | CA-08 e TC-21, TC-22, TC-29, TC-30: `UNIQUE` e `EXCLUDE` viram 409 |
| Excluir um registro em uso devolve 500 | Alto | CA-11 e TC-27, TC-28: a chave estrangeira (SQLState `23503`) vira 409 |
| A mensagem genérica do 409 do banco esconde qual campo repetiu | Baixo | Aceito: a mensagem específica vem da verificação do serviço (TC-31); o banco só pega a corrida |
| O Jackson converte `30.5` em `30` ou `"30"` em número | Médio | CA-12 e TC-33 a TC-36 exigem a leitura sem coerção |
| A exceção de acesso negado do serviço cai no tratamento genérico como 500 | Alto | CA-13 e TC-37 |
| A ordem dos erros muda sem querer (400 antes de 404) | Médio | CA-15 e TC-43 a TC-47 |
| A configuração de segurança de teste interfere nas cadeias da T-15 | Médio | Cadeia de teste de prioridade mais alta, restrita a `/api/testes-de-erro/**`, só em `src/test` |

## 8. Mini-plano de implementação (ciclos TDD)

Nos ciclos 1 e 2, TC-02 e TC-03 já passam depois do primeiro verde e protegem o limite. Quando a coluna "Teste (red)" não cita um caso, ele já passa e só protege.

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: `capacidade = 0` devolve 400 com campo e motivo | `@RestControllerAdvice` com o tratamento da validação, devolvendo `ProblemDetail` | Extrair a criação do `ProblemDetail` para um método único |
| 2 | TC-04: campo ausente devolve 400 | Ajustar a validação do DTO de teste (`@NotNull`) | Nenhuma (TC-02 e TC-03 já passam e protegem o limite) |
| 3 | TC-05: id inexistente devolve 404 | Exceção de recurso não encontrado e seu tratamento | Mover os textos fixos para constantes |
| 4 | TC-06: conflito devolve 409 | Exceção de conflito de regra e seu tratamento | Unificar os tratamentos de exceção de negócio |
| 5 | TC-07, TC-08 e TC-09: erro inesperado devolve 500 neutro e vai para o log | Tratamento genérico de `Exception` com texto fixo e log do detalhe | Garantir que nenhum tratamento use a mensagem da exceção no corpo |
| 6 | TC-11, TC-15 e TC-41: contrato dos erros, títulos em português e `Accept: application/json` | Ajustes que faltarem no `type`, no `instance`, no `title` e no `Content-Type` | Consolidar o teste parametrizado |
| 7 | TC-16 a TC-20: erros que o Spring já trata | Tratar 400, 404, 405 e 415 antes do tratamento genérico | Agrupar os tratamentos por tipo |
| 8 | TC-21, TC-22, TC-29, TC-30, TC-31 e TC-32: `UNIQUE`, `EXCLUDE` e precedência da mensagem | Traduzir `UNIQUE` e `EXCLUDE` para 409 genérico, depois da verificação do serviço | Isolar a leitura do SQLState |
| 9 | TC-27 e TC-28: chave estrangeira | Traduzir o SQLState `23503` para 409 com a mensagem "em uso" | Reaproveitar a leitura do SQLState do ciclo 8 |
| 10 | TC-23 e TC-24: campo do erro | Exceção de regra com o campo indicado | Reaproveitar na T-20 e na T-21 |
| 11 | TC-34 e TC-35: leitura do JSON sem coerção (TC-33 e TC-36 já passam pelo ciclo 7) | Configurar o Jackson sem coerção e traduzir a falha de leitura para 400 no campo | Mover a configuração para um único lugar |
| 12 | TC-37: acesso negado do serviço devolve 403 | Exceção de acesso negado e seu tratamento pela fábrica | Reaproveitar a fábrica do ciclo 15 |
| 13 | TC-10, TC-12, TC-13 e TC-14: telas | Página de erro para falha, 404, 409 e falha de renderização | Reutilizar os textos fixos |
| 14 | TC-38, TC-39 e TC-40: API e telas separadas pelo caminho | Ponto único (`ErrorController` + consulta ao caminho original) | Remover decisões por `Accept` espalhadas |
| 15 | TC-25, TC-26 e TC-42: 401 e 403 | Fábrica de `ProblemDetail` para a T-15 | Nenhuma |
| 16 | TC-43 a TC-47: ordem dos erros | Verificar perfil, existência, dono, entrada e regra, nessa ordem | Documentar a ordem em um só lugar |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [x] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [x] Os casos de teste incluem valores limite e entradas inválidas.
- [x] O contrato da API cobre os erros (400, 401, 403, 409).
- [x] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

Reli a versão atual e os pontos das revisões anteriores foram tratados: a chave estrangeira vira 409, a leitura do JSON aponta o campo, o 403 do serviço tem tratamento, a separação entre API e telas é feita pelo caminho original, a lista de termos foi ampliada, e há testes reais no banco. A prova da resposta real de 401 e 403 ficou com a T-15, que agora verifica `Content-Type`, `title` e `instance`. A spec está pronta para seguir.

A aprovação depende do merge do PR #34 antes deste PR, porque a dependência da T-15 em relação à T-22 está nele.

Pontos para acompanhar na implementação, sem bloquear a aprovação:

- A ordem `404` e `403` por dono antes de `400` precisa de um mecanismo, porque a leitura do JSON e o `@Valid` normalmente rodam antes do serviço. Também falta dizer se JSON malformado fica fora dessa ordem. T-16, T-18, T-20 e T-21 dependem dessa decisão.
- O ciclo 12 reaproveita a fábrica de 403, mas ela só é criada no ciclo 15. A fábrica precisa vir antes.
- TC-33 e o caso `2147483648` do TC-36 não passam só com o ciclo 7, porque `erros` com o campo só aparece no ciclo 11.
- Falta definir o formato de `campo` em lista ou objeto aninhado, como `recursos[0].id`, com um caso de teste.
- A busca da lista de termos pode dar falso positivo em HTML com `<select>` ou `form-select`. Vale buscar no texto visível ou usar padrões como `select .* from`.
- As telas prometem os mesmos erros da API, mas não há caso para 400 e 405. Vale cobrir esses códigos ou limitar a frase.
- O log técnico não deve registrar senha, token nem o corpo da requisição.

**Parecer:** Aprovada · @Samwel-Merces · 2026-11-07

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

Não se aplica: a T-22 não é regra crítica.
