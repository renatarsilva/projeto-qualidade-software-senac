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
- `400` para entrada inválida, com o campo e o motivo (RNF-07).
- `404` para recurso não encontrado e `409` para conflito de regra, inclusive a violação de restrição do banco (`UNIQUE` e `EXCLUDE`), que vira `409` e não `500`.
- Os erros que o Spring já trata (JSON malformado, id não numérico, rota inexistente, método não permitido e formato não suportado) mantêm o código 4xx certo e não viram `500`.
- O campo do erro de uma regra de negócio ou de um valor inválido de enum (por exemplo `estado`, `termino`).
- O formato de `401` e `403`, com a fábrica de `ProblemDetail` que os filtros de segurança da T-15 usam.
- Erro inesperado vira `500` neutro, sem stack trace, SQL, nome de classe nem caminho interno (RNF-08). O detalhe técnico fica só no log.
- Página de erro amigável nas telas, para falhas, `404` e `409`, inclusive falha durante a renderização.

**Fora (e por quê):**
- A implementação dos filtros que devolvem `401` e `403` (autenticação e autorização): é da T-15, que usa o formato definido aqui.
- As regras de negócio que geram o `409`, como a sobreposição de horários: são da T-23 e da T-24. Esta task só define o **formato** da resposta.
- As validações de cada campo de cada endpoint: cada task de funcionalidade define as suas. Esta task entrega o mecanismo.
- O conteúdo e o visual das telas: são da T-44 e da T-45.

## 3. Regras de negócio

Esta task não cria regra de negócio: ela aplica requisitos do PRD.

- **RF-13:** interface responsiva e **mensagens de erro compreensíveis**.
- **RNF-07 (validação de entrada):** toda entrada inválida catalogada é recusada, e nenhuma gravação inválida acontece. A exigência é oficial; a medida (100 % do catálogo) é candidata, com validação pendente.
- **RNF-08 (tratamento seguro de erros):** nenhuma resposta de erro contém stack trace, SQL, nome de classe nem caminho interno.

## 4. Contrato da API

Esta task não cria um endpoint: ela define **como todos os endpoints respondem a erro**. Toda resposta de erro tem o tipo `application/problem+json` (RFC 9457), com mensagens em português, e vale para os três perfis. O tratamento em JSON vale só para a API (`/api/**`): as telas Thymeleaf recebem página HTML.

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
  "instance": "/api/salas",
  "erros": [
    { "campo": "capacidade", "motivo": "deve ser maior que zero" }
  ]
}
```

**Respostas:**

| Situação | Status HTTP | Corpo |
|---|---|---|
| Entrada inválida | 400 | `ProblemDetail` com a lista `erros` (campo e motivo) |
| Valor de enum inexistente ou regra entre campos | 400 | `ProblemDetail` com `erros` apontando o campo certo (por exemplo `estado`, `termino`) |
| JSON malformado, ou id que não é número | 400 | `ProblemDetail`: "Corpo da requisição inválido" ou `erros` com o campo `id` |
| Recurso ou rota não encontrados | 404 | `ProblemDetail`: "Recurso não encontrado" |
| Método não permitido | 405 | `ProblemDetail`: "Método não permitido" |
| Formato não suportado | 415 | `ProblemDetail`: "Formato não suportado" |
| Conflito de regra, ou violação de `UNIQUE` ou `EXCLUDE` no banco | 409 | `ProblemDetail` com o motivo do conflito, em linguagem de negócio. A violação do banco traz a mensagem fixa "Esta operação conflita com outro registro" |
| Erro inesperado, inclusive falha de acesso a dados que não é conflito | 500 | `ProblemDetail` neutro: "Ocorreu um erro inesperado. Tente novamente mais tarde." Sem stack trace, SQL, nome de classe nem caminho |
| Sem autenticação e perfil sem permissão | 401 e 403 | `ProblemDetail` neutro, mesmo formato. Esta task define o formato e a fábrica; os filtros de segurança (T-15) a usam |

Nas telas, os mesmos erros aparecem como uma página de erro amigável, em português, sem detalhe técnico e com um link de volta para a página inicial.

## 5. Critérios de aceite

### CA-01: Entrada inválida (400)

- **Dado** que existe o endpoint de cadastro de sala (`POST /api/salas`)
- **Quando** o cliente envia uma requisição informando uma capacidade menor ou igual a 0 (ex.: 0 ou -1)
- **Então** o sistema retorna o status HTTP 400 Bad Request
- **E** o corpo traz, na lista `erros`, o campo inválido e a mensagem de negócio correspondente (ex.: "deve ser maior que zero")

### CA-02: Recurso inexistente (404)

- **Dado** que o endpoint de consulta de salas por ID está disponível
- **Quando** o cliente executa `GET /api/salas/{id}` com um identificador inexistente
- **Então** o sistema retorna o status HTTP 404 Not Found
- **E** o corpo traz a mensagem padrão em português: "Recurso não encontrado"
- **E** uma rota que não existe devolve o mesmo 404

### CA-03: Conflito de regra de negócio (409)

- **Dado** que existe o mecanismo de tratamento de conflitos de negócio (como uma reserva com horário sobreposto ou uma exceção de conflito de teste)
- **Quando** o cliente executa a operação que gera o conflito de domínio
- **Então** o sistema retorna o status HTTP 409 Conflict
- **E** o corpo traz a explicação em linguagem de negócio, sem expor rastros técnicos do banco de dados
- **E** uma violação de restrição `UNIQUE` ou `EXCLUDE` do banco (inclusive a de quem perde uma corrida entre pedidos) também devolve 409, e não 500

### CA-04: Erro inesperado (500 neutro)

- **Dado** que ocorre uma falha ou exceção não tratada no servidor durante o processamento da requisição
- **Quando** o sistema intercepta o erro inesperado
- **Então** o sistema retorna o status HTTP 500 Internal Server Error
- **E** o corpo traz obrigatoriamente o detalhe fixo: "Ocorreu um erro inesperado. Tente novamente mais tarde."
- **E** o corpo não contém nenhum dos termos proibidos da lista única (seção 6): nomes de exceção, SQL, nomes de pacote e caminhos de diretório, que vão exclusivamente para o log do servidor
- **E** esse tratamento genérico não transforma em 500 os erros que o Spring já trata (CA-07)

### CA-05: Tela de erro amigável (Thymeleaf)

- **Dado** que a pessoa usuária acessa uma rota da interface web renderizada pelo servidor
- **Quando** ocorre uma falha ou erro não tratado durante a requisição ou a renderização da página
- **Então** o servidor responde com uma página HTML de erro amigável, em português, com uma frase que diz o que houve e um link para a página inicial
- **E** isso vale para falha, `404` e `409`, inclusive quando a falha acontece durante a renderização da página
- **E** a página não exibe nenhum termo proibido da lista única
- **E** a resposta das telas nunca é JSON

### CA-06: Formato de erro padronizado (RFC 9457, Problem Details)

- **Dado** que qualquer resposta de erro da API (400, 404, 409 ou 500) é disparada
- **Quando** o cliente recebe o corpo de erro do servidor
- **Então** o cabeçalho `Content-Type` é `application/problem+json`
- **E** o corpo JSON contém os campos do contrato: `type`, `title`, `status`, `detail` e `instance`
- **E** o `title` é o do catálogo em português da seção 4, nunca o texto padrão em inglês (como "Bad Request")

### CA-07: Erros que o Spring já trata mantêm o código certo

- **Dado** o tratamento genérico de erros
- **Quando** o cliente envia um JSON malformado, um id que não é número, uma rota que não existe, um método não permitido ou um formato não suportado
- **Então** o sistema devolve 400, 400, 404, 405 e 415, respectivamente, e não 500
- **E** cada resposta segue o formato do CA-06 e não contém termo proibido

### CA-08: Violação de restrição do banco vira conflito

- **Dado** uma operação que viola uma restrição `UNIQUE` ou `EXCLUDE` do banco
- **Quando** o banco recusa a gravação
- **Então** o sistema devolve 409 com a mensagem fixa "Esta operação conflita com outro registro"
- **E** qualquer outra falha de acesso a dados continua 500 neutro

### CA-09: O campo do erro vem da regra

- **Dado** um valor de enum inexistente, ou uma regra que compara dois campos
- **Quando** o cliente envia esse pedido
- **Então** o sistema devolve 400 com `erros` apontando o campo certo (por exemplo `estado` para o enum e `termino` para a regra de início e término)

### CA-10: 401 e 403 seguem o mesmo formato

- **Dado** o formato de erro desta task
- **Quando** um filtro de segurança (T-15) recusa uma chamada sem autenticação ou sem permissão
- **Então** a resposta é `application/problem+json` com status 401 ou 403 e os `title` "Autenticação necessária" ou "Acesso negado"
- **E** o corpo não contém termo proibido

## 6. Casos de teste

Os testes de integração usam um controller de teste (só em `src/test`) em uma rota própria, `/api/testes-de-erro/**`, que lança as exceções de cada situação e não colide com a `/api/salas` da T-16. A configuração de segurança de teste (também só em `src/test`) libera essa rota, para a T-22 não depender da T-15. O tratamento de erros da API fica limitado a `/api/**`, e as telas têm o tratamento próprio.

**Lista única de termos proibidos**, compartilhada por todos os testes: `Exception`, `SELECT`, `INSERT`, `UPDATE`, `DELETE FROM`, `org.postgresql`, `org.springframework`, `br.senac.recursos`, ` at ` seguido de nome de classe e linha (rastro de pilha), `.java` e `/src/`.

| Caso | Critério | Técnica | Entrada | Resultado esperado | Nível |
|---|---|---|---|---|---|
| TC-01 | CA-01 | Valor limite | `POST` com `capacidade = 0` | 400; `erros` com `campo = capacidade` e `motivo = "deve ser maior que zero"` | Integração |
| TC-02 | CA-01 | Valor limite | `POST` com `capacidade = 1` | Sem erro: requisição aceita (limite válido) | Integração |
| TC-03 | CA-01 | Classe inválida | `POST` com `capacidade = -1` | 400; mesmo `motivo` do TC-01 | Integração |
| TC-04 | CA-01 | Classe inválida | `POST` sem o campo `capacidade` | 400; `erros` aponta `capacidade` | Integração |
| TC-05 | CA-02 | Classe inválida | `GET` com id inexistente | 404; `detail = "Recurso não encontrado"` | Integração |
| TC-06 | CA-03 | Classe inválida | Operação que lança a exceção de conflito de teste | 409; `detail` em linguagem de negócio, sem termo proibido | Integração |
| TC-07 | CA-04 | Classe inválida | Falha com `RuntimeException` cuja mensagem contém `SELECT * FROM salas` | 500; `detail` fixo; corpo sem termo proibido | Integração |
| TC-08 | CA-04 | Classe inválida | Falha de acesso a dados que não é conflito, com SQL na mensagem | 500; `detail` fixo; corpo sem SQL nem nome de classe | Integração |
| TC-09 | CA-04 | Classe inválida | Mesma falha do TC-07, com o log capturado | O log registra o detalhe técnico; a resposta não | Integração |
| TC-10 | CA-05 | Classe inválida | Rota web de teste que falha, com `Accept: text/html` | Página HTML de erro amigável, sem termo proibido | Integração |
| TC-11 | CA-06 | Classe válida | Um erro de cada tipo: 400, 404, 409 e 500 (teste parametrizado) | `Content-Type = application/problem+json` e campos `type`, `title`, `status`, `detail`, `instance` | Integração |
| TC-12 | CA-05 | Classe inválida | Rota web que não existe, com `Accept: text/html` | Página HTML amigável com link para a página inicial; sem termo proibido | Integração |
| TC-13 | CA-05 | Classe inválida | Rota web de teste que lança conflito, com `Accept: text/html` | Página HTML amigável; nunca JSON | Integração |
| TC-14 | CA-05 | Classe inválida | Rota web de teste cujo modelo falha durante a renderização | Página HTML amigável; sem termo proibido | Integração |
| TC-15 | CA-06 | Classe válida | Um erro de cada tipo (teste parametrizado) | O `title` é o do catálogo em português, nunca "Bad Request" nem "Not Found" | Integração |
| TC-16 | CA-07 | Classe inválida | `POST` com JSON malformado | 400 `ProblemDetail`; não é 500 | Integração |
| TC-17 | CA-07 | Classe inválida | `GET /api/testes-de-erro/abc`, em rota que espera um número | 400 com `erros` no campo `id` | Integração |
| TC-18 | CA-07 | Classe inválida | `GET` de uma rota que não existe em `/api` | 404 `ProblemDetail`: "Recurso não encontrado" | Integração |
| TC-19 | CA-07 | Classe inválida | `DELETE` em uma rota que só aceita `GET` | 405 `ProblemDetail` | Integração |
| TC-20 | CA-07 | Classe inválida | `POST` com `Content-Type` não suportado | 415 `ProblemDetail` | Integração |
| TC-21 | CA-08 | Conflito | Violação simulada de `UNIQUE` (SQLState 23505) | 409 com a mensagem fixa; sem termo proibido | Integração |
| TC-22 | CA-08 | Conflito | Violação simulada de `EXCLUDE` (SQLState 23P01) | 409 com a mensagem fixa; sem termo proibido | Integração |
| TC-23 | CA-09 | Classe inválida | Valor de enum inexistente (`"ABC"`) em um campo `estado` | 400; `erros` com `campo = estado` | Integração |
| TC-24 | CA-09 | Classe inválida | Regra que compara início e término e falha | 400; `erros` com `campo = termino` | Integração |
| TC-25 | CA-10 | Classe válida | A fábrica de erro cria o `ProblemDetail` de 401 | `status 401`, `title "Autenticação necessária"`, sem termo proibido | Unitário |
| TC-26 | CA-10 | Classe válida | A fábrica de erro cria o `ProblemDetail` de 403 | `status 403`, `title "Acesso negado"`, sem termo proibido | Unitário |

## 7. Riscos

| Risco | Impacto | Como a spec ou os testes tratam |
|---|---|---|
| Detalhe interno vaza porque a mensagem da exceção é copiada para o `detail` | Alto | O `detail` do 500 é texto fixo; TC-07 e TC-08 verificam os termos proibidos |
| A lista de termos proibidos fica incompleta | Médio | Lista única compartilhada pelos testes, ampliada a cada novo vazamento; a medida do RNF-07 segue candidata |
| A página de erro padrão do Spring (Whitelabel) aparece nas telas | Médio | TC-10; configurar o Spring para nunca incluir stack trace nem mensagem de exceção nas respostas |
| A T-23 (regra de sobreposição) ainda não existe | Médio | TC-06 usa exceção de conflito de teste; a regra real é testada na T-20 |
| Mensagens de validação inconsistentes entre endpoints | Médio | Esta task entrega o mecanismo; cada task de funcionalidade define as suas mensagens |
| Respostas 401 e 403 fora do formato, porque saem dos filtros de segurança e não passam pelo tratamento da API | Médio | O formato e a fábrica são desta task (CA-10, TC-25 e TC-26); a T-15 liga os filtros a eles |
| O tratamento genérico de `Exception` transforma erros do Spring em 500 | Alto | CA-07 e TC-16 a TC-20 |
| A rota de teste colide com uma rota real | Médio | Rota própria `/api/testes-de-erro/**`, só em `src/test` |
| O tratamento em JSON responde também nas telas | Médio | Limitado a `/api/**`; TC-13 verifica que a tela nunca recebe JSON |
| Quem perde a corrida entre pedidos recebe 500 | Alto | CA-08 e TC-21, TC-22: `UNIQUE` e `EXCLUDE` viram 409 |

## 8. Mini-plano de implementação (ciclos TDD)

| Ciclo | Teste (red) | Implementação (green) | Refatoração prevista |
|---|---|---|---|
| 1 | TC-01: `capacidade = 0` devolve 400 com campo e motivo | `@RestControllerAdvice` com o tratamento da validação, devolvendo `ProblemDetail` | Extrair a criação do `ProblemDetail` para um método único |
| 2 | TC-04: campo ausente devolve 400 | Ajustar a validação do DTO de teste (`@NotNull`) | Nenhuma (TC-02 e TC-03 já passam e protegem o limite) |
| 3 | TC-05: id inexistente devolve 404 | Exceção de recurso não encontrado e seu tratamento | Mover os textos fixos para constantes |
| 4 | TC-06: conflito devolve 409 | Exceção de conflito de regra e seu tratamento | Unificar os tratamentos de exceção de negócio |
| 5 | TC-07, TC-08 e TC-09: erro inesperado devolve 500 neutro e vai para o log | Tratamento genérico de `Exception` com texto fixo e log do detalhe | Garantir que nenhum tratamento use a mensagem da exceção no corpo |
| 6 | TC-10: rota web que falha devolve página amigável | Página de erro Thymeleaf e configuração do Spring contra vazamento | Reutilizar os textos fixos da API |
| 7 | TC-11 e TC-15: contrato dos erros e títulos em português | Ajustes que faltarem no `type`, no `instance` e no `title` | Consolidar o teste parametrizado |
| 8 | TC-16 a TC-20: erros que o Spring já trata | Tratar 400, 404, 405 e 415 antes do tratamento genérico | Agrupar os tratamentos por tipo |
| 9 | TC-21 e TC-22: restrições do banco | Traduzir `UNIQUE` e `EXCLUDE` para 409 | Isolar a leitura do SQLState |
| 10 | TC-23 e TC-24: campo do erro | Exceção de regra com o campo indicado | Reaproveitar na T-20 e na T-21 |
| 11 | TC-12 a TC-14: telas | Página de erro para 404, 409 e falha de renderização | Reutilizar os textos fixos |
| 12 | TC-25 e TC-26: 401 e 403 | Fábrica de `ProblemDetail` para a T-15 | Nenhuma |

## 9. Revisão por pares

> Preenchida pelo revisor, com commit próprio. Nunca pelo autor.

- [x] O objetivo e o escopo estão claros, e o "fora do escopo" está justificado.
- [x] As regras citam RNs que existem no PRD.
- [ ] Todo critério de aceite é verificável (dá para dizer "passou" ou "falhou").
- [ ] Os casos de teste incluem valores limite e entradas inválidas.
- [ ] O contrato da API cobre os erros (400, 401, 403, 409).
- [ ] Não há conflito com outras specs nem com os ADRs.

**Comentários:**

O objetivo e o escopo estão claros, e os requisitos citados existem no PRD. O formato do `ProblemDetail` está bem definido, e o `detail` fixo no erro 500 protege contra vazamento de detalhes internos. Os pontos abaixo precisam de ajuste porque as outras specs dependem desta.

Critérios: "orientações gerais" no CA-05 não dá um resultado objetivo. A linha sobre as telas promete os mesmos erros da API, mas CA-05 e TC-10 cobrem só a falha não tratada; faltam 404 e 409 nas telas e a falha durante a renderização. O CA-06 também não exige `title` em português, então "Bad Request" passaria.

Testes: o tratamento genérico de `Exception` pode transformar em 500 erros que o Spring já trata, como JSON malformado, id não numérico, rota inexistente, 405 e 415. Faltam casos para eles. A lista de termos proibidos usa `at com.`, mas o pacote do projeto é `br.senac.recursos`, e ela também não cobre `INSERT`, `org.postgresql` e caminhos fora de `/src/`. No mini-plano, TC-03 não tem red real no ciclo 2, porque a regra de mínimo já precisa existir para TC-01 passar no ciclo 1.

Contrato: 401 e 403 ficam sem dono, porque a T-22 os deixa para a T-15 e a T-15 deixa o formato para a T-22. Esses erros acontecem nos filtros de segurança e não passam pelo advice, e o backlog pede formato único para todos os endpoints. Também é preciso definir que violação de restrição do banco (`UNIQUE` e `EXCLUDE`) vira 409: hoje o TC-08 leva falha de acesso a dados para 500, e quem perde a corrida entre pedidos simultâneos receberia 500.

Conflitos: o controller de teste usa `/api/salas`, a mesma rota que a T-16 vai criar, o que pode causar "Ambiguous mapping". Falta dizer como esses testes passam pela segurança. O `@RestControllerAdvice` precisa ficar limitado à API, senão também responde JSON nas telas do Thymeleaf. A lista `erros` não atende a T-21, que espera `campo = estado` para um enum inválido, nem a T-20, que espera `campo = termino` em uma regra que compara dois campos.

Ajustes de texto: o status ainda está `Rascunho`, e a seção 11 deveria dizer "Não se aplica", porque a task não é crítica. As instruções do template continuam nas seções 3 e 6. Nos riscos, a medida candidata é a do RNF-07, não a do RNF-08.

**Parecer:** Ajustes necessários · @Samwel-Merces · 2026-11-05

## 10. Spec validation

Resultado de `./scripts/validar-spec.sh` (preenchido pelo autor após o script passar):

- **Data:** _preencher_
- **Resultado:** _preencher_

## 11. Grill-with-docs

Não se aplica: a T-22 não é regra crítica.
